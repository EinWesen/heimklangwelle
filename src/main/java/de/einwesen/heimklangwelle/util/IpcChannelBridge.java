package de.einwesen.heimklangwelle.util;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.channels.CompletionHandler;
import java.nio.charset.StandardCharsets;
import java.nio.file.NoSuchFileException;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class IpcChannelBridge implements AutoCloseable {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(IpcChannelBridge.class);	
	
	private final ExecutorService ioExecutor = Executors.newFixedThreadPool(3);
	private final ExecutorService consumerExecutor = Executors.newSingleThreadExecutor();
	private final BlockingQueue<String> sinkQueue = new LinkedBlockingQueue<>();

	private final Consumer<String> consumer;
	private final Consumer<ExecutionException> errorConsumer;
	private final AsynchronousFileChannel namedPipeChannel;
	
	private volatile boolean shouldBeRunning = true;
	private volatile boolean readerRunning = true;
	private volatile boolean writerRunning = true;
	
	public IpcChannelBridge(String pipeName, Consumer<String> consumer) throws FileNotFoundException, IOException {
		this(pipeName, consumer, null);
	}
	public IpcChannelBridge(String pipeName, Consumer<String> consumer, Consumer<ExecutionException> errorConsumer) throws FileNotFoundException, IOException {
		this.consumer = consumer;
		this.errorConsumer = errorConsumer;
		this.namedPipeChannel = openNamedPipeChannel(pipeName, this.ioExecutor);
		this.ioExecutor.submit(this::namedPipeReader);
		this.ioExecutor.submit(this::namedPipeWriter);	
	}		
	
	private void namedPipeReader() {

		final AtomicInteger errorCounter = new AtomicInteger(0);
		final ByteBuffer readingBuffer = ByteBuffer.allocate(128);
		final AsynchronousFileChannel self = this.namedPipeChannel;
		final ByteArrayOutputStream lineBuffer = new ByteArrayOutputStream();

		self.read(readingBuffer, 0, null, new CompletionHandler<Integer, Void>() {

			@Override
			public void completed(Integer bytesRead, Void attachment) {
				if (bytesRead > -1) {
					
					readingBuffer.flip();
					while (readingBuffer.hasRemaining()) {
						byte b = readingBuffer.get();						
						if (b == '\n') {
							final String data = lineBuffer.toString(StandardCharsets.UTF_8); // Important because of lambda
							consumerExecutor.submit(() -> consumer.accept(data));
							lineBuffer.reset();
						} else if (b != '\r') {
							lineBuffer.write(b);
						}
					}
					readingBuffer.clear();

					// Schedule next read
					self.read(readingBuffer, 0, null, this);
				} else {
					this.failed(new IOException("channel closed?"), attachment);
				}
				
			}

			@Override
			public void failed(Throwable exc, Void attachment) {
				if (shouldBeRunning) { // means, it's not duie to closing
					LOGGER.warn("read failed", exc);
					final int tex = errorCounter.incrementAndGet();
					if (errorConsumer != null) {
						final String msg = "R:" + String.valueOf(tex);
						consumerExecutor.submit(() -> errorConsumer.accept(new ExecutionException(msg, exc)));
					}

					if (tex < 10) {
						try {
							Thread.sleep(TimeUnit.SECONDS.toMillis(1));
						} catch (InterruptedException e1) {
						}
						self.read(readingBuffer, 0, null, this);
					} else {
						readerRunning = false;
						LOGGER.error("Reader finished due to errors");
					}
				}
			}
		});

	}
	
    private void namedPipeWriter() {

    	int errorCounter = 0;
    	
        while (shouldBeRunning && writerRunning) {
        	try {
				try {
					String msg = this.sinkQueue.take();
					
					writeToChannel(msg, this.namedPipeChannel);
				} catch (InterruptedException e) {
					LOGGER.warn("waiting on queue interrupted ", e);
					throw e;
				} catch (ExecutionException e) {
					LOGGER.warn("writing failed ", e);
					throw e;
				}
			} catch (Throwable e) {
        		if (shouldBeRunning) { // means, it's not duie to closing
        			errorCounter += 1;
        			if (errorConsumer != null) {
        				final String msg = "W:"+String.valueOf(errorCounter);
        				consumerExecutor.submit(() -> errorConsumer.accept(new ExecutionException(msg, e)));
        			}
        			if (errorCounter < 10) {
        				try { Thread.sleep(TimeUnit.SECONDS.toMillis(1)); } catch (InterruptedException e1) {}        				
        			}else {
        				writerRunning = false;
        				LOGGER.error("Writer finished due to errors");        	
        			}        			
        		}				
			}        	
        }
            	    
    }
	
 
    private static boolean writeToChannel(String msg, AsynchronousFileChannel channel) throws ExecutionException {
    	
    	final CompletableFuture<Boolean> future = new CompletableFuture<>();
        
    	ByteBuffer buffer = ByteBuffer.wrap((msg + "\n").getBytes(StandardCharsets.UTF_8));

    	channel.write(buffer, 0, null, new CompletionHandler<Integer, Void>() {
            
        	@Override
            public void completed(Integer bytesWritten, Void attachment) {
                if (buffer.hasRemaining()) {
                    // continue writing remaining bytes
                	channel.write(buffer, 0, null, this);
                } else {
                    future.complete(Boolean.TRUE); // write fully done
                }
            }

            @Override
            public void failed(Throwable exc, Void attachment) {
                future.completeExceptionally(exc); // propagate failure
            }
        });

		try {
			return future.get().booleanValue();
		} catch (InterruptedException e) {
			throw new ExecutionException(e);
		}
        
    }
    
    public boolean write(String data) throws IOException {
    	if (data == null) throw new IllegalArgumentException("data may not be null");
    	if (!this.shouldBeRunning) throw new IOException("Already closed");
    	if (!this.writerRunning) throw new IOException("Sink has broken down");
    	return this.sinkQueue.offer(data);    	
    }
    
    @Override
    public void close() throws Exception {
    	this.shouldBeRunning = false;
    	
    	this.ioExecutor.shutdown();
    	try {
			if (!this.ioExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
				LOGGER.trace("IOExecutor did not terminate in time");
			}
		} catch (InterruptedException e) {
			LOGGER.trace("IOExecutor interrupted during termination");
		}
    	
    	try {
			this.consumerExecutor.shutdown();
			if (!this.consumerExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
				LOGGER.trace("ConsumerExecutor did not terminate in time");
			}
		} catch (InterruptedException e) {
			LOGGER.trace("ConsumerExecutor interrupted during termination");
		}
    	
    	try {
			this.namedPipeChannel.close();
		} catch (IOException e) {
			// ignore
		}
    	
    }
    
    private static AsynchronousFileChannel openNamedPipeChannel(String pipname, ExecutorService usingExecutor) throws IOException {
        if (pipname == null) {
        	throw new IllegalArgumentException("null");
        }
    	
    	int errorCounter = 0;
    	while (true) {
            try {
                return AsynchronousFileChannel.open(
                        Paths.get(pipname),
                        Set.of(StandardOpenOption.READ,StandardOpenOption.WRITE),
                        usingExecutor
                );
            } catch (NoSuchFileException e) {
                if ((++errorCounter)<40) {
                	LOGGER.trace("Try " + String.valueOf(errorCounter) + ": pipe not found");
                	try {
                		Thread.sleep(50); // Wait a little and retry
                	} catch (InterruptedException ignore) {}                	
                } else {
                	throw e;
                }
            }
        }
    }    
    
    public boolean isClosed() {
    	return !this.shouldBeRunning;
    }
    
    public boolean isReadingAndWriting() {
    	return this.readerRunning && this.writerRunning;
    }
	public boolean isReading() {
		return this.readerRunning;
	}
	public boolean isWriting() {
		return this.writerRunning;
	}

}

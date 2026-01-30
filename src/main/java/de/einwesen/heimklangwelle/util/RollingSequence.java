package de.einwesen.heimklangwelle.util;

import java.util.Iterator;

public class RollingSequence implements Iterator<Integer> {
	
	private int start = Integer.MIN_VALUE;
	private int end = Integer.MAX_VALUE;
	private int current = Integer.MIN_VALUE;

	
	public RollingSequence(int end) {
		this.end = end;
	}

	public RollingSequence(int start, int end, int current) {
		if (!(start<=current && end>=current)) {
			throw new IllegalArgumentException("assertation failed: start <= current <= end");
		}
		
		this.start = start;
		this.end = end;
		this.current = current;

	}

	public int nextInt() {
		if (this.current == this.end) {
			this.current = this.start;
		} else {
			this.current += 1;
		}
		return this.current;		
	}
	
	@Override
	public Integer next() {
		return Integer.valueOf(nextInt()); 
	}

	@Override
	public boolean hasNext() {
		return true;
	}
	
}

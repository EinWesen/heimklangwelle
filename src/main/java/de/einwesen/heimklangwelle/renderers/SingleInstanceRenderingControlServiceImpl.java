package de.einwesen.heimklangwelle.renderers;

import org.jupnp.model.types.UnsignedIntegerFourBytes;
import org.jupnp.model.types.UnsignedIntegerTwoBytes;
import org.jupnp.support.model.Channel;
import org.jupnp.support.renderingcontrol.AbstractAudioRenderingControl;
import org.jupnp.support.renderingcontrol.RenderingControlErrorCode;
import org.jupnp.support.renderingcontrol.RenderingControlException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.einwesen.heimklangwelle.HeimklangServiceRegistry;
import de.einwesen.heimklangwelle.upnpsupport.RendererChangeEventListener;

//UPNP annotations are inherited from parent
public class SingleInstanceRenderingControlServiceImpl extends AbstractAudioRenderingControl implements RendererChangeEventListener {
	
	private final Logger LOGGER = LoggerFactory.getLogger(SingleInstanceRenderingControlServiceImpl.class);
	
	private final AbstractRendererWrapper rendererInstance;
	private final UnsignedIntegerFourBytes[] instanceIds;
	
	public SingleInstanceRenderingControlServiceImpl() {
		super();
		this.rendererInstance = HeimklangServiceRegistry.getCurrentRendererInstance(this);
		this.instanceIds = new UnsignedIntegerFourBytes[] {this.rendererInstance.getInstanceId()};
	}
	
	private Channel validateInstanceChannel(UnsignedIntegerFourBytes instanceId, String channelName) throws RenderingControlException {
		if (instanceId.equals(instanceIds[0])) {
			return Channel.valueOf(channelName);					
		} else {
			throw new RenderingControlException(RenderingControlErrorCode.INVALID_INSTANCE_ID);			
		}
	}
	
	public void firePlayerVolumneChangedEvent(UnsignedIntegerFourBytes instanceId) {
		try {
			this.appendCurrentState(getLastChange(), instanceId);
			this.getLastChange().fire(this.getPropertyChangeSupport());
		} catch (Throwable t) {
			LOGGER.warn("Updating lastChanges failed", t);
		}
	}
	
	@Override
	public UnsignedIntegerFourBytes[] getCurrentInstanceIds() {
		return instanceIds;
	}


	@Override
	public boolean getMute(UnsignedIntegerFourBytes instanceId, String channelName) throws RenderingControlException {				
		return this.rendererInstance.isMute(validateInstanceChannel(instanceId, channelName));
	}

	@Override
	public void setMute(UnsignedIntegerFourBytes instanceId, String channelName, boolean desiredMute) throws RenderingControlException {
		this.rendererInstance.setMute(validateInstanceChannel(instanceId, channelName), desiredMute);
	}

	@Override
	public UnsignedIntegerTwoBytes getVolume(UnsignedIntegerFourBytes instanceId, String channelName) throws RenderingControlException {
		return new UnsignedIntegerTwoBytes(this.rendererInstance.getVolume(validateInstanceChannel(instanceId, channelName)));
	}

	@Override
	public void setVolume(UnsignedIntegerFourBytes instanceId, String channelName, UnsignedIntegerTwoBytes desiredVolume) throws RenderingControlException {
		this.rendererInstance.setVolume(validateInstanceChannel(instanceId, channelName), desiredVolume.getValue());
	}

	@Override
	protected Channel[] getCurrentChannels() {
		return this.rendererInstance.getCurrentChannels();
	}
    
}
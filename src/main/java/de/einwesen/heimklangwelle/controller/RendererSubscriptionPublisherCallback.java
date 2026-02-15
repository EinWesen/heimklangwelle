package de.einwesen.heimklangwelle.controller;

import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.jupnp.model.gena.CancelReason;
import org.jupnp.model.gena.GENASubscription;
import org.jupnp.model.message.UpnpResponse;
import org.jupnp.model.meta.Service;
import org.jupnp.model.types.UnsignedIntegerFourBytes;
import org.jupnp.support.avtransport.lastchange.AVTransportLastChangeParser;
import org.jupnp.support.lastchange.EventedValue;
import org.jupnp.support.lastchange.LastChangeParser;
import org.jupnp.support.model.Channel;
import org.jupnp.support.model.TransportState;
import org.jupnp.support.renderingcontrol.lastchange.ChannelMute;
import org.jupnp.support.renderingcontrol.lastchange.ChannelVolume;
import org.jupnp.support.renderingcontrol.lastchange.RenderingControlLastChangeParser;

import de.einwesen.heimklangwelle.upnpsupport.LastChangeAwareSubscriptionCallback;

public abstract class RendererSubscriptionPublisherCallback extends LastChangeAwareSubscriptionCallback {
	public static final String[] SUPPORTED_PROPERTIES = {
			"AVTransportURIMetaData",
			"RelativeTimePosition",
			"AVTransportURI",
			"CurrentTrack",
			"NumberOfTracks",
			"TransportState",
			"Volume",	
			"Mute"		
	};
	
	public static final int SUBSCRIPTION_AVTRANSPORT = 1;
	public static final int SUBSCRIPTION_RENDERINGCONTROL = 2;
	
	private final Map<String, String> lastStates = Collections.synchronizedMap(new HashMap<>());
	
    /**
	 * @param service
	 * @param requestedDurationSeconds
	 * @param parser
	 */
	@SuppressWarnings("rawtypes")
	public RendererSubscriptionPublisherCallback(Service service, int requestedDurationSeconds, int subscriptionType) {
		super(service, requestedDurationSeconds, initLastChangeParser(subscriptionType));
	}
	
	private static LastChangeParser initLastChangeParser(int subscriptionType) {
		switch(subscriptionType) {
			case SUBSCRIPTION_AVTRANSPORT:
				return new AVTransportLastChangeParser();
			case SUBSCRIPTION_RENDERINGCONTROL:
				return new RenderingControlLastChangeParser();
			default:
				return null;
		}
	}

	@SuppressWarnings("rawtypes")
	@Override
	protected void eventedValueReceived(GENASubscription subscription, EventedValue<?> eventedValue) {
		final String value;
		
		switch (eventedValue.getName()) {
			case "AVTransportURIMetaData":
			case "RelativeTimePosition":
				value = getNonNullString(eventedValue.getValue());
				break;
			case "AVTransportURI":				
				value = getNonNullString((URI)eventedValue.getValue());
				break;
			case "CurrentTrack":
			case "NumberOfTracks":
				value = String.valueOf(getNonNullLong((UnsignedIntegerFourBytes)eventedValue.getValue()));
				break;
			case "TransportState":
				final TransportState transportState = (TransportState)eventedValue.getValue();
				value = (transportState != null ? transportState.name() : "");
				break;
			case "Volume":
				final ChannelVolume vol = (ChannelVolume)eventedValue.getValue();
				if (vol != null && vol.getChannel() == Channel.Master) {
					value = String.valueOf(vol.getVolume());
				} else {
					value = null;					
				}
				break;
			case "Mute":
				final ChannelMute mute = (ChannelMute)eventedValue.getValue();
				if (mute != null && mute.getChannel() == Channel.Master) {
					value = String.valueOf(mute.getMute());
				} else {
					value = null;
				}
				break;
			case "TransportStatus":
			case "CurrentMediaDuration":
			case "CurrentPlayMode":
			case "CurrentTrackURI":
			case "CurrentTransportActions":
			case "CurrentRecordQualityMode":
			case "CurrentTrackDuration":
			case "CurrentTrackMetaData":
			case "PossiblePlaybackStorageMedia":
			case "PossibleRecordQualityModes":
			case "PossibleRecordStorageMedia":
			case "NextAVTransportURI":
			case "NextAVTransportURIMetaData":
			case "RecordMediumWriteStatus":
			case "RecordStorageMedium":
			case "TransportPlaySpeed":
			case "VolumeDB":
			case "Loudness":
			case "PresetNameList":
			default:
				//not supported by us
				value = null;
				break;			
		}		
		
		if (value != null) {			
			if (!value.equals(lastStates.get(eventedValue.getName()))) {				
				publish(subscription, eventedValue.getName(), value);
				this.lastStates.put(eventedValue.getName(), value);
			}
		}		
	}

	@SuppressWarnings("rawtypes")
	@Override
	public void ended(GENASubscription subscription, CancelReason reason, UpnpResponse response) {
		super.ended(subscription, reason, response);
		stopped(subscription);
	}

	@SuppressWarnings("rawtypes")
	@Override
	public void failed(GENASubscription subscription, UpnpResponse responseStatus, Exception exception, String defaultMsg) {
		super.failed(subscription, responseStatus, exception, defaultMsg);
		stopped(subscription);
	}

	private static String getNonNullString(Object u) {
		if (u != null) {
			return u.toString();			
		} else {
			return "";
		}
	}
	
	private static long getNonNullLong(UnsignedIntegerFourBytes u) {
		if (u != null) {
			return u.getValue();
		} else {
			return -1;
		}
	}	
	
	@SuppressWarnings("rawtypes")
	protected abstract void stopped(GENASubscription subscription);
	@SuppressWarnings("rawtypes")
	protected abstract void publish(GENASubscription subscription, String propertyName, String propertyValueString);

}
package de.einwesen.heimklangwelle.upnpsupport;

import java.util.Map;

import org.jupnp.controlpoint.SubscriptionCallback;
import org.jupnp.model.gena.CancelReason;
import org.jupnp.model.gena.GENASubscription;
import org.jupnp.model.message.UpnpResponse;
import org.jupnp.model.meta.Service;
import org.jupnp.model.state.StateVariableValue;
import org.jupnp.support.lastchange.Event;
import org.jupnp.support.lastchange.EventedValue;
import org.jupnp.support.lastchange.InstanceID;
import org.jupnp.support.lastchange.LastChangeParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class LastChangeAwareSubscriptionCallback extends SubscriptionCallback {

	private static final Logger LOGGER = LoggerFactory.getLogger(LastChangeAwareSubscriptionCallback.class);
	
	private final LastChangeParser parser;
	
	protected LastChangeAwareSubscriptionCallback(@SuppressWarnings("rawtypes") Service service, int requestedDurationSeconds, LastChangeParser parser) {
		super(service, requestedDurationSeconds);
		this.parser = parser;
	}

    @Override
    public void established(@SuppressWarnings("rawtypes") GENASubscription subscription) {           	            	
    	LOGGER.debug("Subscription established for " + service.getServiceType() + " (" + subscription.getSubscriptionId() +")");	
    }

    @Override
    public void ended(@SuppressWarnings("rawtypes") GENASubscription subscription, CancelReason reason, UpnpResponse response) {
    	LOGGER.debug("Subscription to " + service.getServiceType() + " ended (" + subscription.getSubscriptionId() + ")");
    }

    @Override
    public void failed(@SuppressWarnings("rawtypes") GENASubscription subscription, UpnpResponse responseStatus, Exception exception, String defaultMsg) {
    	LOGGER.error("Subscription to " + service.getServiceType() + " failed ("+subscription.getSubscriptionId()+"): " + defaultMsg, exception);
    }

	@Override
	protected void eventsMissed(@SuppressWarnings("rawtypes") GENASubscription subscription, int numberOfMissedEvents) {
		LOGGER.trace("Subscription to "+service.getServiceType()+" missed events ("+subscription.getSubscriptionId()+"): " + numberOfMissedEvents);				
	}
	
	@Override
    public void eventReceived(@SuppressWarnings("rawtypes") GENASubscription subscription) {
		 @SuppressWarnings("unchecked")
		 Map<String, StateVariableValue<?>> currentValues = subscription.getCurrentValues();
		 currentValues.forEach((var, value) -> {
            if (var.equals("LastChange")) {
            	try {
					final Event lastChangeEvent = this.parser.parse(value.toString());
					eventReceived(subscription, lastChangeEvent);
				} catch (Exception e) {
					LOGGER.error("Could not parse lastChange", e);
				}
            } else {
            	eventedValueReceived(subscription, value);
            }
        });
    }

	protected void eventReceived(@SuppressWarnings("rawtypes") GENASubscription subscription, Event lastChangeEvent) {		
		for (InstanceID id : lastChangeEvent.getInstanceIDs()) {						
			for (EventedValue<?> ev :id.getValues()) {
				eventedValueReceived(subscription, ev);
			}
		}						
	}
	
	protected void eventedValueReceived(@SuppressWarnings("rawtypes") GENASubscription subscription, StateVariableValue<?> eventedValue) {
		LOGGER.warn("Subscription to " + service.getServiceType() + "recieved unexpected value ("+subscription.getSubscriptionId()+"): " + eventedValue.getStateVariable().getName());
	}	
	protected void failedParse(@SuppressWarnings("rawtypes") GENASubscription subscription, StateVariableValue<?> eventedValue, Exception e) {
		LOGGER.error("Subscription to " + service.getServiceType() + " failed at parsing ("+subscription.getSubscriptionId()+"): " + eventedValue.getStateVariable().getName(), e);
	}	

	protected abstract void eventedValueReceived(@SuppressWarnings("rawtypes")GENASubscription subscription, EventedValue<?> eventedValue);
}

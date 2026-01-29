package de.einwesen.heimklangwelle.upnpsupport;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.jupnp.binding.LocalServiceBindingException;
import org.jupnp.binding.annotations.AnnotationActionBinder;
import org.jupnp.binding.annotations.AnnotationLocalServiceBinder;
import org.jupnp.binding.annotations.UpnpAction;
import org.jupnp.model.action.ActionExecutor;
import org.jupnp.model.meta.Action;
import org.jupnp.model.meta.StateVariable;
import org.jupnp.model.state.StateVariableAccessor;
import org.jupnp.util.Reflections;

import de.einwesen.heimklangwelle.upnpsupport.annotations.UpnpExclude;

public class FilteredAnnotationLocalServiceBinderImpl extends AnnotationLocalServiceBinder {
	
	@SuppressWarnings("rawtypes")
	@Override
	protected Map<Action, ActionExecutor> readActions(Class<?> clazz, Map<StateVariable, StateVariableAccessor> stateVariables, Set<Class> stringConvertibleTypes) throws LocalServiceBindingException {

        Map<Action, ActionExecutor> map = new HashMap<>();

        for (Method method : Reflections.getMethods(clazz, UpnpAction.class)) {
            if (!method.isAnnotationPresent(UpnpExclude.class)) {
            	AnnotationActionBinder actionBinder = new AnnotationActionBinder(method, stateVariables, stringConvertibleTypes);
            	actionBinder.appendAction(map);            	
            }
        }

        return map;
	}


}

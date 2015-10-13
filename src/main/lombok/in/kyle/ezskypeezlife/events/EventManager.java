package in.kyle.ezskypeezlife.events;

import in.kyle.ezskypeezlife.EzSkype;
import lombok.Data;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Created by Kyle on 9/5/2015.
 */
public class EventManager {
    
    private List<HoldListener> listeners;
    
    public EventManager() {
        this.listeners = new ArrayList();
    }
    
    public void registerEvents(Object o) {
        for (Method m : o.getClass().getDeclaredMethods()) {
            if (m.getParameterCount() == 1) {
                Class type = m.getParameterTypes()[0];
                if (SkypeEvent.class.isAssignableFrom(type)) {
                    HoldListener holdListener = new HoldListener(o, m, type);
                    listeners.add(holdListener);
                }
            }
        }
    }
    
    public void unregisterEvents(Object o) {
        Iterator<HoldListener> listenerIterator = listeners.iterator();
        while (listenerIterator.hasNext()) {
            HoldListener listener = listenerIterator.next();
            if (o.equals(listener.getObject())) {
                listenerIterator.remove();
            }
        }
    }
    
    public void fire(SkypeEvent event) {
        listeners.stream().filter(l->l.getEvent().equals(event.getClass())).forEach(holdListener -> {
            try {
                holdListener.getMethod().invoke(holdListener.getObject(), event);
            } catch (Exception e) {
                EzSkype.LOGGER.error("Error while firing event: " + holdListener, e);
            }
        });
    }
    
    @Data
    private static class HoldListener {
        private final Object object;
        private final Method method;
        private final Class event;
    }
}

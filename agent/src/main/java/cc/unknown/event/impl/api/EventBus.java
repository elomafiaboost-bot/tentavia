/*
 * Decompiled with CFR 0.152.
 */
package cc.unknown.event.impl.api;

import cc.unknown.event.Event;
import cc.unknown.event.impl.EventLink;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.CopyOnWriteArrayList;

public class EventBus {
    private CopyOnWriteArrayList<Object> r = new CopyOnWriteArrayList();

    public void register(Object o) {
        if (this.r.contains(o)) {
            return;
        }
        this.r.add(o);
    }

    public void unregister(Object o) {
        this.r.remove(o);
    }

    public void post(Event e) {
        for (Object o : this.r) {
            Method[] m;
            Class<?> c = o.getClass();
            for (Method me : m = c.getDeclaredMethods()) {
                if (!me.isAnnotationPresent(EventLink.class) || me.getParameterCount() != 1 || me.getParameterTypes()[0] != e.getClass() || !me.getDeclaringClass().isAssignableFrom(c)) continue;
                try {
                    me.invoke(o, e);
                }
                catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
                    ex.printStackTrace();
                }
            }
        }
    }
}


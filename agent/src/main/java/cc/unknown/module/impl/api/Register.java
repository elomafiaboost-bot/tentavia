/*
 * Decompiled with CFR 0.152.
 */
package cc.unknown.module.impl.api;

import cc.unknown.module.impl.api.Category;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(value=RetentionPolicy.RUNTIME)
@Target(value={ElementType.TYPE})
public @interface Register {
    public String name();

    public Category category();

    public int key() default 0;

    public boolean enable() default false;
}


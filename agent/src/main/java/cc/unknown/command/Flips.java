/*
 * Decompiled with CFR 0.152.
 */
package cc.unknown.command;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(value=RetentionPolicy.RUNTIME)
@Target(value={ElementType.TYPE})
public @interface Flips {
    public String name();

    public String desc();

    public String alias();

    public String syntax();
}


package cc.unknown.transform;

import java.lang.instrument.ClassFileTransformer;
import java.security.ProtectionDomain;

/**
 * Intercepts vanilla Minecraft 1.8.9 class loading and injects event hooks via ASM.
 * Phase 1: stub — no transformations yet. Phase 2 will add method hooks.
 */
public class VanillaCoreTransformer implements ClassFileTransformer {

    // Internal (obfuscated) class names that we will transform in Phase 2
    private static final String[] TARGET_CLASS_NAMES = {
        "net/minecraft/client/Minecraft",
        "net/minecraft/client/entity/EntityPlayerSP",
        "net/minecraft/client/renderer/EntityRenderer",
        "net/minecraft/client/network/NetHandlerPlayClient",
    };

    @Override
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
                            ProtectionDomain domain, byte[] bytes) {
        if (className == null) return null;

        for (String target : TARGET_CLASS_NAMES) {
            if (className.equals(target)) {
                return transformClass(className, bytes);
            }
        }
        return null;
    }

    private byte[] transformClass(String className, byte[] bytes) {
        // Phase 2 will dispatch to per-class transformers here
        return null; // null = no change
    }

    /**
     * Returns the Class objects for all target classes so TentaviaAgent can retransform them.
     */
    public Class<?>[] getTargetClasses(ClassLoader loader) {
        java.util.List<Class<?>> result = new java.util.ArrayList<>();
        for (String name : TARGET_CLASS_NAMES) {
            String dotName = name.replace('/', '.');
            try {
                result.add(Class.forName(dotName, false, loader));
            } catch (ClassNotFoundException ignored) {}
        }
        return result.toArray(new Class<?>[0]);
    }
}

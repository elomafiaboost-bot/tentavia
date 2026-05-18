package cc.unknown;

import cc.unknown.transform.VanillaCoreTransformer;

import java.lang.instrument.Instrumentation;

public class TentaviaAgent {

    private static Instrumentation instrumentation;

    public static void agentmain(String args, Instrumentation inst) {
        instrumentation = inst;
        init(inst);
    }

    public static void premain(String args, Instrumentation inst) {
        instrumentation = inst;
        init(inst);
    }

    public static void init(Instrumentation inst) {
        VanillaCoreTransformer transformer = new VanillaCoreTransformer();
        inst.addTransformer(transformer, true);

        Thread t = new Thread(() -> startWhenReady(inst, transformer), "Tentavia-Init");
        t.setDaemon(true);
        t.start();
    }

    private static void startWhenReady(Instrumentation inst, VanillaCoreTransformer transformer) {
        // Wait for Minecraft to be loaded in the classloader
        Class<?> mcClass = null;
        while (mcClass == null) {
            try {
                mcClass = Class.forName("net.minecraft.client.Minecraft");
            } catch (ClassNotFoundException ignored) {}
            try { Thread.sleep(200); } catch (InterruptedException e) { return; }
        }

        // Retransform key classes so our hooks are injected
        try {
            inst.retransformClasses(transformer.getTargetClasses(mcClass.getClassLoader()));
        } catch (Throwable e) {
            System.err.println("[Tentavia] Retransform falhou: " + e.getMessage());
        }

        // Wait for the player to be in-game before starting modules
        while (true) {
            try {
                Object mc = mcClass.getMethod("func_71410_x").invoke(null);
                if (mc != null) {
                    Object player = mc.getClass().getField("field_71439_g").get(mc);
                    if (player != null) break;
                }
            } catch (Throwable ignored) {}
            try { Thread.sleep(200); } catch (InterruptedException e) { return; }
        }

        try {
            Haru.instance.startClient();
            System.out.println("[Tentavia] Cliente iniciado.");
        } catch (Throwable e) {
            System.err.println("[Tentavia] Erro ao iniciar:");
            e.printStackTrace();
        }
    }

    public static Instrumentation getInstrumentation() {
        return instrumentation;
    }
}

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

    // SRG (deobf/Forge) vs OBF (vanilla 1.8.9) lookup tables.
    // Method/field names differ; we try SRG first, fall back to OBF.
    private static final String[] MC_CLASS_NAMES   = { "net.minecraft.client.Minecraft", "bib" };
    private static final String[] MC_GETTER_NAMES  = { "func_71410_x", "A" };
    private static final String[] PLAYER_FIELD_NAMES = { "field_71439_g", "h" };

    private static void startWhenReady(Instrumentation inst, VanillaCoreTransformer transformer) {
        // Wait for Minecraft class to appear in any classloader (SRG or OBF).
        Class<?> mcClass = null;
        while (mcClass == null) {
            for (String name : MC_CLASS_NAMES) {
                try { mcClass = Class.forName(name); break; } catch (ClassNotFoundException ignored) {}
            }
            try { Thread.sleep(200); } catch (InterruptedException e) { return; }
        }

        // Retransform key classes so our hooks are injected (agentmain path; premain catches loads).
        try {
            inst.retransformClasses(transformer.getTargetClasses(mcClass.getClassLoader()));
        } catch (Throwable e) {
            System.err.println("[Tentavia] Retransform falhou: " + e.getMessage());
        }

        // Wait for the player to be in-game before starting modules.
        // Try each known getter/field name so both SRG and OBF runtimes work.
        while (true) {
            try {
                Object mc = null;
                for (String getter : MC_GETTER_NAMES) {
                    try { mc = mcClass.getMethod(getter).invoke(null); break; } catch (Throwable ignored) {}
                }
                if (mc != null) {
                    Object player = null;
                    for (String field : PLAYER_FIELD_NAMES) {
                        try { player = mc.getClass().getField(field).get(mc); break; } catch (Throwable ignored) {}
                    }
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

package cc.unknown;

import cc.unknown.bridge.ForgeBridge;
import net.minecraft.client.Minecraft;
import net.minecraftforge.common.MinecraftForge;

import java.lang.instrument.Instrumentation;

public class TentaviaAgent {

    private static Instrumentation instrumentation;

    /**
     * Called when the agent is attached via -javaagent or JVMTI loadAgent.
     */
    public static void agentmain(String args, Instrumentation inst) {
        instrumentation = inst;
        init();
    }

    public static void premain(String args, Instrumentation inst) {
        instrumentation = inst;
        init();
    }

    /**
     * Called from the C++ DLL after loading this JAR into the game classloader.
     * Starts the client in a separate thread to avoid blocking the injector.
     */
    public static void init() {
        Thread t = new Thread(TentaviaAgent::startWhenReady, "Tentavia-Init");
        t.setDaemon(true);
        t.start();
    }

    private static void startWhenReady() {
        // Wait for Minecraft to fully initialize
        while (true) {
            try {
                Minecraft mc = Minecraft.func_71410_x();
                if (mc != null && mc.field_71439_g != null) {
                    break;
                }
            } catch (Throwable ignored) {}
            try { Thread.sleep(200); } catch (InterruptedException e) { return; }
        }

        try {
            // Register Forge event bridge (translates Forge events -> Haru events)
            ForgeBridge bridge = new ForgeBridge();
            MinecraftForge.EVENT_BUS.register(bridge);

            // Start the Haru client
            Haru.instance.startClient();

            System.out.println("[Tentavia] Cliente iniciado com sucesso.");
        } catch (Throwable e) {
            System.err.println("[Tentavia] Erro ao iniciar cliente:");
            e.printStackTrace();
        }
    }

    public static Instrumentation getInstrumentation() {
        return instrumentation;
    }
}

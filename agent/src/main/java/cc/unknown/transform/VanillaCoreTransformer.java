package cc.unknown.transform;

import cc.unknown.transform.visitor.HookMethodVisitor;
import org.objectweb.asm.*;

import java.lang.instrument.ClassFileTransformer;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.List;

import static org.objectweb.asm.Opcodes.*;

/**
 * Intercepts vanilla Minecraft 1.8.9 class loading (premain) or retransformation (agentmain)
 * and injects event-dispatch hooks via ASM bytecode manipulation.
 *
 * Target classes use SRG names (func_*, field_*) — the launcher (Phase 4) ensures the JVM
 * sees deobfuscated names before these classes load.
 *
 * Hooks injected:
 *   Minecraft            runTick          → TickEvent.Pre / Post
 *   EntityPlayerSP       onUpdate         → PreUpdateEvent, MotionEvent.Pre / Post
 *   EntityRenderer       updateCamera…    → RenderEvent.Render2D
 *   EntityRenderer       renderWorldPass  → RenderEvent.Render3D
 *   NetHandlerPlayClient processXxx       → PacketEvent (receive, all server packets)
 *   NetworkManager       sendPacket       → PacketEvent (send, cancellable)
 *
 * Interfaces injected (methods appended to class):
 *   NetworkManager       sendPacketNoEvent(Packet)           → INetworkManager
 *   NetHandlerPlayClient receiveQueueNoEvent(Packet)         → INetHandlerPlayClient
 *   Minecraft            setSession(Session)                 → IMinecraft
 */
public class VanillaCoreTransformer implements ClassFileTransformer {

    private static final String HOOKS = "cc/unknown/transform/hooks/VanillaHooks";

    private static final String MC_CLASS      = "net/minecraft/client/Minecraft";
    private static final String PLAYER_CLASS  = "net/minecraft/client/entity/EntityPlayerSP";
    private static final String RENDERER_CLASS= "net/minecraft/client/renderer/EntityRenderer";
    private static final String NETHANDLER_CLASS = "net/minecraft/client/network/NetHandlerPlayClient";
    private static final String NETMGR_CLASS  = "net/minecraft/network/NetworkManager";

    private static final String[] TARGET_CLASS_NAMES = {
        MC_CLASS, PLAYER_CLASS, RENDERER_CLASS, NETHANDLER_CLASS, NETMGR_CLASS
    };

    @Override
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
                            ProtectionDomain domain, byte[] bytes) {
        if (className == null) return null;
        for (String target : TARGET_CLASS_NAMES) {
            if (className.equals(target)) {
                try {
                    return transformClass(className, bytes);
                } catch (Throwable t) {
                    System.err.println("[Tentavia] ASM transform failed for " + className + ": " + t);
                    return null;
                }
            }
        }
        return null;
    }

    private byte[] transformClass(String className, byte[] bytes) {
        ClassReader cr = new ClassReader(bytes);
        ClassWriter cw = new ClassWriter(cr, ClassWriter.COMPUTE_FRAMES) {
            @Override
            protected String getCommonSuperClass(String type1, String type2) {
                try {
                    return super.getCommonSuperClass(type1, type2);
                } catch (Throwable t) {
                    return "java/lang/Object";
                }
            }
        };

        ClassVisitor cv;
        switch (className) {
            case MC_CLASS:       cv = new MinecraftVisitor(cw);     break;
            case PLAYER_CLASS:   cv = new EntityPlayerVisitor(cw);  break;
            case RENDERER_CLASS: cv = new EntityRendererVisitor(cw);break;
            case NETHANDLER_CLASS: cv = new NetHandlerVisitor(cw);  break;
            case NETMGR_CLASS:   cv = new NetworkManagerVisitor(cw);break;
            default: return null;
        }

        cr.accept(cv, ClassReader.SKIP_FRAMES);
        return cw.toByteArray();
    }

    // ── Minecraft ────────────────────────────────────────────────────────────
    // Hooks: runTick (func_71407_l, ()V) → TickEvent
    // Injects: setSession(Session) → IMinecraft

    private static class MinecraftVisitor extends ClassVisitor {
        MinecraftVisitor(ClassVisitor cv) { super(ASM5, cv); }

        @Override
        public MethodVisitor visitMethod(int access, String name, String desc,
                                         String signature, String[] exceptions) {
            MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
            if ("func_71407_l".equals(name) && "()V".equals(desc)) {
                return new HookMethodVisitor(mv,
                    w -> {
                        w.visitVarInsn(ALOAD, 0);
                        w.visitMethodInsn(INVOKESTATIC, HOOKS, "onRunTickPre", "(Ljava/lang/Object;)V", false);
                    },
                    w -> {
                        w.visitVarInsn(ALOAD, 0);
                        w.visitMethodInsn(INVOKESTATIC, HOOKS, "onRunTickPost", "(Ljava/lang/Object;)V", false);
                    }
                );
            }
            return mv;
        }

        @Override
        public void visitEnd() {
            // Inject: public void setSession(net.minecraft.util.Session session)
            // Implements IMinecraft — allows AltLoginScreen to change the MC session.
            MethodVisitor mv = super.visitMethod(ACC_PUBLIC,
                "setSession",
                "(Lnet/minecraft/util/Session;)V",
                null, null);
            mv.visitCode();
            mv.visitVarInsn(ALOAD, 0);
            mv.visitVarInsn(ALOAD, 1);
            mv.visitFieldInsn(PUTFIELD, MC_CLASS, "field_71449_j", "Lnet/minecraft/util/Session;");
            mv.visitInsn(RETURN);
            mv.visitMaxs(2, 2);
            mv.visitEnd();
            super.visitEnd();
        }
    }

    // ── EntityPlayerSP ───────────────────────────────────────────────────────
    // Hooks: onUpdate (func_70071_h_, ()V) → MotionEvent / PreUpdateEvent

    private static class EntityPlayerVisitor extends ClassVisitor {
        EntityPlayerVisitor(ClassVisitor cv) { super(ASM5, cv); }

        @Override
        public MethodVisitor visitMethod(int access, String name, String desc,
                                         String signature, String[] exceptions) {
            MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
            if ("func_70071_h_".equals(name) && "()V".equals(desc)) {
                return new HookMethodVisitor(mv,
                    w -> {
                        w.visitVarInsn(ALOAD, 0);
                        w.visitMethodInsn(INVOKESTATIC, HOOKS, "onPlayerUpdatePre", "(Ljava/lang/Object;)V", false);
                    },
                    w -> {
                        w.visitVarInsn(ALOAD, 0);
                        w.visitMethodInsn(INVOKESTATIC, HOOKS, "onPlayerUpdatePost", "(Ljava/lang/Object;)V", false);
                    }
                );
            }
            return mv;
        }
    }

    // ── EntityRenderer ───────────────────────────────────────────────────────
    // Hooks:
    //   func_78480_b  (F)V  updateCameraAndRender → RenderEvent.Render2D
    //   func_175068_a (FJ)V renderWorldPass        → RenderEvent.Render3D

    private static class EntityRendererVisitor extends ClassVisitor {
        EntityRendererVisitor(ClassVisitor cv) { super(ASM5, cv); }

        @Override
        public MethodVisitor visitMethod(int access, String name, String desc,
                                         String signature, String[] exceptions) {
            MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
            if ("func_78480_b".equals(name) && "(F)V".equals(desc)) {
                // onRender2DPre(Object renderer, float partialTicks)
                return new HookMethodVisitor(mv,
                    w -> {
                        w.visitVarInsn(ALOAD, 0);
                        w.visitVarInsn(FLOAD, 1);
                        w.visitMethodInsn(INVOKESTATIC, HOOKS, "onRender2DPre", "(Ljava/lang/Object;F)V", false);
                    },
                    null
                );
            }
            if ("func_175068_a".equals(name) && "(FJ)V".equals(desc)) {
                // onRender3DPre(Object renderer, float partialTicks) — J (long) is arg at slots 2+3
                return new HookMethodVisitor(mv,
                    w -> {
                        w.visitVarInsn(ALOAD, 0);
                        w.visitVarInsn(FLOAD, 1);
                        w.visitMethodInsn(INVOKESTATIC, HOOKS, "onRender3DPre", "(Ljava/lang/Object;F)V", false);
                    },
                    null
                );
            }
            return mv;
        }
    }

    // ── NetHandlerPlayClient ─────────────────────────────────────────────────
    // Hooks: every processXxx method that takes exactly one server-bound packet arg.
    //   Pattern: name starts with "func_" AND desc matches (Lnet/minecraft/network/play/server/S...;)V
    // Injects: receiveQueueNoEvent(Packet) → INetHandlerPlayClient

    private static class NetHandlerVisitor extends ClassVisitor {
        NetHandlerVisitor(ClassVisitor cv) { super(ASM5, cv); }

        @Override
        public MethodVisitor visitMethod(int access, String name, String desc,
                                         String signature, String[] exceptions) {
            MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
            if (name.startsWith("func_")
                    && desc.startsWith("(Lnet/minecraft/network/play/server/S")
                    && desc.endsWith(";)V")) {
                return new HookMethodVisitor(mv,
                    w -> {
                        w.visitVarInsn(ALOAD, 0);
                        w.visitVarInsn(ALOAD, 1);
                        w.visitMethodInsn(INVOKESTATIC, HOOKS, "onReceivePacket",
                            "(Ljava/lang/Object;Ljava/lang/Object;)V", false);
                    },
                    null
                );
            }
            return mv;
        }

        @Override
        public void visitEnd() {
            // Inject: public void receiveQueueNoEvent(Packet<?> packet)
            // Implements INetHandlerPlayClient — bypasses the receive PacketEvent.
            MethodVisitor mv = super.visitMethod(ACC_PUBLIC,
                "receiveQueueNoEvent",
                "(Lnet/minecraft/network/Packet;)V",
                "(Lnet/minecraft/network/Packet<*>;)V",
                null);
            mv.visitCode();
            mv.visitVarInsn(ALOAD, 0);
            mv.visitVarInsn(ALOAD, 1);
            mv.visitMethodInsn(INVOKESTATIC, HOOKS, "receiveQueueNoEventImpl",
                "(Ljava/lang/Object;Ljava/lang/Object;)V", false);
            mv.visitInsn(RETURN);
            mv.visitMaxs(2, 2);
            mv.visitEnd();
            super.visitEnd();
        }
    }

    // ── NetworkManager ───────────────────────────────────────────────────────
    // Hooks: sendPacket (func_150732_b, (Packet;GenericFutureListener[];)V) — cancellable
    // Injects: sendPacketNoEvent(Packet) → INetworkManager

    private static final String SEND_DESC =
        "(Lnet/minecraft/network/Packet;[Lio/netty/util/concurrent/GenericFutureListener;)V";

    private static class NetworkManagerVisitor extends ClassVisitor {
        NetworkManagerVisitor(ClassVisitor cv) { super(ASM5, cv); }

        @Override
        public MethodVisitor visitMethod(int access, String name, String desc,
                                         String signature, String[] exceptions) {
            MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
            if ("func_150732_b".equals(name) && SEND_DESC.equals(desc)) {
                // Inject at method entry: call onSendPacket(this, packet).
                // If it returns true (cancelled), return immediately without sending.
                return new HookMethodVisitor(mv,
                    w -> {
                        w.visitVarInsn(ALOAD, 0);
                        w.visitVarInsn(ALOAD, 1);
                        w.visitMethodInsn(INVOKESTATIC, HOOKS, "onSendPacket",
                            "(Ljava/lang/Object;Ljava/lang/Object;)Z", false);
                        Label notCancelled = new Label();
                        w.visitJumpInsn(IFEQ, notCancelled);
                        w.visitInsn(RETURN);
                        w.visitLabel(notCancelled);
                    },
                    null
                );
            }
            return mv;
        }

        @Override
        public void visitEnd() {
            // Inject: public void sendPacketNoEvent(Packet<?> packet)
            // Implements INetworkManager — bypasses the send PacketEvent.
            MethodVisitor mv = super.visitMethod(ACC_PUBLIC,
                "sendPacketNoEvent",
                "(Lnet/minecraft/network/Packet;)V",
                "(Lnet/minecraft/network/Packet<*>;)V",
                null);
            mv.visitCode();
            mv.visitVarInsn(ALOAD, 0);
            mv.visitVarInsn(ALOAD, 1);
            mv.visitMethodInsn(INVOKESTATIC, HOOKS, "sendPacketNoEventImpl",
                "(Ljava/lang/Object;Ljava/lang/Object;)V", false);
            mv.visitInsn(RETURN);
            mv.visitMaxs(2, 2);
            mv.visitEnd();
            super.visitEnd();
        }
    }

    /**
     * Returns the Class objects for all target classes so TentaviaAgent can retransform them
     * (only needed for agentmain attach — premain intercepts classes before they load).
     */
    public Class<?>[] getTargetClasses(ClassLoader loader) {
        List<Class<?>> result = new ArrayList<>();
        for (String name : TARGET_CLASS_NAMES) {
            try {
                result.add(Class.forName(name.replace('/', '.'), false, loader));
            } catch (ClassNotFoundException ignored) {}
        }
        return result.toArray(new Class<?>[0]);
    }
}

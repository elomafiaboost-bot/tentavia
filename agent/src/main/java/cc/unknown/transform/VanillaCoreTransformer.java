package cc.unknown.transform;

import cc.unknown.transform.visitor.HookMethodVisitor;
import org.objectweb.asm.*;

import java.lang.instrument.ClassFileTransformer;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.List;

import static org.objectweb.asm.Opcodes.*;

/**
 * Intercepts Minecraft 1.8.9 class loading and injects event-dispatch hooks via ASM.
 *
 * Supports two naming environments detected automatically at transform time:
 *
 *   SRG mode  — classes loaded with MCP/Forge deobfuscation (func_*, field_* names).
 *               Used when running under a Forge launch profile or a deobfuscating launcher.
 *
 *   OBF mode  — truly vanilla obfuscated class names (bib, bjp, bjl, bew, mn).
 *               Used when launched by tentavia's premain launcher directly.
 *               In this mode, method hooks are matched by SRG name where known or
 *               by descriptor where the name is uniquely identifying.
 *               Field access in VanillaHooks falls back to the obfuscated field-name
 *               table in {@link cc.unknown.transform.hooks.VanillaHooks}.
 *
 * Hooks injected:
 *   Minecraft / bib              runTick          → TickEvent.Pre / Post
 *   EntityPlayerSP / bjp         onUpdate         → PreUpdateEvent, MotionEvent.Pre / Post
 *   EntityRenderer / bjl         updateCamera…    → RenderEvent.Render2D
 *   EntityRenderer / bjl         renderWorldPass  → RenderEvent.Render3D
 *   NetHandlerPlayClient / bew   processXxx       → PacketEvent (receive)
 *   NetworkManager / mn          sendPacket       → PacketEvent (send, cancellable)
 *
 * Interfaces injected (new methods appended to class):
 *   NetworkManager / mn          sendPacketNoEvent(Packet)    → INetworkManager
 *   NetHandlerPlayClient / bew   receiveQueueNoEvent(Packet)  → INetHandlerPlayClient
 *   Minecraft / bib              setSession(Session)          → IMinecraft
 */
public class VanillaCoreTransformer implements ClassFileTransformer {

    private static final String HOOKS = "cc/unknown/transform/hooks/VanillaHooks";

    // ── SRG (deobfuscated) class names ───────────────────────────────────────
    private static final String MC_CLASS         = "net/minecraft/client/Minecraft";
    private static final String PLAYER_CLASS     = "net/minecraft/client/entity/EntityPlayerSP";
    private static final String RENDERER_CLASS   = "net/minecraft/client/renderer/EntityRenderer";
    private static final String NETHANDLER_CLASS = "net/minecraft/client/network/NetHandlerPlayClient";
    private static final String NETMGR_CLASS     = "net/minecraft/network/NetworkManager";

    // ── Obfuscated (vanilla 1.8.9) class names ───────────────────────────────
    // Source: MCP 9.10 for MC 1.8.9 (client-1.8.9.srg).
    private static final String OBF_MC_CLASS         = "bib";
    private static final String OBF_PLAYER_CLASS     = "bjp";
    private static final String OBF_RENDERER_CLASS   = "bjl";
    private static final String OBF_NETHANDLER_CLASS = "bew";
    private static final String OBF_NETMGR_CLASS     = "mn";

    private static final String[] TARGET_CLASS_NAMES = {
        // SRG names (Forge / deobf launcher)
        MC_CLASS, PLAYER_CLASS, RENDERER_CLASS, NETHANDLER_CLASS, NETMGR_CLASS,
        // Obfuscated names (vanilla premain)
        OBF_MC_CLASS, OBF_PLAYER_CLASS, OBF_RENDERER_CLASS,
        OBF_NETHANDLER_CLASS, OBF_NETMGR_CLASS
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

        // Detect environment: if class is loaded with its obfuscated name, we're in OBF mode.
        boolean obf = className.equals(OBF_MC_CLASS)   || className.equals(OBF_PLAYER_CLASS)
                   || className.equals(OBF_RENDERER_CLASS) || className.equals(OBF_NETHANDLER_CLASS)
                   || className.equals(OBF_NETMGR_CLASS);

        ClassVisitor cv;
        switch (className) {
            case MC_CLASS:
            case OBF_MC_CLASS:
                cv = new MinecraftVisitor(cw, obf);     break;
            case PLAYER_CLASS:
            case OBF_PLAYER_CLASS:
                cv = new EntityPlayerVisitor(cw, obf);  break;
            case RENDERER_CLASS:
            case OBF_RENDERER_CLASS:
                cv = new EntityRendererVisitor(cw, obf);break;
            case NETHANDLER_CLASS:
            case OBF_NETHANDLER_CLASS:
                cv = new NetHandlerVisitor(cw, obf);    break;
            case NETMGR_CLASS:
            case OBF_NETMGR_CLASS:
                cv = new NetworkManagerVisitor(cw, obf);break;
            default: return null;
        }

        cr.accept(cv, ClassReader.SKIP_FRAMES);
        return cw.toByteArray();
    }

    // ── Minecraft  /  bib ───────────────────────────────────────────────────
    // SRG hook  : func_71407_l  ()V  → TickEvent
    // OBF hook  : descriptor-based — the only private final void()V that
    //             calls runGameLoop internals (matched by name prefix absence).
    // Injects   : setSession(Session) → IMinecraft
    //
    // OBF method names (MCP 9.10 / 1.8.9):
    //   runTick = "bq"  (verify against client-1.8.9.srg if behaviour changes)

    private static class MinecraftVisitor extends ClassVisitor {
        private final boolean obf;
        MinecraftVisitor(ClassVisitor cv, boolean obf) { super(ASM5, cv); this.obf = obf; }

        @Override
        public MethodVisitor visitMethod(int access, String name, String desc,
                                         String signature, String[] exceptions) {
            MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
            boolean match = obf
                ? ("bq".equals(name) && "()V".equals(desc))       // obfuscated runTick
                : ("func_71407_l".equals(name) && "()V".equals(desc));
            if (match) {
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
            // In obf mode the session field is named "B"; in SRG mode it's "field_71449_j".
            // We use the SRG name here because the PUTFIELD target is the actual MC class
            // whose bytecode we're rewriting — at transform time the field still has its
            // original (obfuscated) name. The SRG name is only valid in Forge/deobf mode.
            // In OBF mode we skip the injection (obf runtime does not expose setSession).
            if (!obf) {
                mv.visitFieldInsn(PUTFIELD, MC_CLASS, "field_71449_j", "Lnet/minecraft/util/Session;");
            } else {
                // OBF: session field is "B" in bib (1.8.9). PUTFIELD bib.B
                mv.visitFieldInsn(PUTFIELD, OBF_MC_CLASS, "B", "Lnet/minecraft/util/Session;");
            }
            mv.visitInsn(RETURN);
            mv.visitMaxs(2, 2);
            mv.visitEnd();
            super.visitEnd();
        }
    }

    // ── EntityPlayerSP  /  bjp ──────────────────────────────────────────────
    // SRG hook : func_70071_h_  ()V  → MotionEvent / PreUpdateEvent
    // OBF hook : "e"  ()V  (MCP 9.10 / 1.8.9: onUpdate → e)

    private static class EntityPlayerVisitor extends ClassVisitor {
        private final boolean obf;
        EntityPlayerVisitor(ClassVisitor cv, boolean obf) { super(ASM5, cv); this.obf = obf; }

        @Override
        public MethodVisitor visitMethod(int access, String name, String desc,
                                         String signature, String[] exceptions) {
            MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
            boolean match = obf
                ? ("e".equals(name) && "()V".equals(desc))
                : ("func_70071_h_".equals(name) && "()V".equals(desc));
            if (match) {
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

    // ── EntityRenderer  /  bjl ──────────────────────────────────────────────
    // SRG hooks:
    //   func_78480_b  (F)V  updateCameraAndRender → RenderEvent.Render2D
    //   func_175068_a (FJ)V renderWorldPass        → RenderEvent.Render3D
    // OBF hooks (MCP 9.10 / 1.8.9):
    //   updateCameraAndRender → "b"   (F)V
    //   renderWorldPass       → "a"   (FJ)V

    private static class EntityRendererVisitor extends ClassVisitor {
        private final boolean obf;
        EntityRendererVisitor(ClassVisitor cv, boolean obf) { super(ASM5, cv); this.obf = obf; }

        @Override
        public MethodVisitor visitMethod(int access, String name, String desc,
                                         String signature, String[] exceptions) {
            MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
            boolean render2D = obf
                ? ("b".equals(name) && "(F)V".equals(desc))
                : ("func_78480_b".equals(name) && "(F)V".equals(desc));
            boolean render3D = obf
                ? ("a".equals(name) && "(FJ)V".equals(desc))
                : ("func_175068_a".equals(name) && "(FJ)V".equals(desc));

            if (render2D) {
                return new HookMethodVisitor(mv,
                    w -> {
                        w.visitVarInsn(ALOAD, 0);
                        w.visitVarInsn(FLOAD, 1);
                        w.visitMethodInsn(INVOKESTATIC, HOOKS, "onRender2DPre", "(Ljava/lang/Object;F)V", false);
                    },
                    null
                );
            }
            if (render3D) {
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

    // ── NetHandlerPlayClient  /  bew ────────────────────────────────────────
    // SRG  : name starts with "func_" AND desc starts with (Lnet/minecraft/network/play/server/S
    // OBF  : methods are single-char ("a", "b" …) taking an obfuscated packet type.
    //         In OBF mode we match by: desc ends with ";)V" AND single-arg
    //         AND the single arg starts with "L" (reference type).
    //         This is wide but safe — each processXxx has a unique packet param.
    // Injects: receiveQueueNoEvent(Packet) → INetHandlerPlayClient

    private static class NetHandlerVisitor extends ClassVisitor {
        private final boolean obf;
        NetHandlerVisitor(ClassVisitor cv, boolean obf) { super(ASM5, cv); this.obf = obf; }

        private static boolean isSrgProcess(String name, String desc) {
            return name.startsWith("func_")
                && desc.startsWith("(Lnet/minecraft/network/play/server/S")
                && desc.endsWith(";)V");
        }

        private static boolean isObfProcess(String name, String desc) {
            // Single-letter name, single reference-type arg, returns void
            if (name.length() > 2) return false;
            if (!desc.endsWith(";)V")) return false;
            // Ensure it's exactly one arg: desc form "(L...;)V"
            return desc.startsWith("(L") && desc.indexOf(';') == desc.length() - 3;
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String desc,
                                         String signature, String[] exceptions) {
            MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
            boolean match = obf ? isObfProcess(name, desc) : isSrgProcess(name, desc);
            if (match) {
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

    // ── NetworkManager  /  mn ───────────────────────────────────────────────
    // SRG hook : func_150732_b  (Packet;GenericFutureListener[];)V  — cancellable
    // OBF hook : Netty's GenericFutureListener is NOT obfuscated, so we match any
    //            method whose descriptor ends with "[Lio/netty/util/concurrent/GenericFutureListener;)V"
    //            (there is exactly one such method in mn — the sendPacket overload).
    // Injects  : sendPacketNoEvent(Packet) → INetworkManager

    private static final String SEND_DESC =
        "(Lnet/minecraft/network/Packet;[Lio/netty/util/concurrent/GenericFutureListener;)V";
    private static final String SEND_DESC_SUFFIX =
        "[Lio/netty/util/concurrent/GenericFutureListener;)V";

    private static class NetworkManagerVisitor extends ClassVisitor {
        private final boolean obf;
        NetworkManagerVisitor(ClassVisitor cv, boolean obf) { super(ASM5, cv); this.obf = obf; }

        private boolean isSendPacket(String name, String desc) {
            if (obf) {
                // In vanilla the first arg is an obfuscated Packet type, but Netty stays clear.
                return name.length() <= 2 && desc.endsWith(SEND_DESC_SUFFIX);
            }
            return "func_150732_b".equals(name) && SEND_DESC.equals(desc);
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String desc,
                                         String signature, String[] exceptions) {
            MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
            if (isSendPacket(name, desc)) {
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

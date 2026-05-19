package cc.unknown.transform.hooks;

import cc.unknown.Haru;
import cc.unknown.event.impl.move.MotionEvent;
import cc.unknown.event.impl.move.PreUpdateEvent;
import cc.unknown.event.impl.network.PacketEvent;
import cc.unknown.event.impl.player.TickEvent;
import cc.unknown.event.impl.render.RenderEvent;
import net.minecraft.network.EnumPacketDirection;
import net.minecraft.network.Packet;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Static hook methods called from ASM-injected bytecode in vanilla MC 1.8.9 classes.
 *
 * Naming convention for Haru's PacketEvent (directions are inverted vs MC convention):
 *   CLIENTBOUND → isSend() == true  (client sending to server)
 *   SERVERBOUND → isReceive() == true (server sending to client)
 *
 * skipSendEvent / skipReceiveEvent: ThreadLocal flags set by NoEvent helpers
 * so that PacketUtil.sendPacketNoEvent / receivePacketNoEvent bypass the event.
 */
public final class VanillaHooks {

    public static final ThreadLocal<Boolean> skipSendEvent    = ThreadLocal.withInitial(() -> Boolean.FALSE);
    public static final ThreadLocal<Boolean> skipReceiveEvent = ThreadLocal.withInitial(() -> Boolean.FALSE);

    private VanillaHooks() {}

    private static boolean ready() {
        try {
            return Haru.instance.getEventBus() != null;
        } catch (Throwable t) {
            return false;
        }
    }

    // ── Minecraft.runTick  (func_71407_l, ()V) ───────────────────────────────

    public static void onRunTickPre(Object mc) {
        if (!ready()) return;
        Haru.instance.getEventBus().post(new TickEvent.Pre());
    }

    public static void onRunTickPost(Object mc) {
        if (!ready()) return;
        Haru.instance.getEventBus().post(new TickEvent.Post());
    }

    // ── EntityPlayerSP.onUpdate  (func_70071_h_, ()V) ────────────────────────

    public static void onPlayerUpdatePre(Object player) {
        if (!ready()) return;
        Haru.instance.getEventBus().post(new PreUpdateEvent());
        try {
            Class<?> cls = player.getClass();
            double  x        = getDouble(cls, player, "field_70165_t");
            double  y        = getDouble(cls, player, "field_70163_u");
            double  z        = getDouble(cls, player, "field_70161_v");
            float   yaw      = getFloat(cls, player, "field_70177_z");
            float   pitch    = getFloat(cls, player, "field_70125_A");
            boolean onGround = getBool(cls, player, "field_70122_E");

            MotionEvent event = new MotionEvent(MotionEvent.MotionType.Pre, x, y, z, yaw, pitch, onGround);
            Haru.instance.getEventBus().post(event);

            if (event.getX()        != x)        setDouble(cls, player, "field_70165_t", event.getX());
            if (event.getY()        != y)        setDouble(cls, player, "field_70163_u", event.getY());
            if (event.getZ()        != z)        setDouble(cls, player, "field_70161_v", event.getZ());
            if (event.getYaw()      != yaw)      setFloat(cls, player, "field_70177_z", event.getYaw());
            if (event.getPitch()    != pitch)    setFloat(cls, player, "field_70125_A", event.getPitch());
            if (event.isOnGround()  != onGround) setBool(cls, player, "field_70122_E", event.isOnGround());
        } catch (Throwable ignored) {}
    }

    public static void onPlayerUpdatePost(Object player) {
        if (!ready()) return;
        try {
            Class<?> cls = player.getClass();
            double  x        = getDouble(cls, player, "field_70165_t");
            double  y        = getDouble(cls, player, "field_70163_u");
            double  z        = getDouble(cls, player, "field_70161_v");
            float   yaw      = getFloat(cls, player, "field_70177_z");
            float   pitch    = getFloat(cls, player, "field_70125_A");
            boolean onGround = getBool(cls, player, "field_70122_E");
            Haru.instance.getEventBus().post(
                new MotionEvent(MotionEvent.MotionType.Post, x, y, z, yaw, pitch, onGround));
        } catch (Throwable ignored) {}
    }

    // ── EntityRenderer.updateCameraAndRender  (func_78480_b, (F)V) ──────────

    public static void onRender2DPre(Object renderer, float partialTicks) {
        if (!ready()) return;
        Haru.instance.getEventBus().post(new RenderEvent(RenderEvent.RenderType.Render2D, partialTicks));
    }

    // ── EntityRenderer.renderWorldPass  (func_175068_a, (FJ)V) ─────────────

    public static void onRender3DPre(Object renderer, float partialTicks) {
        if (!ready()) return;
        Haru.instance.getEventBus().post(new RenderEvent(RenderEvent.RenderType.Render3D, partialTicks));
    }

    // ── NetworkManager.sendPacket  (func_150732_b) ───────────────────────────
    // Returns true if the packet was cancelled and the send should be skipped.

    @SuppressWarnings("unchecked")
    public static boolean onSendPacket(Object netManager, Object packet) {
        if (skipSendEvent.get()) return false;
        if (!ready()) return false;
        try {
            PacketEvent event = new PacketEvent(EnumPacketDirection.CLIENTBOUND, (Packet<?>) packet);
            Haru.instance.getEventBus().post(event);
            return event.isCancelled();
        } catch (Throwable t) {
            return false;
        }
    }

    // ── NetHandlerPlayClient.processXxx  (all SRG process methods) ──────────

    @SuppressWarnings("unchecked")
    public static void onReceivePacket(Object handler, Object packet) {
        if (skipReceiveEvent.get()) return;
        if (!ready()) return;
        try {
            PacketEvent event = new PacketEvent(EnumPacketDirection.SERVERBOUND, (Packet<?>) packet);
            Haru.instance.getEventBus().post(event);
        } catch (Throwable ignored) {}
    }

    // ── NoEvent helpers (called from methods injected into MC classes) ────────

    /**
     * Sends a packet without firing PacketEvent.
     * Injected as NetworkManager.sendPacketNoEvent(Packet) body.
     */
    public static void sendPacketNoEventImpl(Object netManager, Object packet) {
        skipSendEvent.set(Boolean.TRUE);
        try {
            for (Method m : netManager.getClass().getMethods()) {
                if ("func_150732_b".equals(m.getName())) {
                    m.setAccessible(true);
                    m.invoke(netManager, packet, (Object) null);
                    break;
                }
            }
        } catch (Throwable ignored) {
        } finally {
            skipSendEvent.set(Boolean.FALSE);
        }
    }

    /**
     * Processes an incoming packet without firing PacketEvent.
     * Injected as NetHandlerPlayClient.receiveQueueNoEvent(Packet) body.
     */
    public static void receiveQueueNoEventImpl(Object handler, Object packet) {
        skipReceiveEvent.set(Boolean.TRUE);
        try {
            for (Method m : packet.getClass().getMethods()) {
                if ("func_148833_a".equals(m.getName())) {
                    m.setAccessible(true);
                    m.invoke(packet, handler);
                    break;
                }
            }
        } catch (Throwable ignored) {
        } finally {
            skipReceiveEvent.set(Boolean.FALSE);
        }
    }

    // ── Field access helpers (walk superclass chain) ─────────────────────────

    private static Field findField(Class<?> cls, String name) throws NoSuchFieldException {
        for (Class<?> c = cls; c != null; c = c.getSuperclass()) {
            try {
                Field f = c.getDeclaredField(name);
                f.setAccessible(true);
                return f;
            } catch (NoSuchFieldException ignored) {}
        }
        throw new NoSuchFieldException(name);
    }

    private static double  getDouble(Class<?> cls, Object o, String n) throws Exception { return (Double)  findField(cls, n).get(o); }
    private static float   getFloat (Class<?> cls, Object o, String n) throws Exception { return (Float)   findField(cls, n).get(o); }
    private static boolean getBool  (Class<?> cls, Object o, String n) throws Exception { return (Boolean) findField(cls, n).get(o); }
    private static void    setDouble(Class<?> cls, Object o, String n, double  v) throws Exception { findField(cls, n).set(o, v); }
    private static void    setFloat (Class<?> cls, Object o, String n, float   v) throws Exception { findField(cls, n).set(o, v); }
    private static void    setBool  (Class<?> cls, Object o, String n, boolean v) throws Exception { findField(cls, n).set(o, v); }
}

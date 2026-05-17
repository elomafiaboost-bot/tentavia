package cc.unknown.bridge;

import cc.unknown.Haru;
import cc.unknown.event.impl.move.LivingEvent;
import cc.unknown.event.impl.move.MotionEvent;
import cc.unknown.event.impl.move.PreUpdateEvent;
import cc.unknown.event.impl.other.MouseEvent;
import cc.unknown.event.impl.player.TickEvent;
import cc.unknown.event.impl.render.RenderEvent;
import cc.unknown.event.impl.world.WorldEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.event.entity.living.LivingUpdateEvent;
import net.minecraftforge.event.world.WorldEvent.Load;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.ClientTickEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;
import net.minecraftforge.fml.common.gameevent.TickEvent.PlayerTickEvent;

/**
 * Translates Forge events into the Haru internal event bus.
 * This replaces all the Mixin hooks from the original Forge mod.
 */
public class ForgeBridge {

    private static final Minecraft mc = Minecraft.func_71410_x();

    // ── Client Tick ───────────────────────────────────────────────────────────

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onClientTickPre(ClientTickEvent e) {
        if (mc.field_71439_g == null || mc.field_71441_e == null) return;
        if (e.phase != Phase.START) return;

        // Process module keybinds
        Haru.instance.getModuleManager().getModule().forEach(m -> {
            try { m.keybind(); } catch (Throwable ignored) {}
        });

        Haru.instance.getEventBus().post(new TickEvent.Pre());
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onClientTickPost(ClientTickEvent e) {
        if (mc.field_71439_g == null || mc.field_71441_e == null) return;
        if (e.phase != Phase.END) return;
        Haru.instance.getEventBus().post(new TickEvent.Post());
    }

    // ── Player Tick (Motion hooks) ────────────────────────────────────────────

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onPlayerTickPre(PlayerTickEvent e) {
        if (e.phase != Phase.START || mc.field_71439_g == null) return;

        Haru.instance.getEventBus().post(new PreUpdateEvent());

        MotionEvent motionPre = new MotionEvent(
                MotionEvent.MotionType.Pre,
                mc.field_71439_g.field_70159_w,
                mc.field_71439_g.field_70181_x,
                mc.field_71439_g.field_70179_y,
                mc.field_71439_g.field_70177_z,
                mc.field_71439_g.field_70125_A,
                mc.field_71439_g.field_70122_E);

        Haru.instance.getEventBus().post(motionPre);

        // Apply modified motion back
        mc.field_71439_g.field_70159_w = motionPre.getX();
        mc.field_71439_g.field_70181_x = motionPre.getY();
        mc.field_71439_g.field_70179_y = motionPre.getZ();
        mc.field_71439_g.field_70177_z = motionPre.getYaw();
        mc.field_71439_g.field_70125_A = motionPre.getPitch();
        mc.field_71439_g.field_70122_E = motionPre.isOnGround();
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onPlayerTickPost(PlayerTickEvent e) {
        if (e.phase != Phase.END || mc.field_71439_g == null) return;

        MotionEvent motionPost = new MotionEvent(
                MotionEvent.MotionType.Post,
                mc.field_71439_g.field_70159_w,
                mc.field_71439_g.field_70181_x,
                mc.field_71439_g.field_70179_y,
                mc.field_71439_g.field_70177_z,
                mc.field_71439_g.field_70125_A,
                mc.field_71439_g.field_70122_E);

        Haru.instance.getEventBus().post(motionPost);
    }

    // ── Living Update (NoSlow, Sprint via LivingEvent) ────────────────────────

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onLivingUpdate(LivingUpdateEvent e) {
        if (e.entity == mc.field_71439_g) {
            Haru.instance.getEventBus().post(new LivingEvent());
        }
    }

    // ── Render ────────────────────────────────────────────────────────────────

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onRenderWorld(RenderWorldLastEvent e) {
        Haru.instance.getEventBus().post(new RenderEvent(RenderEvent.RenderType.Render3D, e.partialTicks));
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onRenderOverlay(RenderGameOverlayEvent.Post e) {
        if (e.type == RenderGameOverlayEvent.ElementType.ALL) {
            Haru.instance.getEventBus().post(new RenderEvent(RenderEvent.RenderType.Render2D, e.partialTicks));
        }
    }

    // ── Input ─────────────────────────────────────────────────────────────────

    @SubscribeEvent
    public void onMouseInput(InputEvent.MouseInputEvent e) {
        Haru.instance.getEventBus().post(new MouseEvent());
    }

    // ── World ─────────────────────────────────────────────────────────────────

    @SubscribeEvent
    public void onWorldLoad(Load e) {
        if (e.world instanceof WorldClient) {
            Haru.instance.getEventBus().post(new WorldEvent((WorldClient) e.world));
        }
    }
}

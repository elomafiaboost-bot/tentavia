package cc.unknown.mc;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.multiplayer.PlayerControllerMP;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Session;

/**
 * Central accessor for Minecraft objects.
 *
 * Unlike Loona.mc (static final, set once at first class load), every method here
 * calls Minecraft.func_71410_x() directly so the reference is always fresh.
 * Use this class in hooks, utilities, and any code that runs before or outside
 * the normal module lifecycle.
 *
 * Modules that already use the Loona.mc field can keep doing so — that field is
 * initialized at the right time (first module created inside startClient()). This
 * class is the canonical alternative for code that needs reliable lazy access.
 */
public final class MC {

    private MC() {}

    // ── Core instance ─────────────────────────────────────────────────────────

    /** The singleton Minecraft instance. Never cache this reference. */
    public static Minecraft get() {
        return Minecraft.func_71410_x();
    }

    // ── Frequently accessed sub-objects ───────────────────────────────────────

    /** The local player. Null when not in a world. */
    public static EntityPlayerSP player() {
        Minecraft m = get();
        return m == null ? null : m.field_71439_g;
    }

    /** The client-side world. Null when not in a world. */
    public static WorldClient world() {
        Minecraft m = get();
        return m == null ? null : m.field_71441_e;
    }

    /** The player controller (block breaking, item use interactions). */
    public static PlayerControllerMP controller() {
        Minecraft m = get();
        return m == null ? null : m.field_71442_b;
    }

    /** The game settings (sensitivity, key binds, render options). */
    public static GameSettings settings() {
        Minecraft m = get();
        return m == null ? null : m.field_71474_y;
    }

    /** The network handler for the current server connection. Null if not connected. */
    public static NetHandlerPlayClient netHandler() {
        Minecraft m = get();
        return m == null ? null : m.func_147114_u();
    }

    /** The current mouse-over result (block or entity the crosshair is pointing at). */
    public static MovingObjectPosition objectOver() {
        Minecraft m = get();
        return m == null ? null : m.field_71476_x;
    }

    /** The active session (account info, access token). */
    public static Session session() {
        Minecraft m = get();
        return m == null ? null : m.func_110432_I();
    }

    // ── State checks ──────────────────────────────────────────────────────────

    /** True when the player and world are both loaded (safe to run gameplay logic). */
    public static boolean inGame() {
        EntityPlayerSP p = player();
        WorldClient    w = world();
        return p != null && w != null;
    }

    /** True when the player is alive and in a world. */
    public static boolean isAlive() {
        EntityPlayerSP p = player();
        return p != null && !p.field_70128_L;
    }

    /** True when a GUI screen is currently open (blocks most module inputs). */
    public static boolean isGuiOpen() {
        Minecraft m = get();
        return m != null && m.field_71462_r != null;
    }

    /** The currently open screen, or null. */
    public static GuiScreen currentScreen() {
        Minecraft m = get();
        return m == null ? null : m.field_71462_r;
    }

    // ── Convenience shortcuts ─────────────────────────────────────────────────

    /**
     * Shorthand: mc.field_71439_g != null && mc.field_71441_e != null.
     * Exactly mirrors PlayerUtil.inGame() but available without Loona dependency.
     */
    public static boolean hasWorld() {
        return player() != null && world() != null;
    }
}

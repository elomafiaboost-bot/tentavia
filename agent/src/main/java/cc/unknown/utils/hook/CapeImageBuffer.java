/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.entity.AbstractClientPlayer
 *  net.minecraft.client.renderer.IImageBuffer
 *  net.minecraft.client.renderer.ImageBufferDownload
 *  net.minecraft.util.ResourceLocation
 */
package cc.unknown.utils.hook;

import java.awt.image.BufferedImage;
import java.lang.ref.WeakReference;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.renderer.IImageBuffer;
import net.minecraft.client.renderer.ImageBufferDownload;
import net.minecraft.util.ResourceLocation;

public class CapeImageBuffer
implements IImageBuffer {
    public ImageBufferDownload imageBufferDownload;
    public final WeakReference<AbstractClientPlayer> playerRef;
    public final ResourceLocation resourceLocation;

    public CapeImageBuffer(AbstractClientPlayer player, ResourceLocation resourceLocation) {
        this.playerRef = new WeakReference<AbstractClientPlayer>(player);
        this.resourceLocation = resourceLocation;
        this.imageBufferDownload = new ImageBufferDownload();
    }

    public BufferedImage func_78432_a(BufferedImage image) {
        return this.func_78432_a(image);
    }

    public void skinAvailable() {}

    public void func_152634_a() {
        AbstractClientPlayer player = (AbstractClientPlayer)this.playerRef.get();
        if (player != null) {
            CapeImageBuffer.setLocationOfCape(player, this.resourceLocation);
        }
    }

    private static void setLocationOfCape(AbstractClientPlayer player, ResourceLocation resourceLocation) {
    }
}


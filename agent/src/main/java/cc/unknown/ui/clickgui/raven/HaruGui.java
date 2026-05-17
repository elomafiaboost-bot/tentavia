/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.gui.GuiScreen
 *  net.minecraft.client.gui.ScaledResolution
 *  net.minecraft.util.ResourceLocation
 */
package cc.unknown.ui.clickgui.raven;

import cc.unknown.Haru;
import cc.unknown.module.impl.api.Category;
import cc.unknown.module.impl.visuals.ClickGuiModule;
import cc.unknown.ui.clickgui.raven.impl.CategoryComp;
import cc.unknown.ui.clickgui.raven.impl.api.Theme;
import cc.unknown.utils.client.FuckUtil;
import cc.unknown.utils.client.RenderUtil;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.util.ResourceLocation;

public class HaruGui
extends GuiScreen {
    private final ArrayList<CategoryComp> categoryList = new ArrayList();
    private final Map<String, ResourceLocation> waifuMap = new HashMap<String, ResourceLocation>();
    private boolean isDragging = false;
    private AtomicInteger lastMouseX = new AtomicInteger(0);
    private AtomicInteger lastMouseY = new AtomicInteger(0);

    public HaruGui() {
        int topOffset = 5;
        for (Category category : Category.values()) {
            CategoryComp comp = new CategoryComp(category);
            comp.setY(topOffset);
            this.categoryList.add(comp);
            topOffset += 20;
        }
        String[] waifuNames = new String[]{"uzaki", "megumin", "ai", "mai", "kiwi", "astolfo"};
        Arrays.stream(waifuNames).forEach(name -> this.waifuMap.put((String)name, new ResourceLocation("haru/img/clickgui/" + name + ".png")));
    }

    public void func_73866_w_() {
        super.func_73866_w_();
    }

    public void func_73863_a(int mouseX, int mouseY, float partialTicks) {
        ScaledResolution sr = new ScaledResolution(this.field_146297_k);
        ClickGuiModule cg = (ClickGuiModule)Haru.instance.getModuleManager().getModule(ClickGuiModule.class);
        ResourceLocation waifuImage = this.waifuMap.get(cg.waifuMode.getMode().toLowerCase());
        if (cg.backGroundMode.is("Gradient")) {
            this.func_73733_a(0, 0, sr.func_78326_a(), sr.func_78328_b(), Theme.instance.getMainColor().getRGB(), Theme.instance.getMainColor().getAlpha());
        } else if (cg.backGroundMode.is("Normal")) {
            this.func_73733_a(0, 0, this.field_146294_l, this.field_146295_m, -1072689136, -804253680);
        }
        this.categoryList.forEach(c -> {
            c.render(this.field_146289_q);
            c.updatePosition(mouseX, mouseY);
            c.getModules().forEach(comp -> comp.updateComponent(mouseX, mouseY));
        });
        if (waifuImage != null) {
            RenderUtil.drawImage(waifuImage, FuckUtil.instance.getWaifuX(), FuckUtil.instance.getWaifuY(), (float)sr.func_78326_a() / 5.2f, (float)sr.func_78328_b() / 2.0f);
        }
        if (this.isDragging) {
            FuckUtil.instance.setWaifuX(FuckUtil.instance.getWaifuX() + mouseX - this.lastMouseX.get());
            FuckUtil.instance.setWaifuY(FuckUtil.instance.getWaifuY() + mouseY - this.lastMouseY.get());
            this.lastMouseX.set(mouseX);
            this.lastMouseY.set(mouseY);
        }
    }

    public void func_73864_a(int mouseX, int mouseY, int mouseButton) {
        ScaledResolution sr = new ScaledResolution(this.field_146297_k);
        if (mouseButton == 0 && this.isBound(mouseX, mouseY, sr)) {
            this.isDragging = true;
            this.lastMouseX.set(mouseX);
            this.lastMouseY.set(mouseY);
            return;
        }
        this.categoryList.forEach(c -> {
            if (c.isInside(mouseX, mouseY)) {
                switch (mouseButton) {
                    case 0: {
                        c.setDragging(true);
                        c.setDragX(mouseX - c.getX());
                        c.setDragY(mouseY - c.getY());
                        break;
                    }
                    case 1: {
                        c.setOpen(!c.isOpen());
                    }
                }
            }
            if (c.isOpen() && !c.getModules().isEmpty()) {
                c.getModules().forEach(component -> component.mouseClicked(mouseX, mouseY, mouseButton));
            }
        });
    }

    public void func_146286_b(int mouseX, int mouseY, int state) {
        ScaledResolution sr = new ScaledResolution(this.field_146297_k);
        if (state == 0 && this.isBound(mouseX, mouseY, sr)) {
            this.isDragging = false;
            return;
        }
        this.categoryList.forEach(c -> {
            c.setDragging(false);
            if (c.isOpen() && !c.getModules().isEmpty()) {
                c.getModules().forEach(component -> component.mouseReleased(mouseX, mouseY, state));
            }
        });
        if (Haru.instance.getClientConfig() != null) {
            Haru.instance.getClientConfig().saveConfig();
        }
    }

    public void func_73869_a(char t, int k) {
        this.categoryList.forEach(c -> {
            if (c.isOpen() && k != 1 && !c.getModules().isEmpty()) {
                c.getModules().forEach(component -> component.keyTyped(t, k));
            }
        });
        if (k == 1 || k == 54) {
            this.field_146297_k.func_147108_a(null);
        }
    }

    public void func_146281_b() {
        ClickGuiModule cg = (ClickGuiModule)Haru.instance.getModuleManager().getModule(ClickGuiModule.class);
        if (cg != null && cg.isEnabled() && Haru.instance.getClientConfig() != null) {
            Haru.instance.getClientConfig().saveConfig();
            cg.disable();
        }
    }

    public boolean func_73868_f() {
        return false;
    }

    public ArrayList<CategoryComp> getCategoryList() {
        return this.categoryList;
    }

    private boolean isBound(int x, int y, ScaledResolution sr) {
        return x >= FuckUtil.instance.getWaifuX() && (float)x <= (float)FuckUtil.instance.getWaifuX() + (float)sr.func_78326_a() / 5.1f && y >= FuckUtil.instance.getWaifuY() && (float)y <= (float)FuckUtil.instance.getWaifuY() + (float)sr.func_78328_b() / 2.0f;
    }
}


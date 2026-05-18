/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.gui.inventory.GuiChest
 *  net.minecraft.entity.player.EntityPlayer
 *  net.minecraft.init.Blocks
 *  net.minecraft.init.Items
 *  net.minecraft.inventory.ContainerChest
 *  net.minecraft.inventory.ContainerPlayer
 *  net.minecraft.item.Item
 *  net.minecraft.item.ItemArmor
 *  net.minecraft.item.ItemAxe
 *  net.minecraft.item.ItemBlock
 *  net.minecraft.item.ItemPickaxe
 *  net.minecraft.item.ItemStack
 *  net.minecraft.item.ItemSword
 */
package cc.unknown.module.impl.player;

import cc.unknown.event.impl.EventLink;
import cc.unknown.event.impl.move.MotionEvent;
import cc.unknown.event.impl.other.ClickGuiEvent;
import cc.unknown.module.impl.Module;
import cc.unknown.module.impl.api.Category;
import cc.unknown.module.impl.api.Register;
import cc.unknown.module.setting.impl.BooleanValue;
import cc.unknown.module.setting.impl.DoubleSliderValue;
import cc.unknown.utils.client.Cold;
import cc.unknown.utils.player.PlayerUtil;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import net.minecraft.client.gui.inventory.GuiChest;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.inventory.ContainerChest;
import net.minecraft.inventory.ContainerPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemAxe;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemPickaxe;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;

@Register(name="Stealer", category=Category.Player)
public class Stealer
extends Module {
    private final DoubleSliderValue openDelay = new DoubleSliderValue("Open Delay", 125.0, 150.0, 25.0, 1000.0, 25.0);
    private final DoubleSliderValue stealDelay = new DoubleSliderValue("Steal Delay", 125.0, 150.0, 25.0, 1000.0, 25.0);
    private final BooleanValue autoClose = new BooleanValue("Auto Close", true);
    private final DoubleSliderValue closeDelay = new DoubleSliderValue("Close Delay", 0.0, 0.0, 0.0, 1000.0, 1.0);
    private final AtomicReference<ArrayList<Slot>> sortedSlots = new AtomicReference();
    private final AtomicReference<ContainerChest> chest = new AtomicReference();
    private final AtomicBoolean inChest = new AtomicBoolean(false);
    private final Cold delayTimer = new Cold(0L);
    private final Cold closeTimer = new Cold(0L);
    private final List<Item> whiteListedItems = Arrays.asList(Items.field_151117_aB, Items.field_151153_ao, Items.field_151068_bn, Items.field_151079_bi, Items.field_151131_as, Items.field_151032_g, Items.field_151031_f);

    public Stealer() {
        this.registerSetting(this.openDelay, this.stealDelay, this.autoClose, this.closeDelay);
    }

    @EventLink
    public void onGui(ClickGuiEvent e) {
        this.setSuffix("- [" + this.openDelay.getInputMinToInt() + ", " + this.openDelay.getInputMaxToInt() + " ms]");
    }

    @EventLink
    public void onPre(MotionEvent e) {
        if (e.isPre()) {
            if (Stealer.mc.field_71462_r != null && Stealer.mc.field_71439_g.field_71069_bz != null && Stealer.mc.field_71439_g.field_71069_bz instanceof ContainerPlayer && Stealer.mc.field_71462_r instanceof GuiChest) {
                if (!this.inChest.get()) {
                    this.chest.set((ContainerChest)Stealer.mc.field_71439_g.field_71070_bA);
                    this.delayTimer.setCooldown((long)ThreadLocalRandom.current().nextDouble(this.openDelay.getInputMin(), this.openDelay.getInputMax() + 0.01));
                    this.delayTimer.start();
                    this.generatePath(this.chest.get());
                    this.inChest.set(true);
                }
                if (this.inChest.get() && this.sortedSlots.get() != null && !this.sortedSlots.get().isEmpty() && this.delayTimer.hasFinished()) {
                    this.clickSlot(this.sortedSlots.get().get((int)0).s);
                    this.delayTimer.setCooldown((long)ThreadLocalRandom.current().nextDouble(this.stealDelay.getInputMin(), this.stealDelay.getInputMax() + 0.01));
                    this.delayTimer.start();
                    this.sortedSlots.get().remove(0);
                }
                if (this.sortedSlots.get() != null && this.sortedSlots.get().isEmpty() && this.autoClose.isToggled()) {
                    if (this.closeTimer.firstFinish()) {
                        Stealer.mc.field_71439_g.func_71053_j();
                        this.inChest.set(false);
                    } else {
                        this.closeTimer.setCooldown((long)ThreadLocalRandom.current().nextDouble(this.closeDelay.getInputMin(), this.closeDelay.getInputMax() + 0.01));
                        this.closeTimer.start();
                    }
                }
            } else {
                this.inChest.set(false);
            }
        }
    }

    private void generatePath(ContainerChest chest) {
        ArrayList slots = IntStream.range(0, chest.func_85151_d().func_70302_i_()).mapToObj(i -> {
            Predicate<ItemStack> stealCondition;
            ItemStack itemStack = (ItemStack)chest.func_75138_a().get(i);
            if (itemStack != null && (stealCondition = stack -> {
                Item item = stack.func_77973_b();
                return item instanceof ItemSword && (PlayerUtil.getBestSword() == null || PlayerUtil.isBetterSword(stack, PlayerUtil.getBestSword())) || item instanceof ItemAxe && (PlayerUtil.getBestAxe() == null || PlayerUtil.isBetterTool(stack, PlayerUtil.getBestAxe(), Blocks.field_150344_f)) || item instanceof ItemPickaxe && (PlayerUtil.getBestAxe() == null || PlayerUtil.isBetterTool(stack, PlayerUtil.getBestAxe(), Blocks.field_150348_b)) || item instanceof ItemBlock || item instanceof ItemArmor || this.whiteListedItems.contains(item);
            }).test(itemStack)) {
                return new Slot(i);
            }
            return null;
        }).filter(Objects::nonNull).collect(Collectors.toCollection(ArrayList::new));
        @SuppressWarnings("unchecked")
        Slot[] sorted = this.sort((Slot[])slots.toArray(new Slot[0]));
        this.sortedSlots.set(new ArrayList<Slot>(Arrays.asList(sorted)));
    }

    private Slot[] sort(Slot[] in) {
        if (in == null || in.length == 0) {
            return in;
        }
        Slot[] out = new Slot[in.length];
        Slot current = in[ThreadLocalRandom.current().nextInt(0, in.length)];
        for (int i = 0; i < in.length; ++i) {
            Slot next;
            Slot currentSlot = current;
            if (i == in.length - 1) {
                out[in.length - 1] = Arrays.stream(in).filter(p -> !p.visited).findAny().orElse(null);
                break;
            }
            out[i] = current;
            current.visit();
            current = next = (Slot)Arrays.stream(in).filter(p -> !p.visited).min(Comparator.comparingDouble(p -> p.getDistance(currentSlot))).orElse(null);
        }
        return out;
    }

    private void clickSlot(int x) {
        Stealer.mc.field_71442_b.func_78753_a(Stealer.mc.field_71439_g.field_71070_bA.field_75152_c, x, 0, 1, (EntityPlayer)Stealer.mc.field_71439_g);
    }

    class Slot {
        final int x;
        final int y;
        final int s;
        boolean visited;

        Slot(int s) {
            this.x = (s + 1) % 10;
            this.y = s / 9;
            this.s = s;
        }

        public double getDistance(Slot s) {
            return Math.abs(this.x - s.x) + Math.abs(this.y - s.y);
        }

        public void visit() {
            this.visited = true;
        }
    }
}


package de.project.ae2aquarium.client.gui;

import appeng.api.storage.cells.CellState;
import de.project.ae2aquarium.block.AquariumControllerBlockEntity;
import de.project.ae2aquarium.config.AquariumConfig;
import de.project.ae2aquarium.menu.AquariumControllerMenu;
import de.project.ae2aquarium.multiblock.AquariumBounds;
import de.project.ae2aquarium.network.SetAquariumPartitionsPayload;
import de.project.ae2aquarium.partition.AquariumPartition;
import de.project.ae2aquarium.partition.AquariumPartitionList;
import de.project.ae2aquarium.recipe.AquariumDropEntry;
import de.project.ae2aquarium.recipe.AquariumDropRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class AquariumControllerScreen extends AbstractContainerScreen<AquariumControllerMenu> {

    private static final int[] PALETTE = {
            0xFF00B4D8, // Reef Cyan
            0xFFFF5D8F, // Brain Coral Pink
            0xFF9D4EDD, // Bubble Coral Purple
            0xFFE63946, // Fire Coral Red
            0xFFFFBE0B, // Horn Coral Gold
            0xFF2BA84A, // Kelp Green
            0xFF3A86FF, // Tube Coral Blue
            0xFFFF9F1C  // Sponge Amber
    };

    public static class PartitionDraft {
        public Item target;
        public int percent;
        public boolean voidSecondary;

        public PartitionDraft(Item target, int percent, boolean voidSecondary) {
            this.target = target;
            this.percent = percent;
            this.voidSecondary = voidSecondary;
        }
    }

    private final List<PartitionDraft> workingList = new ArrayList<>();
    private AquariumPartitionList lastSyncedPartitions = null;
    private boolean dirty = false;
    private int selectedRowForPicker = -1;
    private int scrollOffset = 0;

    public AquariumControllerScreen(AquariumControllerMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 280;
        this.imageHeight = 262;
    }

    @Override
    protected void init() {
        super.init();
        syncFromControllerIfClean(true);
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        syncFromControllerIfClean(false);
    }

    private void syncFromControllerIfClean(boolean force) {
        AquariumPartitionList current = menu.getPartitions();
        if (force || (!dirty && !Objects.equals(current, lastSyncedPartitions))) {
            lastSyncedPartitions = current;
            workingList.clear();
            for (AquariumPartition p : current.partitions()) {
                workingList.add(new PartitionDraft(p.target(), p.percent(), p.voidSecondary()));
            }
            dirty = false;
        }
    }

    private int getTotalPercent() {
        int sum = 0;
        for (PartitionDraft d : workingList) {
            sum += d.percent;
        }
        return sum;
    }

    private int getUnallocatedPercent() {
        return Math.max(0, 100 - getTotalPercent());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
        renderCustomTooltips(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(this.font, this.title, 10, 6, 0xFF70F8FF, true);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        renderCustomBackground(graphics, mouseX, mouseY);
    }

    private void renderCustomBackground(GuiGraphics graphics, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;

        // Outer AE2 Metallic Slate Window Frame
        graphics.fill(x, y, x + imageWidth, y + imageHeight, 0xFF2D3742);
        // Top & Left 3D Bevel Highlight
        graphics.fill(x, y, x + imageWidth, y + 2, 0xFF7389A1);
        graphics.fill(x, y + 2, x + 2, y + imageHeight, 0xFF7389A1);
        // Bottom & Right 3D Bevel Shadow
        graphics.fill(x, y + imageHeight - 2, x + imageWidth, y + imageHeight, 0xFF14191F);
        graphics.fill(x + imageWidth - 2, y, x + imageWidth, y + imageHeight, 0xFF14191F);
        // Inner Metallic Panel Surface
        graphics.fill(x + 2, y + 2, x + imageWidth - 2, y + imageHeight - 2, 0xFF28313B);

        // Header Separator with subtle highlight
        graphics.fill(x + 8, y + 16, x + imageWidth - 8, y + 17, 0xFF192028);
        graphics.fill(x + 8, y + 17, x + imageWidth - 8, y + 18, 0xFF475768);

        // Status Box (x=12, y=19, w=196, h=20)
        drawInsetPanel(graphics, x + 12, y + 19, 196, 20, 0xFF1B232B);
        AquariumBounds bounds = menu.getFormedBounds();
        boolean formed = menu.isFormed() && bounds != null;
        boolean online = menu.isNetworkOnline();
        boolean hasCell = menu.hasStorageCell();
        AquariumControllerBlockEntity be = menu.getBlockEntity();

        if (formed) {
            String sizeStr = String.format("Tank: %dx%dx%d (%d m³)", bounds.width(), bounds.height(), bounds.depth(), bounds.innerVolume());
            graphics.drawString(this.font, sizeStr, x + 16, y + 21, 0xFF66FF77, true);

            String netBadge = online ? "ME: Online" : "ME: Offline";
            int netColor = online ? 0xFF33F0FF : 0xFFFFB733;
            graphics.drawString(this.font, netBadge, x + 204 - this.font.width(netBadge), y + 21, netColor, true);

            if (hasCell) {
                int drops = be != null ? be.getDropsPerCycle() : bounds.innerVolume();
                int intervalTicks = be != null ? be.getTargetInterval() : 60;
                String rateStr = String.format("Yield: %d Drops / %.1fs | Alloc: %d%%", drops, intervalTicks / 20.0f, getTotalPercent());
                graphics.drawString(this.font, rateStr, x + 16, y + 30, 0xFFD0DCE8, false);
            } else {
                graphics.drawString(this.font, "Paused: Insert ME Storage Cell ->", x + 16, y + 30, 0xFFFFCC44, false);
            }
        } else {
            graphics.drawString(this.font, "Structure Incomplete", x + 16, y + 21, 0xFFFF6666, true);
            graphics.drawString(this.font, "Build hollow 3x3x3+ Casing & Glass tank", x + 16, y + 30, 0xFFB0C0D0, false);
        }

        // Visual Allocation Bar (x=12, y=42, w=196, h=14)
        int barX = x + 12;
        int barY = y + 42;
        int barW = 196;
        int barH = 14;
        graphics.fill(barX - 1, barY - 1, barX + barW + 1, barY + barH + 1, 0xFF12181E);

        int currentX = barX;
        for (int i = 0; i < workingList.size(); i++) {
            PartitionDraft p = workingList.get(i);
            int sliceW = (int) Math.round((p.percent / 100.0) * barW);
            if (sliceW > 0) {
                int color = PALETTE[i % PALETTE.length];
                int endX = Math.min(barX + barW, currentX + sliceW);
                graphics.fill(currentX, barY, endX, barY + barH, color);
                graphics.fill(currentX, barY, endX, barY + 2, 0x44FFFFFF);
                graphics.fill(endX - 1, barY, endX, barY + barH, 0xFF141A20);
                currentX += sliceW;
            }
        }
        if (currentX < barX + barW) {
            graphics.fill(currentX, barY, barX + barW, barY + barH, 0xFF323E4B);
            if (barX + barW - currentX > 34) {
                String freeTxt = getUnallocatedPercent() + "% Free";
                graphics.drawString(this.font, freeTxt, currentX + (barX + barW - currentX - this.font.width(freeTxt)) / 2, barY + 3, 0xFFD0DCE8, true);
            }
        }

        // Partition Table Area (x=12, y=58, w=196, h=76)
        int tableX = x + 12;
        int tableY = y + 58;
        int tableW = 196;
        int tableH = 76;
        drawInsetPanel(graphics, tableX, tableY, tableW, tableH, 0xFF1D252E);
        graphics.fill(tableX + 1, tableY + 1, tableX + tableW - 1, tableY + 13, 0xFF25303B);

        graphics.drawString(this.font, "Specimen", tableX + 22, tableY + 3, 0xFFB8CADE, false);
        graphics.drawString(this.font, "Share", tableX + 102, tableY + 3, 0xFFB8CADE, false);
        graphics.drawString(this.font, "2nd", tableX + 144, tableY + 3, 0xFFB8CADE, false);

        int maxVisible = 3;
        if (workingList.isEmpty()) {
            String emptyMsg = "No specimens. Click '+ Add' below.";
            graphics.drawString(this.font, emptyMsg, tableX + (tableW - this.font.width(emptyMsg)) / 2, tableY + 38, 0xFF94A8BC, false);
        } else {
            for (int i = 0; i < maxVisible; i++) {
                int index = scrollOffset + i;
                if (index >= workingList.size()) break;

                PartitionDraft p = workingList.get(index);
                int rowY = tableY + 14 + i * 20;

                int rowBg = (index % 2 == 1) ? 0xFF232D38 : 0xFF1F2832;
                graphics.fill(tableX + 2, rowY - 1, tableX + tableW - 2, rowY + 18, rowBg);

                int color = PALETTE[index % PALETTE.length];
                graphics.fill(tableX + 4, rowY + 1, tableX + 7, rowY + 17, color);

                int boxX = tableX + 10;
                int boxY = rowY;
                int boxColor = (selectedRowForPicker == index) ? 0xFFFFEA00 : 0xFF566A7F;
                graphics.fill(boxX - 1, boxY - 1, boxX + 17, boxY + 17, boxColor);
                graphics.fill(boxX, boxY, boxX + 16, boxY + 16, 0xFF2A3542);

                if (p.target != null && p.target != Items.AIR) {
                    ItemStack renderStack = new ItemStack(p.target);
                    graphics.renderItem(renderStack, boxX, boxY);
                    String name = renderStack.getHoverName().getString();
                    if (font.width(name) > 52) {
                        name = font.plainSubstrByWidth(name, 48) + "..";
                    }
                    graphics.drawString(this.font, name, tableX + 30, rowY + 4, 0xFFF2F6FA, true);
                } else {
                    graphics.drawString(this.font, "Select...", tableX + 30, rowY + 4, 0xFF9EB0C2, false);
                }

                drawButton(graphics, tableX + 88, rowY + 3, 11, 11, "-", 0xFFFFFFFF, mouseX, mouseY);
                String pctStr = p.percent + "%";
                graphics.drawString(this.font, pctStr, tableX + 111 - this.font.width(pctStr) / 2, rowY + 4, 0xFFFFFFFF, true);
                drawButton(graphics, tableX + 122, rowY + 3, 11, 11, "+", 0xFFFFFFFF, mouseX, mouseY);

                boolean canVoid = menu.hasVoidSecondaryCard();
                int voidBg = canVoid ? (p.voidSecondary ? 0xFF6B21A8 : 0xFF354250) : 0xFF26303A;
                int voidText = canVoid ? (p.voidSecondary ? 0xFFFFFFFF : 0xFFB8C7D6) : 0xFF677788;
                drawButtonWithCustomBg(graphics, tableX + 138, rowY + 3, 26, 11, "Void", voidText, voidBg, mouseX, mouseY);

                drawButton(graphics, tableX + 170, rowY + 3, 11, 11, "x", 0xFFFF6B6B, mouseX, mouseY);
            }
        }

        if (workingList.size() > maxVisible) {
            drawButton(graphics, tableX + tableW - 11, tableY + 14, 9, 9, "▲", scrollOffset > 0 ? 0xFFFFFFFF : 0xFF6C7D8E, mouseX, mouseY);
            drawButton(graphics, tableX + tableW - 11, tableY + tableH - 12, 9, 9, "▼", (scrollOffset + maxVisible < workingList.size()) ? 0xFFFFFFFF : 0xFF6C7D8E, mouseX, mouseY);
        }

        // Action Buttons Row (y=137)
        int btnY = y + 137;
        int addColor = (workingList.size() < 8 && getUnallocatedPercent() > 0) ? 0xFFFFFFFF : 0xFF7C8D9E;
        drawButton(graphics, x + 12, btnY, 36, 14, "+ Add", addColor, mouseX, mouseY);

        int eqColor = !workingList.isEmpty() ? 0xFFFFFFFF : 0xFF7C8D9E;
        drawButton(graphics, x + 51, btnY, 44, 14, "Equalize", eqColor, mouseX, mouseY);

        int clearColor = !workingList.isEmpty() ? 0xFFFF8888 : 0xFF7C8D9E;
        drawButton(graphics, x + 98, btnY, 36, 14, "Clear", clearColor, mouseX, mouseY);

        int applyBg = dirty ? 0xFF1F6E4A : 0xFF394654;
        int applyText = dirty ? 0xFF77FF99 : 0xFFE8F0F8;
        drawButtonWithCustomBg(graphics, x + 138, btnY, 70, 14, dirty ? "Apply *" : "Apply", applyText, applyBg, mouseX, mouseY);

        // Upgrade Slots (y=154)
        for (int i = 0; i < 4; i++) {
            drawSlotBox(graphics, x + 11 + i * 18, y + 154);
        }
        drawVoidSlotBox(graphics, x + 87, y + 154);
        graphics.drawString(this.font, "Upgrades", x + 110, y + 159, 0xFFC0D2E4, false);

        // Right Panel: Dedicated ME Storage Cell Bay & Specimen Picker (x=212, y=19, w=60, h=153)
        drawInsetPanel(graphics, x + 212, y + 19, 60, 153, 0xFF1D252E);
        graphics.fill(x + 213, y + 20, x + 271, y + 32, 0xFF25303B);
        graphics.drawString(this.font, "ME Cell", x + 223, y + 22, 0xFF70F8FF, false);

        // Storage Cell Slot Box at (x=233, y=35) + Status LED
        drawCellSlotBox(graphics, x + 233, y + 35, be != null ? be.getCellState() : CellState.ABSENT);

        if (hasCell && be != null && be.getCellTotalBytes() > 0) {
            long usedB = be.getCellUsedBytes();
            long totalB = be.getCellTotalBytes();
            int usedT = be.getCellUsedTypes();
            int totalT = be.getCellTotalTypes();

            // Capacity bar (x=217, y=58, w=50, h=6)
            int cbX = x + 217;
            int cbY = y + 58;
            int cbW = 50;
            int cbH = 6;
            graphics.fill(cbX - 1, cbY - 1, cbX + cbW + 1, cbY + cbH + 1, 0xFF10161C);
            graphics.fill(cbX, cbY, cbX + cbW, cbY + cbH, 0xFF2B3642);
            int fillW = (int) Math.min(cbW, Math.round((usedB / (double) totalB) * cbW));
            if (fillW > 0) {
                int barColor = (usedB >= totalB) ? 0xFFFF4444 : (usedB > totalB * 0.8 ? 0xFFFFAA00 : 0xFF00E5FF);
                graphics.fill(cbX, cbY, cbX + fillW, cbY + cbH, barColor);
            }

            String pctUsed = Math.min(100, Math.round((usedB * 100.0f) / totalB)) + "% Full";
            graphics.drawString(this.font, pctUsed, x + 242 - this.font.width(pctUsed) / 2, y + 68, 0xFFE6F2FF, false);

            String bytesTxt = formatCompactBytes(usedB) + "/" + formatCompactBytes(totalB);
            graphics.drawString(this.font, bytesTxt, x + 242 - this.font.width(bytesTxt) / 2, y + 79, 0xFFAEC2D6, false);

            String typesTxt = usedT + "/" + totalT + " Types";
            graphics.drawString(this.font, typesTxt, x + 242 - this.font.width(typesTxt) / 2, y + 90, 0xFF8EA4BA, false);
        } else {
            graphics.drawString(this.font, "No Cell", x + 224, y + 62, 0xFFFF6666, false);
            graphics.drawString(this.font, "Insert AE2", x + 216, y + 76, 0xFF8EA4BA, false);
            graphics.drawString(this.font, "Item Cell", x + 219, y + 86, 0xFF8EA4BA, false);
        }

        // Divider on Right Panel
        graphics.fill(x + 216, y + 114, x + 268, y + 115, 0xFF364554);

        // Quick-Cycle Specimen Button on Right Panel (y=124)
        boolean rowSelected = selectedRowForPicker >= 0 && selectedRowForPicker < workingList.size();
        drawButtonWithCustomBg(
                graphics,
                x + 216,
                y + 124,
                52,
                16,
                "Cycle >>",
                rowSelected ? 0xFF66FFFF : 0xFF8899AA,
                rowSelected ? 0xFF1E526B : 0xFF2D3844,
                mouseX,
                mouseY
        );
        graphics.drawString(this.font, rowSelected ? "Pick coral" : "Click icon", x + 217, y + 144, 0xFFA5B8CC, false);

        // Player Inventory (x=30, y=181)
        int invStartX = x + 30;
        int invStartY = y + 181;
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                drawSlotBox(graphics, invStartX + col * 18 - 1, invStartY + row * 18 - 1);
            }
        }

        // Hotbar (y=239)
        int hotbarStartY = y + 239;
        for (int col = 0; col < 9; ++col) {
            drawSlotBox(graphics, invStartX + col * 18 - 1, hotbarStartY - 1);
        }
    }

    private static String formatCompactBytes(long bytes) {
        if (bytes >= 1024) {
            return String.format("%.1fk", bytes / 1024.0);
        }
        return String.valueOf(bytes);
    }

    private void drawInsetPanel(GuiGraphics graphics, int px, int py, int pw, int ph, int bgColor) {
        graphics.fill(px, py, px + pw, py + ph, bgColor);
        graphics.fill(px, py, px + pw, py + 1, 0xFF12171D);
        graphics.fill(px, py, px + 1, py + ph, 0xFF12171D);
        graphics.fill(px, py + ph - 1, px + pw, py + ph, 0xFF475768);
        graphics.fill(px + pw - 1, py, px + pw, py + ph, 0xFF475768);
    }

    private void drawSlotBox(GuiGraphics graphics, int sx, int sy) {
        graphics.fill(sx, sy, sx + 18, sy + 18, 0xFF171D24);
        graphics.fill(sx + 1, sy + 1, sx + 18, sy + 18, 0xFF63768A);
        graphics.fill(sx + 1, sy + 1, sx + 17, sy + 17, 0xFF2B3540);
    }

    private void drawCellSlotBox(GuiGraphics graphics, int sx, int sy, CellState cellState) {
        // Highlighted AE2 Drive Cell slot frame
        graphics.fill(sx - 1, sy - 1, sx + 19, sy + 19, 0xFF0096C7);
        graphics.fill(sx, sy, sx + 18, sy + 18, 0xFF141C24);
        graphics.fill(sx + 1, sy + 1, sx + 17, sy + 17, 0xFF263340);

        // Drive LED indicator to the left of the slot
        int ledColor = switch (cellState) {
            case EMPTY -> 0xFF00FF66;
            case NOT_EMPTY -> 0xFF00E5FF;
            case TYPES_FULL -> 0xFFFFAA00;
            case FULL -> 0xFFFF3333;
            default -> 0xFF222A32;
        };
        graphics.fill(sx - 6, sy + 6, sx - 2, sy + 12, 0xFF0E1318);
        graphics.fill(sx - 5, sy + 7, sx - 3, sy + 11, ledColor);
    }

    private void drawVoidSlotBox(GuiGraphics graphics, int sx, int sy) {
        graphics.fill(sx, sy, sx + 18, sy + 18, 0xFF3B0F66);
        graphics.fill(sx + 1, sy + 1, sx + 18, sy + 18, 0xFF9D4EDD);
        graphics.fill(sx + 1, sy + 1, sx + 17, sy + 17, 0xFF28173B);
    }

    private void drawButton(GuiGraphics graphics, int bx, int by, int bw, int bh, String text, int textColor, int mouseX, int mouseY) {
        drawButtonWithCustomBg(graphics, bx, by, bw, bh, text, textColor, 0xFF3B4958, mouseX, mouseY);
    }

    private void drawButtonWithCustomBg(GuiGraphics graphics, int bx, int by, int bw, int bh, String text, int textColor, int bgColor, int mouseX, int mouseY) {
        boolean hovered = mouseX >= bx && mouseX < bx + bw && mouseY >= by && mouseY < by + bh;
        int fill = hovered ? brightenColor(bgColor, 22) : bgColor;
        graphics.fill(bx, by, bx + bw, by + bh, fill);
        graphics.fill(bx, by, bx + bw, by + 1, hovered ? 0xFF8AA2BA : 0xFF64788E);
        graphics.fill(bx, by, bx + 1, by + bh, hovered ? 0xFF8AA2BA : 0xFF64788E);
        graphics.fill(bx, by + bh - 1, bx + bw, by + bh, 0xFF161C22);
        graphics.fill(bx + bw - 1, by, bx + bw, by + bh, 0xFF161C22);
        graphics.drawString(this.font, text, bx + (bw - this.font.width(text)) / 2, by + (bh - 8) / 2, textColor, true);
    }

    private static int brightenColor(int argb, int delta) {
        int a = (argb >>> 24) & 0xFF;
        int r = Math.min(255, ((argb >>> 16) & 0xFF) + delta);
        int g = Math.min(255, ((argb >>> 8) & 0xFF) + delta);
        int b = Math.min(255, (argb & 0xFF) + delta);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private void renderCustomTooltips(GuiGraphics graphics, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;
        AquariumControllerBlockEntity be = menu.getBlockEntity();
        long totalCellBytes = be != null ? be.getCellTotalBytes() : 0;

        // Allocation bar tooltips
        int barX = x + 12;
        int barY = y + 42;
        int barW = 196;
        int barH = 14;
        if (mouseX >= barX && mouseX <= barX + barW && mouseY >= barY && mouseY <= barY + barH) {
            int relX = mouseX - barX;
            int currentX = 0;
            for (PartitionDraft p : workingList) {
                int sliceW = (int) Math.round((p.percent / 100.0) * barW);
                if (relX >= currentX && relX < currentX + sliceW && p.target != null) {
                    ItemStack stack = new ItemStack(p.target);
                    List<Component> tooltip = new ArrayList<>();
                    tooltip.add(stack.getHoverName().copy().withStyle(ChatFormatting.AQUA));
                    if (totalCellBytes > 0) {
                        long allocBytes = (totalCellBytes * p.percent) / 100L;
                        tooltip.add(Component.literal("Habitat Share: " + p.percent + "% (" + String.format("%,d", allocBytes) + " Bytes)").withStyle(ChatFormatting.GRAY));
                    } else {
                        tooltip.add(Component.literal("Habitat Share: " + p.percent + "%").withStyle(ChatFormatting.GRAY));
                    }
                    tooltip.add(Component.literal(p.voidSecondary ? "Void Secondary Drops: ON" : "Void Secondary Drops: OFF")
                            .withStyle(p.voidSecondary ? ChatFormatting.LIGHT_PURPLE : ChatFormatting.DARK_GRAY));
                    graphics.renderComponentTooltip(font, tooltip, mouseX, mouseY);
                    break;
                }
                currentX += sliceW;
            }
        }

        // Table row icon tooltips with drop preview
        int tableX = x + 12;
        int tableY = y + 58;
        int maxVisible = 3;
        for (int i = 0; i < maxVisible; i++) {
            int index = scrollOffset + i;
            if (index >= workingList.size()) break;

            PartitionDraft p = workingList.get(index);
            int rowY = tableY + 14 + i * 20;

            if (mouseX >= tableX + 10 && mouseX <= tableX + 27 && mouseY >= rowY && mouseY <= rowY + 17) {
                List<Component> tooltip = new ArrayList<>();
                if (p.target != null && p.target != Items.AIR) {
                    ItemStack stack = new ItemStack(p.target);
                    tooltip.add(stack.getHoverName().copy().withStyle(ChatFormatting.AQUA));
                    List<AquariumDropEntry> drops = AquariumDropRegistry.getDropEntries(
                            p.target,
                            minecraft != null ? minecraft.level : null,
                            Integer.MAX_VALUE
                    );
                    for (int d = 0; d < drops.size(); d++) {
                        AquariumDropEntry entry = drops.get(d);
                        String prefix = (d == 0) ? "Primary: " : "Secondary: ";
                        ChatFormatting fmt = (d == 0) ? ChatFormatting.GREEN : ChatFormatting.GRAY;
                        tooltip.add(Component.literal(prefix + entry.item().getHoverName().getString() + " (" + entry.weight() + "%)").withStyle(fmt));
                    }
                }
                tooltip.add(Component.literal("Click to select from inventory or 'Cycle >>'").withStyle(ChatFormatting.YELLOW));
                graphics.renderComponentTooltip(font, tooltip, mouseX, mouseY);
            }
        }

        // Empty upgrade slot tooltips
        for (int i = 0; i < 4; i++) {
            int slotX = x + 11 + i * 18;
            int slotY = y + 154;
            if (mouseX >= slotX && mouseX <= slotX + 18 && mouseY >= slotY && mouseY <= slotY + 18) {
                if (!menu.getSlot(i).hasItem()) {
                    graphics.renderTooltip(font, Component.literal("AE2 Acceleration Card (" + (i + 1) + "/4)").withStyle(ChatFormatting.GRAY), mouseX, mouseY);
                }
            }
        }
        if (mouseX >= x + 87 && mouseX <= x + 105 && mouseY >= y + 154 && mouseY <= y + 172) {
            if (!menu.getSlot(4).hasItem()) {
                graphics.renderTooltip(font, Component.literal("Void Secondary Card / AE2 Void Card").withStyle(ChatFormatting.LIGHT_PURPLE), mouseX, mouseY);
            }
        }

        // Empty Storage Cell slot tooltip
        if (mouseX >= x + 233 && mouseX <= x + 251 && mouseY >= y + 35 && mouseY <= y + 53) {
            if (!menu.getSlot(5).hasItem()) {
                List<Component> cellTip = List.of(
                        Component.literal("ME Storage Cell Slot").withStyle(ChatFormatting.AQUA),
                        Component.literal("Insert an AE2 Item Storage Cell (1k - 256k).").withStyle(ChatFormatting.GRAY),
                        Component.literal("All aquarium drops are stored strictly inside this cell").withStyle(ChatFormatting.GREEN),
                        Component.literal("and exposed to the ME Network without overflowing drives.").withStyle(ChatFormatting.GREEN)
                );
                graphics.renderComponentTooltip(font, cellTip, mouseX, mouseY);
            }
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (workingList.size() > 3) {
            int tableX = this.leftPos + 12;
            int tableY = this.topPos + 58;
            if (mouseX >= tableX && mouseX <= tableX + 196 && mouseY >= tableY && mouseY <= tableY + 76) {
                if (scrollY > 0 && scrollOffset > 0) {
                    scrollOffset--;
                    return true;
                } else if (scrollY < 0 && scrollOffset + 3 < workingList.size()) {
                    scrollOffset++;
                    return true;
                }
            }
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int x = this.leftPos;
        int y = this.topPos;

        // Cycle >> Button on Right Panel (x + 216, y + 124, w=52, h=16)
        if (mouseX >= x + 216 && mouseX <= x + 268 && mouseY >= y + 124 && mouseY <= y + 140) {
            if (selectedRowForPicker >= 0 && selectedRowForPicker < workingList.size()) {
                cycleSpecimenForSelectedRow(button == 1 ? -1 : 1);
                return true;
            }
        }

        // Action Buttons Row (y = 137)
        int btnY = y + 137;
        if (mouseY >= btnY && mouseY <= btnY + 14) {
            // [+ Add]
            if (mouseX >= x + 12 && mouseX <= x + 48 && workingList.size() < 8 && getUnallocatedPercent() > 0) {
                playClickSound();
                Item initialTarget = findFirstUnusedSpecimen();
                int pct = Math.min(25, Math.max(5, getUnallocatedPercent()));
                workingList.add(new PartitionDraft(initialTarget, pct, false));
                selectedRowForPicker = workingList.size() - 1;
                if (workingList.size() > 3) {
                    scrollOffset = workingList.size() - 3;
                }
                dirty = true;
                return true;
            }
            // [Equalize]
            if (mouseX >= x + 51 && mouseX <= x + 95 && !workingList.isEmpty()) {
                playClickSound();
                int count = workingList.size();
                int share = 100 / count;
                int rem = 100 % count;
                for (int i = 0; i < count; i++) {
                    workingList.get(i).percent = share + (i < rem ? 1 : 0);
                }
                dirty = true;
                return true;
            }
            // [Clear]
            if (mouseX >= x + 98 && mouseX <= x + 134 && !workingList.isEmpty()) {
                playClickSound();
                workingList.clear();
                selectedRowForPicker = -1;
                scrollOffset = 0;
                dirty = true;
                return true;
            }
            // [Apply]
            if (mouseX >= x + 138 && mouseX <= x + 208) {
                playClickSound();
                applyPartitionsToServer();
                return true;
            }
        }

        // Table rows interaction
        int tableX = x + 12;
        int tableY = y + 58;
        int maxVisible = 3;
        for (int i = 0; i < maxVisible; i++) {
            int index = scrollOffset + i;
            if (index >= workingList.size()) break;

            PartitionDraft p = workingList.get(index);
            int rowY = tableY + 14 + i * 20;

            // Specimen Icon Box
            if (mouseX >= tableX + 10 && mouseX <= tableX + 27 && mouseY >= rowY && mouseY <= rowY + 17) {
                ItemStack carried = menu.getCarried();
                if (!carried.isEmpty() && AquariumDropRegistry.isValidSpecimen(carried.getItem(), minecraft != null ? minecraft.level : null)) {
                    playClickSound();
                    p.target = carried.getItem();
                    selectedRowForPicker = -1;
                    dirty = true;
                    return true;
                }
                playClickSound();
                if (button == 1) {
                    selectedRowForPicker = index;
                    cycleSpecimenForSelectedRow(1);
                } else {
                    selectedRowForPicker = (selectedRowForPicker == index) ? -1 : index;
                }
                return true;
            }

            // [-] Button
            if (mouseX >= tableX + 88 && mouseX <= tableX + 99 && mouseY >= rowY + 3 && mouseY <= rowY + 14) {
                playClickSound();
                int step = Screen.hasShiftDown() ? 1 : 5;
                p.percent = Math.max(1, p.percent - step);
                dirty = true;
                return true;
            }

            // [+] Button
            if (mouseX >= tableX + 122 && mouseX <= tableX + 133 && mouseY >= rowY + 3 && mouseY <= rowY + 14) {
                playClickSound();
                int unalloc = getUnallocatedPercent();
                if (unalloc > 0) {
                    int step = Screen.hasShiftDown() ? 1 : 5;
                    p.percent += Math.min(step, unalloc);
                    dirty = true;
                }
                return true;
            }

            // [Void] Button
            if (mouseX >= tableX + 138 && mouseX <= tableX + 164 && mouseY >= rowY + 3 && mouseY <= rowY + 14) {
                if (menu.hasVoidSecondaryCard()) {
                    playClickSound();
                    p.voidSecondary = !p.voidSecondary;
                    dirty = true;
                }
                return true;
            }

            // [x] Remove Button
            if (mouseX >= tableX + 170 && mouseX <= tableX + 181 && mouseY >= rowY + 3 && mouseY <= rowY + 14) {
                playClickSound();
                workingList.remove(index);
                if (selectedRowForPicker == index) {
                    selectedRowForPicker = -1;
                }
                if (scrollOffset > 0 && scrollOffset + maxVisible > workingList.size()) {
                    scrollOffset--;
                }
                dirty = true;
                return true;
            }
        }

        // Scrollbar buttons
        if (workingList.size() > maxVisible && mouseX >= tableX + 185 && mouseX <= tableX + 194) {
            if (mouseY >= tableY + 14 && mouseY <= tableY + 23 && scrollOffset > 0) {
                playClickSound();
                scrollOffset--;
                return true;
            }
            if (mouseY >= tableY + 64 && mouseY <= tableY + 73 && scrollOffset + maxVisible < workingList.size()) {
                playClickSound();
                scrollOffset++;
                return true;
            }
        }

        // Click on player inventory slot while a row is selected in picker mode
        Slot hoveredSlot = getSlotUnderMouse();
        if (hoveredSlot != null && hoveredSlot.hasItem() && selectedRowForPicker >= 0 && selectedRowForPicker < workingList.size()) {
            Item item = hoveredSlot.getItem().getItem();
            if (AquariumDropRegistry.isValidSpecimen(item, minecraft != null ? minecraft.level : null)) {
                playClickSound();
                workingList.get(selectedRowForPicker).target = item;
                selectedRowForPicker = -1;
                dirty = true;
                return true;
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void cycleSpecimenForSelectedRow(int direction) {
        if (selectedRowForPicker < 0 || selectedRowForPicker >= workingList.size()) {
            return;
        }
        List<Item> candidates = getAvailableSpecimens();
        if (candidates.isEmpty()) {
            return;
        }
        PartitionDraft draft = workingList.get(selectedRowForPicker);
        int currentIdx = candidates.indexOf(draft.target);
        int nextIdx = Math.floorMod(currentIdx + direction, candidates.size());
        draft.target = candidates.get(nextIdx);
        dirty = true;
        playClickSound();
    }

    private List<Item> getAvailableSpecimens() {
        boolean enforceInv = false;
        try {
            if (AquariumConfig.SPEC.isLoaded()) {
                enforceInv = AquariumConfig.ENFORCE_INVENTORY_CHECK.get();
            }
        } catch (Exception ignored) {
        }

        if (!enforceInv) {
            return AquariumDropRegistry.getAllKnownSpecimens(minecraft != null ? minecraft.level : null);
        }

        List<Item> owned = new ArrayList<>();
        if (minecraft != null && minecraft.player != null) {
            Inventory inv = minecraft.player.getInventory();
            for (int i = 0; i < inv.getContainerSize(); i++) {
                ItemStack stack = inv.getItem(i);
                if (!stack.isEmpty()
                        && AquariumDropRegistry.isValidSpecimen(stack.getItem(), minecraft.level)
                        && !owned.contains(stack.getItem())) {
                    owned.add(stack.getItem());
                }
            }
        }
        return owned;
    }

    private Item findFirstUnusedSpecimen() {
        if (minecraft != null && minecraft.player != null) {
            Inventory inv = minecraft.player.getInventory();
            for (int i = 0; i < inv.getContainerSize(); i++) {
                ItemStack stack = inv.getItem(i);
                if (!stack.isEmpty() && AquariumDropRegistry.isValidSpecimen(stack.getItem(), minecraft.level)) {
                    boolean used = workingList.stream().anyMatch(d -> d.target == stack.getItem());
                    if (!used) {
                        return stack.getItem();
                    }
                }
            }
        }
        for (Item item : getAvailableSpecimens()) {
            boolean used = workingList.stream().anyMatch(d -> d.target == item);
            if (!used) {
                return item;
            }
        }
        return Items.TUBE_CORAL_BLOCK;
    }

    private void applyPartitionsToServer() {
        List<AquariumPartition> list = new ArrayList<>();
        for (PartitionDraft draft : workingList) {
            if (draft.target != null && draft.target != Items.AIR && draft.percent > 0) {
                list.add(new AquariumPartition(draft.target, draft.percent, draft.voidSecondary));
            }
        }
        PacketDistributor.sendToServer(new SetAquariumPartitionsPayload(list));
        dirty = false;
    }

    private void playClickSound() {
        if (minecraft != null) {
            minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
        }
    }
}

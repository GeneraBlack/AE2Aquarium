package de.project.ae2aquarium.menu;

import appeng.core.definitions.AEItems;
import de.project.ae2aquarium.block.AquariumControllerBlockEntity;
import de.project.ae2aquarium.config.AquariumConfig;
import de.project.ae2aquarium.multiblock.AquariumBounds;
import de.project.ae2aquarium.partition.AquariumPartition;
import de.project.ae2aquarium.partition.AquariumPartitionList;
import de.project.ae2aquarium.recipe.AquariumDropRegistry;
import de.project.ae2aquarium.registry.ModBlocks;
import de.project.ae2aquarium.registry.ModMenus;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class AquariumControllerMenu extends AbstractContainerMenu {

    @Nullable
    private final AquariumControllerBlockEntity blockEntity;
    private final ContainerLevelAccess access;
    public final Container upgradeContainer;
    public final Container cellContainer;
    private AquariumPartitionList initialPartitions = AquariumPartitionList.EMPTY;

    public AquariumControllerMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf extraData) {
        this(
                containerId,
                playerInventory,
                resolveClientBlockEntity(playerInventory, extraData),
                ContainerLevelAccess.NULL
        );
    }

    @Nullable
    private static AquariumControllerBlockEntity resolveClientBlockEntity(Inventory playerInventory, RegistryFriendlyByteBuf extraData) {
        BlockPos pos = extraData.readBlockPos();
        if (playerInventory.player.level() != null) {
            BlockEntity be = playerInventory.player.level().getBlockEntity(pos);
            if (be instanceof AquariumControllerBlockEntity controller) {
                controller.readSyncState(extraData);
                return controller;
            }
        }
        return null;
    }

    public AquariumControllerMenu(
            int containerId,
            Inventory playerInventory,
            @Nullable AquariumControllerBlockEntity blockEntity,
            ContainerLevelAccess access
    ) {
        super(ModMenus.AQUARIUM_CONTROLLER_MENU.get(), containerId);
        this.blockEntity = blockEntity;
        this.access = access;
        this.upgradeContainer = blockEntity != null ? blockEntity.getUpgradeInventory() : new SimpleContainer(5);
        this.cellContainer = blockEntity != null ? blockEntity.getCellInventory() : new SimpleContainer(1);
        if (blockEntity != null) {
            this.initialPartitions = blockEntity.getPartitions();
        }

        // Slots 0..3: 4 Acceleration Card Slots (x = 12 + i * 18, y = 155)
        for (int i = 0; i < 4; i++) {
            this.addSlot(new Slot(this.upgradeContainer, i, 12 + i * 18, 155) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return stack.is(AEItems.SPEED_CARD.asItem());
                }

                @Override
                public int getMaxStackSize() {
                    return 1;
                }
            });
        }

        // Slot 4: 1 Void Secondary / Void Overflow Card Slot (x = 88, y = 155)
        this.addSlot(new Slot(this.upgradeContainer, 4, 88, 155) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return AquariumControllerBlockEntity.isVoidSecondaryCard(stack)
                        || stack.is(AEItems.VOID_CARD.asItem());
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }
        });

        // Slot 5: Dedicated ME Storage Cell Slot on the Right Panel (x = 234, y = 36)
        this.addSlot(new Slot(this.cellContainer, 0, 234, 36) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return AquariumControllerBlockEntity.isValidStorageCell(stack);
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }
        });

        // Player Inventory (Slots 6..32) at x = 30, y = 181
        int invStartX = 30;
        int invStartY = 181;
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, invStartX + col * 18, invStartY + row * 18));
            }
        }

        // Player Hotbar (Slots 33..41) at x = 30, y = 239
        int hotbarY = 239;
        for (int col = 0; col < 9; ++col) {
            this.addSlot(new Slot(playerInventory, col, invStartX + col * 18, hotbarY));
        }
    }

    @Nullable
    public AquariumControllerBlockEntity getBlockEntity() {
        return blockEntity;
    }

    public AquariumPartitionList getPartitions() {
        return blockEntity != null ? blockEntity.getPartitions() : initialPartitions;
    }

    @Nullable
    public AquariumBounds getFormedBounds() {
        return blockEntity != null ? blockEntity.getFormedBounds() : null;
    }

    public boolean isFormed() {
        return blockEntity != null && blockEntity.isFormed();
    }

    public boolean isNetworkOnline() {
        return blockEntity != null && blockEntity.isNetworkOnline();
    }

    public boolean hasStorageCell() {
        ItemStack stack = cellContainer.getItem(0);
        return AquariumControllerBlockEntity.isValidStorageCell(stack);
    }

    public ItemStack getStorageCellStack() {
        return cellContainer.getItem(0);
    }

    public boolean hasVoidSecondaryCard() {
        ItemStack stack = upgradeContainer.getItem(4);
        return AquariumControllerBlockEntity.isVoidSecondaryCard(stack);
    }

    public void applyPartitions(Player player, List<AquariumPartition> incoming) {
        if (blockEntity == null || player.level().isClientSide()) {
            return;
        }

        boolean enforceInventory = AquariumConfig.ENFORCE_INVENTORY_CHECK.get();
        Set<Item> ownedItems = new HashSet<>();
        if (enforceInventory) {
            for (AquariumPartition existing : blockEntity.getPartitions().partitions()) {
                ownedItems.add(existing.target());
            }
            Inventory inv = player.getInventory();
            for (int i = 0; i < inv.getContainerSize(); i++) {
                ItemStack s = inv.getItem(i);
                if (!s.isEmpty()) {
                    ownedItems.add(s.getItem());
                }
            }
        }

        List<AquariumPartition> validated = new ArrayList<>();
        for (AquariumPartition p : incoming) {
            if (p == null || p.target() == null || p.percent() <= 0) {
                continue;
            }
            if (!AquariumDropRegistry.isValidSpecimen(p.target(), player.level())) {
                continue;
            }
            if (enforceInventory && !ownedItems.contains(p.target())) {
                continue;
            }
            validated.add(p);
        }

        AquariumPartitionList sanitized = AquariumPartitionList.sanitize(validated);
        blockEntity.setPartitions(sanitized);
        player.level().playSound(
                null,
                blockEntity.getBlockPos(),
                SoundEvents.BUBBLE_COLUMN_UPWARDS_AMBIENT,
                SoundSource.BLOCKS,
                0.8f,
                1.2f
        );
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack quickMovedStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);

        if (slot != null && slot.hasItem()) {
            ItemStack rawStack = slot.getItem();
            quickMovedStack = rawStack.copy();

            // Slots 0..4: Upgrades, Slot 5: Storage Cell, Slots 6..41: Player Inventory
            if (index < 6) {
                if (!this.moveItemStackTo(rawStack, 6, 42, true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                if (AquariumControllerBlockEntity.isValidStorageCell(rawStack)) {
                    if (!this.moveItemStackTo(rawStack, 5, 6, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (rawStack.is(AEItems.SPEED_CARD.asItem())) {
                    if (!this.moveItemStackTo(rawStack, 0, 4, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (AquariumControllerBlockEntity.isVoidSecondaryCard(rawStack) || rawStack.is(AEItems.VOID_CARD.asItem())) {
                    if (!this.moveItemStackTo(rawStack, 4, 5, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (index < 33) {
                    if (!this.moveItemStackTo(rawStack, 33, 42, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (!this.moveItemStackTo(rawStack, 6, 33, false)) {
                    return ItemStack.EMPTY;
                }
            }

            if (rawStack.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }

        return quickMovedStack;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(this.access, player, ModBlocks.AQUARIUM_CONTROLLER.get());
    }
}

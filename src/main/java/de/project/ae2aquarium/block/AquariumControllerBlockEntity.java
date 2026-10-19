package de.project.ae2aquarium.block;

import appeng.api.config.Actionable;
import appeng.api.config.PowerMultiplier;
import appeng.api.networking.GridFlags;
import appeng.api.networking.IGrid;
import appeng.api.networking.IGridNode;
import appeng.api.networking.IManagedGridNode;
import appeng.api.networking.energy.IEnergyService;
import appeng.api.networking.security.IActionHost;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.AEKeyType;
import appeng.api.stacks.KeyCounter;
import appeng.api.storage.IStorageMounts;
import appeng.api.storage.IStorageProvider;
import appeng.api.storage.MEStorage;
import appeng.api.storage.StorageCells;
import appeng.api.storage.cells.CellState;
import appeng.api.storage.cells.IBasicCellItem;
import appeng.api.storage.cells.ISaveProvider;
import appeng.api.storage.cells.StorageCell;
import appeng.blockentity.grid.AENetworkedBlockEntity;
import appeng.core.definitions.AEItems;
import appeng.me.cells.BasicCellInventory;
import de.project.ae2aquarium.config.AquariumConfig;
import de.project.ae2aquarium.menu.AquariumControllerMenu;
import de.project.ae2aquarium.multiblock.AquariumBounds;
import de.project.ae2aquarium.multiblock.AquariumMultiblockValidator;
import de.project.ae2aquarium.partition.AquariumPartition;
import de.project.ae2aquarium.partition.AquariumPartitionList;
import de.project.ae2aquarium.recipe.AquariumDropEntry;
import de.project.ae2aquarium.recipe.AquariumDropRegistry;
import de.project.ae2aquarium.registry.ModBlockEntities;
import de.project.ae2aquarium.registry.ModItems;
import it.unimi.dsi.fastutil.objects.Object2LongMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class AquariumControllerBlockEntity extends AENetworkedBlockEntity
        implements IActionHost, MenuProvider, IStorageProvider, ISaveProvider {

    private static final List<String> SISTER_MOD_NAMESPACES = List.of(
            "ae2virtualgarden", "ae2virtualmine", "ae2virtualbattle", "ae2virtualwell"
    );

    @Nullable
    private AquariumBounds formedBounds = null;
    private AquariumPartitionList partitions = AquariumPartitionList.EMPTY;
    private boolean clientOnline = false;
    private long clientCellUsedBytes = 0;
    private long clientCellTotalBytes = 0;
    private int clientCellUsedTypes = 0;
    private int clientCellTotalTypes = 0;

    private boolean validationRequested = true;
    private boolean updatingShellStates = false;
    private int tickCounter = 0;
    private int generationProgress = 0;

    private final IActionSource actionSource = IActionSource.ofMachine(this);

    private final SimpleContainer upgradeInventory = new SimpleContainer(5) {
        @Override
        public void setChanged() {
            super.setChanged();
            AquariumControllerBlockEntity.this.setChanged();
        }
    };

    private final SimpleContainer cellInventory = new SimpleContainer(1) {
        @Override
        public void setChanged() {
            super.setChanged();
            AquariumControllerBlockEntity.this.onCellSlotChanged();
        }
    };

    private IItemHandler externalItemHandler;

    public AquariumControllerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.AQUARIUM_CONTROLLER.get(), pos, state);
    }

    @Override
    protected IManagedGridNode createMainNode() {
        return super.createMainNode()
                .setVisualRepresentation(ModItems.AQUARIUM_CONTROLLER.get())
                .setIdlePowerUsage(1.0)
                .setFlags(GridFlags.REQUIRE_CHANNEL)
                .addService(IStorageProvider.class, this);
    }

    @Override
    protected Item getItemFromBlockEntity() {
        return ModItems.AQUARIUM_CONTROLLER.get();
    }

    @Nullable
    @Override
    public IGridNode getActionableNode() {
        return getMainNode().getNode();
    }

    @Override
    public void onReady() {
        super.onReady();
        requestValidation();
        refreshCellTelemetry();
        IStorageProvider.requestUpdate(getMainNode());
    }

    public static boolean isValidStorageCell(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (itemId != null && (itemId.getNamespace().startsWith("ae2virtual") || SISTER_MOD_NAMESPACES.contains(itemId.getNamespace()))) {
            // Reject read-only virtual generator cells from AE2VirtualGarden/Mine/Battle/Well
            return false;
        }
        if (stack.getItem() instanceof IBasicCellItem basicCell) {
            return basicCell.isStorageCell(stack) && basicCell.getKeyType() == AEKeyType.items();
        }
        return StorageCells.isCellHandled(stack);
    }

    @Nullable
    public StorageCell getInternalStorageCell() {
        ItemStack cellStack = cellInventory.getItem(0);
        if (!isValidStorageCell(cellStack)) {
            return null;
        }
        return StorageCells.getCellInventory(cellStack, this);
    }

    public boolean hasStorageCell() {
        return isValidStorageCell(cellInventory.getItem(0));
    }

    @Override
    public void saveChanges() {
        refreshCellTelemetry();
        setChanged();
    }

    private void onCellSlotChanged() {
        refreshCellTelemetry();
        setChanged();
        if (level != null && !level.isClientSide()) {
            IStorageProvider.requestUpdate(getMainNode());
            markForUpdate();
        }
    }

    private void refreshCellTelemetry() {
        ItemStack stack = cellInventory.getItem(0);
        if (!isValidStorageCell(stack)) {
            this.clientCellUsedBytes = 0;
            this.clientCellTotalBytes = 0;
            this.clientCellUsedTypes = 0;
            this.clientCellTotalTypes = 0;
            return;
        }
        StorageCell cell = StorageCells.getCellInventory(stack, this);
        if (cell instanceof BasicCellInventory basic) {
            this.clientCellUsedBytes = basic.getUsedBytes();
            this.clientCellTotalBytes = basic.getTotalBytes();
            this.clientCellUsedTypes = (int) basic.getStoredItemTypes();
            this.clientCellTotalTypes = (int) basic.getTotalItemTypes();
        } else if (stack.getItem() instanceof IBasicCellItem basicItem) {
            this.clientCellTotalBytes = basicItem.getBytes(stack);
            this.clientCellTotalTypes = basicItem.getTotalTypes(stack);
        }
    }

    @Override
    public void mountInventories(IStorageMounts storageMounts) {
        StorageCell cellInv = getInternalStorageCell();
        if (cellInv != null) {
            storageMounts.mount(new ControllerCellMountWrapper(cellInv), IStorageMounts.DEFAULT_PRIORITY);
        }
    }

    /**
     * Exposes the controller's internal Storage Cell to the ME Network as read/extract-only
     * so external ME devices never dump unrelated items into the Aquarium's cell,
     * while ME Terminals and Autocrafting CPUs can freely view and extract all cultivated reef items.
     */
    private class ControllerCellMountWrapper implements MEStorage {
        private final StorageCell delegate;

        private ControllerCellMountWrapper(StorageCell delegate) {
            this.delegate = delegate;
        }

        @Override
        public boolean isPreferredStorageFor(AEKey what, IActionSource source) {
            return false;
        }

        @Override
        public long insert(AEKey what, long amount, Actionable mode, IActionSource source) {
            // Only the Aquarium Controller itself inserts into its internal Storage Cell
            return 0;
        }

        @Override
        public long extract(AEKey what, long amount, Actionable mode, IActionSource source) {
            long extracted = delegate.extract(what, amount, mode, source);
            if (extracted > 0 && mode == Actionable.MODULATE) {
                delegate.persist();
                saveChanges();
                markForUpdate();
            }
            return extracted;
        }

        @Override
        public void getAvailableStacks(KeyCounter out) {
            delegate.getAvailableStacks(out);
        }

        @Override
        public Component getDescription() {
            return delegate.getDescription();
        }
    }

    public boolean isUpdatingShellStates() {
        return updatingShellStates;
    }

    public void requestValidation() {
        if (!updatingShellStates) {
            this.validationRequested = true;
        }
    }

    public void onControllerRemoved() {
        if (level != null && !level.isClientSide() && formedBounds != null) {
            resetShellStates(formedBounds);
            formedBounds = null;
        }
    }

    public void validateStructureNow() {
        if (level == null || level.isClientSide()) {
            return;
        }
        this.validationRequested = false;
        AquariumBounds previousBounds = this.formedBounds;
        AquariumBounds detected = AquariumMultiblockValidator.findValidAquarium(level, worldPosition);
        boolean onlineNow = isNetworkOnlineServer();

        boolean boundsChanged = !Objects.equals(previousBounds, detected);
        boolean onlineChanged = (this.clientOnline != onlineNow);

        this.formedBounds = detected;
        this.clientOnline = onlineNow;

        if (boundsChanged && previousBounds != null) {
            resetShellStates(previousBounds);
        }
        if (detected != null) {
            applyFormedShellStates(detected);
        }

        BlockState state = getBlockState();
        if (state.getBlock() instanceof AquariumControllerBlock && state.getValue(AquariumControllerBlock.FORMED) != isFormed()) {
            updatingShellStates = true;
            try {
                level.setBlock(worldPosition, state.setValue(AquariumControllerBlock.FORMED, isFormed()), 3);
            } finally {
                updatingShellStates = false;
            }
        }

        if (boundsChanged || onlineChanged) {
            setChanged();
            markForUpdate();
        }
    }

    private void applyFormedShellStates(AquariumBounds bounds) {
        if (level == null || level.isClientSide()) {
            return;
        }
        updatingShellStates = true;
        try {
            BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
            for (int y = bounds.minY(); y <= bounds.maxY(); y++) {
                for (int x = bounds.minX(); x <= bounds.maxX(); x++) {
                    for (int z = bounds.minZ(); z <= bounds.maxZ(); z++) {
                        if (bounds.isInterior(x, y, z)) {
                            continue;
                        }
                        cursor.set(x, y, z);
                        BlockState st = level.getBlockState(cursor);
                        if (st.getBlock() instanceof AquariumCasingBlock) {
                            AquariumCasingBlock.CasingPart targetPart;
                            if (y == bounds.minY()) {
                                targetPart = AquariumCasingBlock.CasingPart.BASE;
                            } else if (y == bounds.maxY()) {
                                targetPart = AquariumCasingBlock.CasingPart.ROOF;
                            } else {
                                targetPart = AquariumCasingBlock.CasingPart.FRAME;
                            }
                            if (st.getValue(AquariumCasingBlock.PART) != targetPart) {
                                level.setBlock(cursor.immutable(), st.setValue(AquariumCasingBlock.PART, targetPart), 3);
                            }
                        } else if (st.getBlock() instanceof AquariumGlassBlock) {
                            if (!st.getValue(AquariumGlassBlock.FORMED)) {
                                level.setBlock(cursor.immutable(), st.setValue(AquariumGlassBlock.FORMED, true), 3);
                            }
                        }
                    }
                }
            }
        } finally {
            updatingShellStates = false;
        }
    }

    private void resetShellStates(AquariumBounds bounds) {
        if (level == null || level.isClientSide()) {
            return;
        }
        updatingShellStates = true;
        try {
            BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
            for (int y = bounds.minY(); y <= bounds.maxY(); y++) {
                for (int x = bounds.minX(); x <= bounds.maxX(); x++) {
                    for (int z = bounds.minZ(); z <= bounds.maxZ(); z++) {
                        if (bounds.isInterior(x, y, z)) {
                            continue;
                        }
                        cursor.set(x, y, z);
                        if (!level.isLoaded(cursor)) {
                            continue;
                        }
                        BlockState st = level.getBlockState(cursor);
                        if (st.getBlock() instanceof AquariumCasingBlock
                                && st.getValue(AquariumCasingBlock.PART) != AquariumCasingBlock.CasingPart.UNFORMED) {
                            level.setBlock(cursor.immutable(), st.setValue(AquariumCasingBlock.PART, AquariumCasingBlock.CasingPart.UNFORMED), 3);
                        } else if (st.getBlock() instanceof AquariumGlassBlock
                                && st.getValue(AquariumGlassBlock.FORMED)) {
                            level.setBlock(cursor.immutable(), st.setValue(AquariumGlassBlock.FORMED, false), 3);
                        }
                    }
                }
            }
        } finally {
            updatingShellStates = false;
        }
    }

    public void serverTick() {
        if (level == null || level.isClientSide()) {
            return;
        }

        tickCounter++;
        if (validationRequested || tickCounter % 20 == 0) {
            validateStructureNow();
        }

        if (!isFormed() || formedBounds == null) {
            return;
        }

        if (!hasStorageCell()) {
            return;
        }

        if (tickCounter % 5 != 0) {
            return;
        }

        boolean requireEnergy = AquariumConfig.REQUIRE_AE_ENERGY.get();
        IGrid grid = getMainNode().getGrid();
        IEnergyService energyService = grid != null ? grid.getEnergyService() : null;

        if (requireEnergy) {
            if (!getMainNode().isActive() || energyService == null || !energyService.isNetworkPowered()) {
                return;
            }
        }

        int targetInterval = getTargetInterval();
        generationProgress += 5;
        if (generationProgress >= targetInterval) {
            generationProgress = 0;
            processGenerationCycle(energyService, requireEnergy);
        }
    }

    private void processGenerationCycle(@Nullable IEnergyService energyService, boolean requireEnergy) {
        if (level == null || formedBounds == null || partitions.isEmpty()) {
            return;
        }

        StorageCell cellInv = getInternalStorageCell();
        if (cellInv == null) {
            return;
        }

        int innerVolume = formedBounds.innerVolume();
        int dropCycles = getDropsPerCycle();
        if (dropCycles <= 0) {
            return;
        }

        int speedCards = getInstalledSpeedCards();
        double baseEnergy = AquariumConfig.ENERGY_PER_DROP.get();
        double energyPerDrop = baseEnergy * Math.pow(1.5, speedCards);

        boolean globalVoidSecondary = hasVoidSecondaryCard();
        boolean voidOverflow = hasVoidOverflowCard();
        RandomSource random = level.getRandom();
        boolean cellModified = false;

        for (int c = 0; c < dropCycles; c++) {
            int roll = random.nextInt(100);
            int cumulative = 0;
            AquariumPartition selected = null;

            for (AquariumPartition p : partitions.partitions()) {
                cumulative += p.percent();
                if (roll < cumulative) {
                    selected = p;
                    break;
                }
            }

            if (selected == null) {
                continue;
            }

            Item specimen = selected.target();
            if (specimen == null || !AquariumDropRegistry.isValidSpecimen(specimen, level)) {
                continue;
            }

            List<AquariumDropEntry> dropEntries = AquariumDropRegistry.getDropEntries(specimen, level, innerVolume);
            if (dropEntries.isEmpty()) {
                continue;
            }

            AquariumDropRegistry.RolledDrop rolledDrop = AquariumDropRegistry.rollDropWithIndex(dropEntries, random);
            if (rolledDrop.stack().isEmpty()) {
                continue;
            }

            boolean voidThisSecondary = globalVoidSecondary && selected.voidSecondary();
            if (voidThisSecondary && rolledDrop.isSecondary()) {
                if (requireEnergy && energyService != null && energyPerDrop > 0) {
                    double extracted = energyService.extractAEPower(energyPerDrop, Actionable.SIMULATE, PowerMultiplier.CONFIG);
                    if (extracted < energyPerDrop) {
                        break;
                    }
                    energyService.extractAEPower(energyPerDrop, Actionable.MODULATE, PowerMultiplier.CONFIG);
                }
                continue;
            }

            // Enforce per-partition byte share on the installed Storage Cell so one specimen never clogs the cell
            if (cellInv instanceof BasicCellInventory basicCell) {
                long totalBytes = basicCell.getTotalBytes();
                long maxPartitionBytes = Math.max(1L, (totalBytes * selected.percent()) / 100L);
                long usedPartitionBytes = computePartitionUsedBytes(basicCell, dropEntries);
                if (usedPartitionBytes >= maxPartitionBytes) {
                    if (voidOverflow) {
                        if (requireEnergy && energyService != null && energyPerDrop > 0) {
                            double extracted = energyService.extractAEPower(energyPerDrop, Actionable.SIMULATE, PowerMultiplier.CONFIG);
                            if (extracted < energyPerDrop) {
                                break;
                            }
                            energyService.extractAEPower(energyPerDrop, Actionable.MODULATE, PowerMultiplier.CONFIG);
                        }
                    }
                    continue;
                }
            }

            ItemStack dropStack = rolledDrop.stack();
            int dropCount = dropStack.getCount();
            AEItemKey itemKey = AEItemKey.of(dropStack);
            if (itemKey == null) {
                continue;
            }

            long canInsert = cellInv.insert(itemKey, dropCount, Actionable.SIMULATE, actionSource);
            int canAccept = voidOverflow ? dropCount : (int) Math.min(dropCount, canInsert);
            if (canAccept <= 0) {
                continue;
            }

            double scaledEnergy = (canAccept < dropCount)
                    ? energyPerDrop * ((double) canAccept / dropCount)
                    : energyPerDrop;

            if (requireEnergy && energyService != null && scaledEnergy > 0) {
                double extracted = energyService.extractAEPower(scaledEnergy, Actionable.SIMULATE, PowerMultiplier.CONFIG);
                if (extracted < scaledEnergy) {
                    break;
                }
                energyService.extractAEPower(scaledEnergy, Actionable.MODULATE, PowerMultiplier.CONFIG);
            }

            if (canInsert > 0) {
                long inserted = cellInv.insert(itemKey, Math.min(dropCount, canInsert), Actionable.MODULATE, actionSource);
                if (inserted > 0) {
                    cellModified = true;
                }
            }
        }

        if (cellModified) {
            cellInv.persist();
            saveChanges();
            IStorageProvider.requestUpdate(getMainNode());
            markForUpdate();
        }
    }

    private long computePartitionUsedBytes(BasicCellInventory basicCell, List<AquariumDropEntry> dropEntries) {
        KeyCounter available = basicCell.getAvailableStacks();
        int bytesPerType = basicCell.getBytesPerType();
        long totalItems = 0;
        int activeTypes = 0;
        for (AquariumDropEntry entry : dropEntries) {
            AEItemKey key = AEItemKey.of(entry.item());
            if (key != null) {
                long count = available.get(key);
                if (count > 0) {
                    activeTypes++;
                    totalItems += count;
                }
            }
        }
        if (activeTypes == 0) {
            return 0;
        }
        return (long) activeTypes * bytesPerType + (totalItems + 7L) / 8L;
    }

    public boolean isFormed() {
        return formedBounds != null;
    }

    @Nullable
    public AquariumBounds getFormedBounds() {
        return formedBounds;
    }

    public boolean isNetworkOnline() {
        if (level != null && level.isClientSide()) {
            return clientOnline;
        }
        return isNetworkOnlineServer();
    }

    private boolean isNetworkOnlineServer() {
        if (!AquariumConfig.REQUIRE_AE_ENERGY.get()) {
            return true;
        }
        IGrid grid = getMainNode().getGrid();
        return getMainNode().isActive() && grid != null && grid.getEnergyService().isNetworkPowered();
    }

    public AquariumPartitionList getPartitions() {
        return partitions;
    }

    public void setPartitions(AquariumPartitionList newPartitions) {
        this.partitions = newPartitions != null ? newPartitions : AquariumPartitionList.EMPTY;
        setChanged();
        markForUpdate();
    }

    public Container getUpgradeInventory() {
        return upgradeInventory;
    }

    public Container getCellInventory() {
        return cellInventory;
    }

    public long getCellUsedBytes() {
        return clientCellUsedBytes;
    }

    public long getCellTotalBytes() {
        return clientCellTotalBytes;
    }

    public int getCellUsedTypes() {
        return clientCellUsedTypes;
    }

    public int getCellTotalTypes() {
        return clientCellTotalTypes;
    }

    public CellState getCellState() {
        if (!hasStorageCell() || clientCellTotalBytes <= 0) {
            return CellState.ABSENT;
        }
        if (clientCellUsedBytes <= 0) {
            return CellState.EMPTY;
        }
        if (clientCellUsedBytes >= clientCellTotalBytes) {
            return CellState.FULL;
        }
        if (clientCellTotalTypes > 0 && clientCellUsedTypes >= clientCellTotalTypes) {
            return CellState.TYPES_FULL;
        }
        return CellState.NOT_EMPTY;
    }

    public int getInstalledSpeedCards() {
        int count = 0;
        for (int i = 0; i < 4; i++) {
            ItemStack stack = upgradeInventory.getItem(i);
            if (!stack.isEmpty() && stack.is(AEItems.SPEED_CARD.asItem())) {
                count++;
            }
        }
        return count;
    }

    public boolean hasVoidSecondaryCard() {
        ItemStack stack = upgradeInventory.getItem(4);
        return isVoidSecondaryCard(stack);
    }

    public boolean hasVoidOverflowCard() {
        ItemStack stack = upgradeInventory.getItem(4);
        return !stack.isEmpty() && stack.is(AEItems.VOID_CARD.asItem());
    }

    public static boolean isVoidSecondaryCard(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        if (stack.is(ModItems.VOID_SECONDARY_CARD.get())) {
            return true;
        }
        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return itemId != null
                && "void_secondary_card".equals(itemId.getPath())
                && SISTER_MOD_NAMESPACES.contains(itemId.getNamespace());
    }

    public int getTargetInterval() {
        int baseInterval = 60;
        try {
            if (AquariumConfig.SPEC.isLoaded()) {
                baseInterval = AquariumConfig.BASE_TICK_INTERVAL.get();
            }
        } catch (Exception ignored) {
        }
        return calculateTargetInterval(baseInterval, getInstalledSpeedCards());
    }

    public static int calculateTargetInterval(int baseInterval, int speedCards) {
        return switch (Math.clamp(speedCards, 0, 4)) {
            case 1 -> Math.max(5, (int) (baseInterval * 0.70));
            case 2 -> Math.max(5, (int) (baseInterval * 0.45));
            case 3 -> Math.max(5, (int) (baseInterval * 0.30));
            case 4 -> Math.max(10, (int) (baseInterval * 0.20));
            default -> Math.max(5, baseInterval);
        };
    }

    public int getDropsPerCycle() {
        if (formedBounds == null) {
            return 0;
        }
        double mult = 1.0;
        int maxCap = 1024;
        try {
            if (AquariumConfig.SPEC.isLoaded()) {
                mult = AquariumConfig.VOLUME_DROP_MULTIPLIER.get();
                maxCap = AquariumConfig.MAX_DROPS_PER_CYCLE.get();
            }
        } catch (Exception ignored) {
        }
        return formedBounds.calculateDropsPerCycle(mult, maxCap);
    }

    public void writeMenuOpenData(RegistryFriendlyByteBuf buf) {
        buf.writeBlockPos(worldPosition);
        writeSyncState(buf);
    }

    public void writeSyncState(RegistryFriendlyByteBuf buf) {
        refreshCellTelemetry();
        buf.writeBoolean(formedBounds != null);
        if (formedBounds != null) {
            buf.writeVarInt(formedBounds.minX());
            buf.writeVarInt(formedBounds.minY());
            buf.writeVarInt(formedBounds.minZ());
            buf.writeVarInt(formedBounds.maxX());
            buf.writeVarInt(formedBounds.maxY());
            buf.writeVarInt(formedBounds.maxZ());
        }
        buf.writeBoolean(isNetworkOnline());
        buf.writeVarLong(clientCellUsedBytes);
        buf.writeVarLong(clientCellTotalBytes);
        buf.writeVarInt(clientCellUsedTypes);
        buf.writeVarInt(clientCellTotalTypes);
        AquariumPartitionList.STREAM_CODEC.encode(buf, partitions);
    }

    public void readSyncState(RegistryFriendlyByteBuf buf) {
        boolean hasBounds = buf.readBoolean();
        if (hasBounds) {
            int minX = buf.readVarInt();
            int minY = buf.readVarInt();
            int minZ = buf.readVarInt();
            int maxX = buf.readVarInt();
            int maxY = buf.readVarInt();
            int maxZ = buf.readVarInt();
            this.formedBounds = new AquariumBounds(minX, minY, minZ, maxX, maxY, maxZ);
        } else {
            this.formedBounds = null;
        }
        this.clientOnline = buf.readBoolean();
        this.clientCellUsedBytes = buf.readVarLong();
        this.clientCellTotalBytes = buf.readVarLong();
        this.clientCellUsedTypes = buf.readVarInt();
        this.clientCellTotalTypes = buf.readVarInt();
        this.partitions = AquariumPartitionList.STREAM_CODEC.decode(buf);
    }

    @Override
    protected void writeToStream(RegistryFriendlyByteBuf data) {
        super.writeToStream(data);
        writeSyncState(data);
    }

    @Override
    protected boolean readFromStream(RegistryFriendlyByteBuf data) {
        boolean changed = super.readFromStream(data);
        AquariumBounds oldBounds = this.formedBounds;
        boolean oldOnline = this.clientOnline;
        long oldUsedBytes = this.clientCellUsedBytes;
        long oldTotalBytes = this.clientCellTotalBytes;
        AquariumPartitionList oldPartitions = this.partitions;
        readSyncState(data);
        return changed
                || !Objects.equals(oldBounds, this.formedBounds)
                || oldOnline != this.clientOnline
                || oldUsedBytes != this.clientCellUsedBytes
                || oldTotalBytes != this.clientCellTotalBytes
                || !Objects.equals(oldPartitions, this.partitions);
    }

    @Override
    public void saveAdditional(@NotNull CompoundTag data, @NotNull HolderLookup.Provider registries) {
        super.saveAdditional(data, registries);
        if (formedBounds != null) {
            data.put("FormedBounds", formedBounds.saveToTag());
        }
        AquariumPartitionList.CODEC.encodeStart(registries.createSerializationContext(NbtOps.INSTANCE), partitions)
                .result()
                .ifPresent(tag -> data.put("Partitions", tag));
        saveContainer(data, "Upgrades", upgradeInventory, registries);
        saveContainer(data, "CellInventory", cellInventory, registries);
    }

    @Override
    public void loadTag(@NotNull CompoundTag data, @NotNull HolderLookup.Provider registries) {
        super.loadTag(data, registries);
        if (data.contains("FormedBounds", Tag.TAG_COMPOUND)) {
            this.formedBounds = AquariumBounds.loadFromTag(data.getCompound("FormedBounds"));
        } else {
            this.formedBounds = null;
        }
        if (data.contains("Partitions")) {
            this.partitions = AquariumPartitionList.CODEC
                    .parse(registries.createSerializationContext(NbtOps.INSTANCE), data.get("Partitions"))
                    .result()
                    .orElse(AquariumPartitionList.EMPTY);
        } else {
            this.partitions = AquariumPartitionList.EMPTY;
        }
        loadContainer(data, "Upgrades", upgradeInventory, registries);
        loadContainer(data, "CellInventory", cellInventory, registries);
        refreshCellTelemetry();
    }

    private static void saveContainer(CompoundTag root, String key, SimpleContainer container, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (int i = 0; i < container.getContainerSize(); i++) {
            ItemStack stack = container.getItem(i);
            if (!stack.isEmpty()) {
                CompoundTag slotTag = new CompoundTag();
                slotTag.putByte("Slot", (byte) i);
                list.add(stack.save(registries, slotTag));
            }
        }
        root.put(key, list);
    }

    private static void loadContainer(CompoundTag root, String key, SimpleContainer container, HolderLookup.Provider registries) {
        container.clearContent();
        if (!root.contains(key, Tag.TAG_LIST)) {
            return;
        }
        ListTag list = root.getList(key, Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag slotTag = list.getCompound(i);
            int slot = slotTag.getByte("Slot") & 255;
            if (slot < container.getContainerSize()) {
                container.setItem(slot, ItemStack.parseOptional(registries, slotTag));
            }
        }
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("gui.ae2aquarium.aquarium_controller");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new AquariumControllerMenu(
                containerId,
                playerInventory,
                this,
                level != null ? ContainerLevelAccess.create(level, worldPosition) : ContainerLevelAccess.NULL
        );
    }

    public IItemHandler getItemHandler(@Nullable Direction side) {
        if (externalItemHandler == null) {
            externalItemHandler = new CellExtractionItemHandler();
        }
        return externalItemHandler;
    }

    private class CellExtractionItemHandler implements IItemHandler {
        private List<Object2LongMap.Entry<AEKey>> getEntries() {
            StorageCell cell = getInternalStorageCell();
            if (cell == null) {
                return List.of();
            }
            KeyCounter counter = cell.getAvailableStacks();
            List<Object2LongMap.Entry<AEKey>> list = new ArrayList<>();
            for (Object2LongMap.Entry<AEKey> entry : counter) {
                if (entry.getKey() instanceof AEItemKey && entry.getLongValue() > 0) {
                    list.add(entry);
                }
            }
            return list;
        }

        @Override
        public int getSlots() {
            return Math.max(1, getEntries().size());
        }

        @NotNull
        @Override
        public ItemStack getStackInSlot(int slot) {
            List<Object2LongMap.Entry<AEKey>> entries = getEntries();
            if (slot < 0 || slot >= entries.size()) {
                return ItemStack.EMPTY;
            }
            Object2LongMap.Entry<AEKey> entry = entries.get(slot);
            if (entry.getKey() instanceof AEItemKey itemKey) {
                int count = (int) Math.min(itemKey.getMaxStackSize(), entry.getLongValue());
                return itemKey.toStack(count);
            }
            return ItemStack.EMPTY;
        }

        @NotNull
        @Override
        public ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
            return stack;
        }

        @NotNull
        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            if (amount <= 0) {
                return ItemStack.EMPTY;
            }
            StorageCell cell = getInternalStorageCell();
            if (cell == null) {
                return ItemStack.EMPTY;
            }
            List<Object2LongMap.Entry<AEKey>> entries = getEntries();
            if (slot < 0 || slot >= entries.size()) {
                return ItemStack.EMPTY;
            }
            Object2LongMap.Entry<AEKey> entry = entries.get(slot);
            if (!(entry.getKey() instanceof AEItemKey itemKey)) {
                return ItemStack.EMPTY;
            }
            int maxExtract = Math.min(amount, itemKey.getMaxStackSize());
            long extracted = cell.extract(itemKey, maxExtract, simulate ? Actionable.SIMULATE : Actionable.MODULATE, actionSource);
            if (extracted <= 0) {
                return ItemStack.EMPTY;
            }
            if (!simulate) {
                cell.persist();
                saveChanges();
                IStorageProvider.requestUpdate(getMainNode());
                markForUpdate();
            }
            return itemKey.toStack((int) extracted);
        }

        @Override
        public int getSlotLimit(int slot) {
            return 64;
        }

        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return false;
        }
    }
}

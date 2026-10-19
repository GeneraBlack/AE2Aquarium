package de.project.ae2aquarium.block;

import net.minecraft.core.BlockPos;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class AquariumCasingBlock extends Block {

    public enum CasingPart implements StringRepresentable {
        UNFORMED("unformed"),
        BASE("base"),
        ROOF("roof"),
        FRAME("frame");

        private final String serializedName;

        CasingPart(String serializedName) {
            this.serializedName = serializedName;
        }

        @Override
        public String getSerializedName() {
            return serializedName;
        }
    }

    public static final EnumProperty<CasingPart> PART = EnumProperty.create("part", CasingPart.class);

    private static final VoxelShape BASE_SHAPE = Block.box(0.0, 0.0, 0.0, 16.0, 4.0, 16.0);
    private static final VoxelShape ROOF_SHAPE = Block.box(0.0, 13.0, 0.0, 16.0, 16.0, 16.0);
    private static final VoxelShape FRAME_SHAPE = Block.box(3.0, 0.0, 3.0, 13.0, 16.0, 13.0);

    public AquariumCasingBlock(Properties properties) {
        super(properties);
        registerDefaultState(getStateDefinition().any().setValue(PART, CasingPart.UNFORMED));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(PART);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(PART, CasingPart.UNFORMED);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return switch (state.getValue(PART)) {
            case BASE -> BASE_SHAPE;
            case ROOF -> ROOF_SHAPE;
            case FRAME -> FRAME_SHAPE;
            case UNFORMED -> Shapes.block();
        };
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.block();
    }

    @Override
    protected VoxelShape getVisualShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    @Override
    protected float getShadeBrightness(BlockState state, BlockGetter level, BlockPos pos) {
        return 1.0F;
    }

    @Override
    protected boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) {
        return state.getValue(PART) != CasingPart.UNFORMED;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide()) {
            notifyNearbyControllers(level, pos);
        }
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && !level.isClientSide()) {
            notifyNearbyControllers(level, pos);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    public static void notifyNearbyControllers(Level level, BlockPos origin) {
        int radius = 16;
        int heightRadius = 8;
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        List<AquariumControllerBlockEntity> controllers = new ArrayList<>();

        for (int dy = -heightRadius; dy <= heightRadius; dy++) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    cursor.set(origin.getX() + dx, origin.getY() + dy, origin.getZ() + dz);
                    if (level.isLoaded(cursor) && level.getBlockState(cursor).getBlock() instanceof AquariumControllerBlock) {
                        BlockEntity be = level.getBlockEntity(cursor);
                        if (be instanceof AquariumControllerBlockEntity controller && !controller.isUpdatingShellStates()) {
                            controllers.add(controller);
                        }
                    }
                }
            }
        }

        for (AquariumControllerBlockEntity controller : controllers) {
            controller.validateStructureNow();
        }

        // If no formed controller claims nearby blocks anymore, ensure orphaned formed blocks are reset
        for (int dy = -heightRadius; dy <= heightRadius; dy++) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    cursor.set(origin.getX() + dx, origin.getY() + dy, origin.getZ() + dz);
                    if (!level.isLoaded(cursor)) {
                        continue;
                    }
                    BlockState st = level.getBlockState(cursor);
                    boolean isFormedCasing = st.getBlock() instanceof AquariumCasingBlock
                            && st.getValue(PART) != CasingPart.UNFORMED;
                    boolean isFormedGlass = st.getBlock() instanceof AquariumGlassBlock
                            && st.getValue(AquariumGlassBlock.FORMED);
                    if (!isFormedCasing && !isFormedGlass) {
                        continue;
                    }
                    boolean claimed = false;
                    for (AquariumControllerBlockEntity controller : controllers) {
                        if (controller.getFormedBounds() != null && controller.getFormedBounds().isOnShell(cursor)) {
                            claimed = true;
                            break;
                        }
                    }
                    if (!claimed) {
                        if (isFormedCasing) {
                            level.setBlock(cursor.immutable(), st.setValue(PART, CasingPart.UNFORMED), 3);
                        } else {
                            level.setBlock(cursor.immutable(), st.setValue(AquariumGlassBlock.FORMED, false), 3);
                        }
                    }
                }
            }
        }
    }
}

package de.project.ae2aquarium.registry;

import de.project.ae2aquarium.AE2Aquarium;
import de.project.ae2aquarium.block.AquariumCasingBlock;
import de.project.ae2aquarium.block.AquariumControllerBlock;
import de.project.ae2aquarium.block.AquariumGlassBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(AE2Aquarium.MODID);

    public static final DeferredBlock<AquariumControllerBlock> AQUARIUM_CONTROLLER =
            BLOCKS.register("aquarium_controller", AquariumControllerBlock::new);

    public static final DeferredBlock<AquariumCasingBlock> AQUARIUM_CASING =
            BLOCKS.registerBlock("aquarium_casing", AquariumCasingBlock::new,
                    BlockBehaviour.Properties.of()
                            .strength(3.0f, 9.0f)
                            .sound(SoundType.METAL)
                            .noOcclusion()
                            .lightLevel(state -> 7)
                            .isViewBlocking((state, level, pos) -> false));

    public static final DeferredBlock<AquariumGlassBlock> AQUARIUM_GLASS =
            BLOCKS.registerBlock("aquarium_glass", AquariumGlassBlock::new,
                    BlockBehaviour.Properties.of()
                            .strength(2.0f, 6.0f)
                            .sound(SoundType.GLASS)
                            .noOcclusion()
                            .lightLevel(state -> 10)
                            .isViewBlocking((state, level, pos) -> false)
                            .isSuffocating((state, level, pos) -> false));
}

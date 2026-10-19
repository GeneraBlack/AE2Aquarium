package de.project.ae2aquarium.registry;

import de.project.ae2aquarium.AE2Aquarium;
import de.project.ae2aquarium.block.AquariumControllerBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, AE2Aquarium.MODID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<AquariumControllerBlockEntity>> AQUARIUM_CONTROLLER =
            BLOCK_ENTITIES.register("aquarium_controller", () -> {
                BlockEntityType<AquariumControllerBlockEntity> type = BlockEntityType.Builder.of(
                        AquariumControllerBlockEntity::new,
                        ModBlocks.AQUARIUM_CONTROLLER.get()
                ).build(null);
                ModBlocks.AQUARIUM_CONTROLLER.get().setBlockEntity(
                        AquariumControllerBlockEntity.class,
                        type,
                        null,
                        (level, pos, state, be) -> be.serverTick()
                );
                return type;
            });
}

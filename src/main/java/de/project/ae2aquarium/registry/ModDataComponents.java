package de.project.ae2aquarium.registry;

import de.project.ae2aquarium.AE2Aquarium;
import de.project.ae2aquarium.partition.AquariumPartitionList;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModDataComponents {
    public static final DeferredRegister<DataComponentType<?>> DATA_COMPONENTS =
            DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, AE2Aquarium.MODID);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<AquariumPartitionList>> PARTITIONS =
            DATA_COMPONENTS.register("partitions", () ->
                    DataComponentType.<AquariumPartitionList>builder()
                            .persistent(AquariumPartitionList.CODEC)
                            .networkSynchronized(AquariumPartitionList.STREAM_CODEC)
                            .build()
            );
}

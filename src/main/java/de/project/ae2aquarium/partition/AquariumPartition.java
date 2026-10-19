package de.project.ae2aquarium.partition;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.Item;

public record AquariumPartition(
        Item target,
        int percent,
        boolean voidSecondary
) {
    public static final Codec<AquariumPartition> CODEC = Codec.lazyInitialized(() -> RecordCodecBuilder.create(instance -> instance.group(
            BuiltInRegistries.ITEM.byNameCodec().fieldOf("target").forGetter(AquariumPartition::target),
            Codec.INT.fieldOf("percent").forGetter(AquariumPartition::percent),
            Codec.BOOL.optionalFieldOf("void_secondary", false).forGetter(AquariumPartition::voidSecondary)
    ).apply(instance, AquariumPartition::new)));

    public static final StreamCodec<RegistryFriendlyByteBuf, AquariumPartition> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.registry(Registries.ITEM), AquariumPartition::target,
            ByteBufCodecs.VAR_INT, AquariumPartition::percent,
            ByteBufCodecs.BOOL, AquariumPartition::voidSecondary,
            AquariumPartition::new
    );
}

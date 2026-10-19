package de.project.ae2aquarium.network;

import de.project.ae2aquarium.AE2Aquarium;
import de.project.ae2aquarium.partition.AquariumPartition;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

public record SetAquariumPartitionsPayload(List<AquariumPartition> partitions) implements CustomPacketPayload {
    public static final Type<SetAquariumPartitionsPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(AE2Aquarium.MODID, "set_aquarium_partitions"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SetAquariumPartitionsPayload> STREAM_CODEC =
            StreamCodec.composite(
                    AquariumPartition.STREAM_CODEC.apply(ByteBufCodecs.list()),
                    SetAquariumPartitionsPayload::partitions,
                    SetAquariumPartitionsPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

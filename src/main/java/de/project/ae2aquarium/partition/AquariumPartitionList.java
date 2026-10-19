package de.project.ae2aquarium.partition;

import com.mojang.serialization.Codec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.Item;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public record AquariumPartitionList(List<AquariumPartition> partitions) {

    public static final AquariumPartitionList EMPTY = new AquariumPartitionList(List.of());

    public static final Codec<AquariumPartitionList> CODEC =
            AquariumPartition.CODEC.listOf().xmap(AquariumPartitionList::new, AquariumPartitionList::partitions);

    public static final StreamCodec<RegistryFriendlyByteBuf, AquariumPartitionList> STREAM_CODEC =
            AquariumPartition.STREAM_CODEC.apply(ByteBufCodecs.list()).map(AquariumPartitionList::new, AquariumPartitionList::partitions);

    public boolean isEmpty() {
        return partitions == null || partitions.isEmpty();
    }

    public int size() {
        return partitions == null ? 0 : partitions.size();
    }

    public int getTotalPercent() {
        if (partitions == null) return 0;
        int total = 0;
        for (AquariumPartition p : partitions) {
            total += p.percent();
        }
        return total;
    }

    public int getUnallocatedPercent() {
        return Math.max(0, 100 - getTotalPercent());
    }

    @Nullable
    public AquariumPartition getPartition(Item item) {
        if (partitions == null) return null;
        for (AquariumPartition p : partitions) {
            if (p.target() == item) {
                return p;
            }
        }
        return null;
    }

    public boolean contains(Item item) {
        return getPartition(item) != null;
    }

    public AquariumPartitionList withUpdated(List<AquariumPartition> newPartitions) {
        return new AquariumPartitionList(new ArrayList<>(newPartitions));
    }

    public static AquariumPartitionList sanitize(List<AquariumPartition> raw) {
        if (raw == null || raw.isEmpty()) {
            return EMPTY;
        }
        List<AquariumPartition> cleaned = new ArrayList<>();
        int sum = 0;
        for (AquariumPartition p : raw) {
            if (p == null || p.target() == null || p.percent() <= 0) {
                continue;
            }
            boolean duplicate = cleaned.stream().anyMatch(existing -> existing.target() == p.target());
            if (duplicate) {
                continue;
            }
            int allowed = Math.min(p.percent(), 100 - sum);
            if (allowed > 0) {
                cleaned.add(new AquariumPartition(p.target(), allowed, p.voidSecondary()));
                sum += allowed;
            }
            if (sum >= 100) {
                break;
            }
        }
        return new AquariumPartitionList(List.copyOf(cleaned));
    }
}

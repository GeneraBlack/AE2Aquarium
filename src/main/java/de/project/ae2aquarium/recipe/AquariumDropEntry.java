package de.project.ae2aquarium.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;

public record AquariumDropEntry(ItemStack item, int weight, int minCount, int maxCount) {
    public static final Codec<AquariumDropEntry> CODEC = Codec.lazyInitialized(() -> RecordCodecBuilder.create(instance -> instance.group(
            ItemStack.CODEC.fieldOf("item").forGetter(AquariumDropEntry::item),
            Codec.INT.optionalFieldOf("weight", 1).forGetter(AquariumDropEntry::weight),
            Codec.INT.optionalFieldOf("min_count", 1).forGetter(AquariumDropEntry::minCount),
            Codec.INT.optionalFieldOf("max_count", 1).forGetter(AquariumDropEntry::maxCount)
    ).apply(instance, AquariumDropEntry::new)));

    public static final StreamCodec<RegistryFriendlyByteBuf, AquariumDropEntry> STREAM_CODEC = StreamCodec.of(
            (buf, val) -> {
                ItemStack.STREAM_CODEC.encode(buf, val.item());
                ByteBufCodecs.VAR_INT.encode(buf, val.weight());
                ByteBufCodecs.VAR_INT.encode(buf, val.minCount());
                ByteBufCodecs.VAR_INT.encode(buf, val.maxCount());
            },
            buf -> new AquariumDropEntry(
                    ItemStack.STREAM_CODEC.decode(buf),
                    ByteBufCodecs.VAR_INT.decode(buf),
                    ByteBufCodecs.VAR_INT.decode(buf),
                    ByteBufCodecs.VAR_INT.decode(buf)
            )
    );

    public int rollCount(RandomSource random) {
        int min = Math.max(1, minCount);
        int max = Math.max(min, maxCount);
        if (max > min) {
            return min + random.nextInt(max - min + 1);
        }
        return min;
    }
}

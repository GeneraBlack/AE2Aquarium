package de.project.ae2aquarium.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import de.project.ae2aquarium.registry.ModRecipes;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;

import java.util.List;

public record AquariumDropRecipe(Ingredient specimen, int minVolume, List<AquariumDropEntry> drops) implements Recipe<SingleRecipeInput> {

    @Override
    public boolean matches(SingleRecipeInput input, Level level) {
        return specimen.test(input.item());
    }

    @Override
    public ItemStack assemble(SingleRecipeInput input, HolderLookup.Provider registries) {
        if (!drops.isEmpty()) {
            return drops.get(0).item().copy();
        }
        return ItemStack.EMPTY;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        if (!drops.isEmpty()) {
            return drops.get(0).item().copy();
        }
        return ItemStack.EMPTY;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.AQUARIUM_DROP_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return ModRecipes.AQUARIUM_DROP_TYPE.get();
    }

    public static class Serializer implements RecipeSerializer<AquariumDropRecipe> {
        public static final MapCodec<AquariumDropRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                Ingredient.CODEC.fieldOf("specimen").forGetter(AquariumDropRecipe::specimen),
                Codec.INT.optionalFieldOf("min_volume", 1).forGetter(AquariumDropRecipe::minVolume),
                AquariumDropEntry.CODEC.listOf().fieldOf("drops").forGetter(AquariumDropRecipe::drops)
        ).apply(instance, AquariumDropRecipe::new));

        public static final StreamCodec<RegistryFriendlyByteBuf, AquariumDropRecipe> STREAM_CODEC = StreamCodec.composite(
                Ingredient.CONTENTS_STREAM_CODEC, AquariumDropRecipe::specimen,
                ByteBufCodecs.VAR_INT, AquariumDropRecipe::minVolume,
                AquariumDropEntry.STREAM_CODEC.apply(ByteBufCodecs.list()), AquariumDropRecipe::drops,
                AquariumDropRecipe::new
        );

        @Override
        public MapCodec<AquariumDropRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, AquariumDropRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}

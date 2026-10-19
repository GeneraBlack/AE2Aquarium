package de.project.ae2aquarium.registry;

import de.project.ae2aquarium.AE2Aquarium;
import de.project.ae2aquarium.recipe.AquariumDropRecipe;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModRecipes {
    public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS =
            DeferredRegister.create(Registries.RECIPE_SERIALIZER, AE2Aquarium.MODID);

    public static final DeferredRegister<RecipeType<?>> RECIPE_TYPES =
            DeferredRegister.create(Registries.RECIPE_TYPE, AE2Aquarium.MODID);

    public static final DeferredHolder<RecipeType<?>, RecipeType<AquariumDropRecipe>> AQUARIUM_DROP_TYPE =
            RECIPE_TYPES.register("aquarium_drop", () -> RecipeType.simple(ResourceLocation.fromNamespaceAndPath(AE2Aquarium.MODID, "aquarium_drop")));

    public static final DeferredHolder<RecipeSerializer<?>, AquariumDropRecipe.Serializer> AQUARIUM_DROP_SERIALIZER =
            SERIALIZERS.register("aquarium_drop", AquariumDropRecipe.Serializer::new);
}

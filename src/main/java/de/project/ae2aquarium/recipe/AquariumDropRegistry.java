package de.project.ae2aquarium.recipe;

import de.project.ae2aquarium.config.AquariumConfig;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseCoralPlantTypeBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CoralBlock;
import net.minecraft.world.level.block.SeaPickleBlock;
import net.minecraft.world.level.block.SpongeBlock;
import net.minecraft.world.level.block.WetSpongeBlock;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class AquariumDropRegistry {

    public static final TagKey<Item> CORAL_BLOCKS_TAG = TagKey.create(Registries.ITEM, ResourceLocation.withDefaultNamespace("coral_blocks"));
    public static final TagKey<Item> CORALS_TAG = TagKey.create(Registries.ITEM, ResourceLocation.withDefaultNamespace("corals"));

    private static final Map<Item, List<AquariumDropEntry>> BUILTIN_DROPS = new LinkedHashMap<>();
    private static final Map<Item, Integer> BUILTIN_MIN_VOLUMES = new LinkedHashMap<>();
    private static final Map<Item, List<AquariumDropEntry>> DYNAMIC_CACHE = new LinkedHashMap<>();
    private static final Map<Item, AquariumDropRecipe> RECIPE_CACHE = new LinkedHashMap<>();

    private static volatile boolean initialized = false;

    public record RolledDrop(ItemStack stack, boolean isSecondary) {
    }

    public static void clearCache() {
        DYNAMIC_CACHE.clear();
        RECIPE_CACHE.clear();
    }

    public static void refreshRecipeCache(RecipeManager recipeManager) {
        RECIPE_CACHE.clear();
        for (RecipeHolder<?> holder : recipeManager.getRecipes()) {
            if (holder.value() instanceof AquariumDropRecipe recipe) {
                for (ItemStack stack : recipe.specimen().getItems()) {
                    if (!stack.isEmpty()) {
                        RECIPE_CACHE.put(stack.getItem(), recipe);
                    }
                }
            }
        }
    }

    public static synchronized void ensureInitialized() {
        if (!initialized) {
            initialized = true;
            registerCoralFamily(
                    Items.TUBE_CORAL_BLOCK,
                    Items.TUBE_CORAL,
                    Items.TUBE_CORAL_FAN,
                    Items.DEAD_TUBE_CORAL_BLOCK,
                    Items.DEAD_TUBE_CORAL,
                    Items.DEAD_TUBE_CORAL_FAN
            );
            registerCoralFamily(
                    Items.BRAIN_CORAL_BLOCK,
                    Items.BRAIN_CORAL,
                    Items.BRAIN_CORAL_FAN,
                    Items.DEAD_BRAIN_CORAL_BLOCK,
                    Items.DEAD_BRAIN_CORAL,
                    Items.DEAD_BRAIN_CORAL_FAN
            );
            registerCoralFamily(
                    Items.BUBBLE_CORAL_BLOCK,
                    Items.BUBBLE_CORAL,
                    Items.BUBBLE_CORAL_FAN,
                    Items.DEAD_BUBBLE_CORAL_BLOCK,
                    Items.DEAD_BUBBLE_CORAL,
                    Items.DEAD_BUBBLE_CORAL_FAN
            );
            registerCoralFamily(
                    Items.FIRE_CORAL_BLOCK,
                    Items.FIRE_CORAL,
                    Items.FIRE_CORAL_FAN,
                    Items.DEAD_FIRE_CORAL_BLOCK,
                    Items.DEAD_FIRE_CORAL,
                    Items.DEAD_FIRE_CORAL_FAN
            );
            registerCoralFamily(
                    Items.HORN_CORAL_BLOCK,
                    Items.HORN_CORAL,
                    Items.HORN_CORAL_FAN,
                    Items.DEAD_HORN_CORAL_BLOCK,
                    Items.DEAD_HORN_CORAL,
                    Items.DEAD_HORN_CORAL_FAN
            );
            registerMarineDefaults();
        }
    }

    private static void registerCoralFamily(
            Item coralBlock,
            Item coralPlant,
            Item coralFan,
            Item deadCoralBlock,
            Item deadCoralPlant,
            Item deadCoralFan
    ) {
        // Coral Block as specimen -> primary is Coral Block
        addBuiltin(coralBlock, List.of(
                new AquariumDropEntry(new ItemStack(coralBlock), 55, 1, 1),
                new AquariumDropEntry(new ItemStack(coralPlant), 25, 1, 2),
                new AquariumDropEntry(new ItemStack(coralFan), 15, 1, 2),
                new AquariumDropEntry(new ItemStack(deadCoralBlock), 5, 1, 1)
        ));

        // Coral Plant as specimen -> primary is Coral Plant
        addBuiltin(coralPlant, List.of(
                new AquariumDropEntry(new ItemStack(coralPlant), 55, 1, 2),
                new AquariumDropEntry(new ItemStack(coralBlock), 25, 1, 1),
                new AquariumDropEntry(new ItemStack(coralFan), 15, 1, 2),
                new AquariumDropEntry(new ItemStack(deadCoralPlant), 5, 1, 1)
        ));

        // Coral Fan as specimen -> primary is Coral Fan
        addBuiltin(coralFan, List.of(
                new AquariumDropEntry(new ItemStack(coralFan), 55, 1, 2),
                new AquariumDropEntry(new ItemStack(coralBlock), 25, 1, 1),
                new AquariumDropEntry(new ItemStack(coralPlant), 15, 1, 2),
                new AquariumDropEntry(new ItemStack(deadCoralFan), 5, 1, 1)
        ));
    }

    private static void registerMarineDefaults() {
        // Sea Pickle
        addBuiltin(Items.SEA_PICKLE, List.of(
                new AquariumDropEntry(new ItemStack(Items.SEA_PICKLE), 75, 1, 2),
                new AquariumDropEntry(new ItemStack(Items.GLOW_INK_SAC), 15, 1, 1),
                new AquariumDropEntry(new ItemStack(Items.PRISMARINE_CRYSTALS), 10, 1, 1)
        ));

        // Kelp
        addBuiltin(Items.KELP, List.of(
                new AquariumDropEntry(new ItemStack(Items.KELP), 80, 1, 3),
                new AquariumDropEntry(new ItemStack(Items.SEAGRASS), 15, 1, 2),
                new AquariumDropEntry(new ItemStack(Items.BONE_MEAL), 5, 1, 1)
        ));

        // Seagrass
        addBuiltin(Items.SEAGRASS, List.of(
                new AquariumDropEntry(new ItemStack(Items.SEAGRASS), 80, 1, 3),
                new AquariumDropEntry(new ItemStack(Items.KELP), 15, 1, 2),
                new AquariumDropEntry(new ItemStack(Items.PRISMARINE_SHARD), 5, 1, 1)
        ));

        // Wet Sponge & Sponge
        addBuiltin(Items.WET_SPONGE, List.of(
                new AquariumDropEntry(new ItemStack(Items.WET_SPONGE), 70, 1, 1),
                new AquariumDropEntry(new ItemStack(Items.SPONGE), 20, 1, 1),
                new AquariumDropEntry(new ItemStack(Items.SEA_PICKLE), 10, 1, 1)
        ));
        addBuiltin(Items.SPONGE, List.of(
                new AquariumDropEntry(new ItemStack(Items.SPONGE), 70, 1, 1),
                new AquariumDropEntry(new ItemStack(Items.WET_SPONGE), 20, 1, 1),
                new AquariumDropEntry(new ItemStack(Items.SEA_PICKLE), 10, 1, 1)
        ));

        // Prismarine Shard & Crystals & Sea Lantern
        addBuiltin(Items.PRISMARINE_SHARD, List.of(
                new AquariumDropEntry(new ItemStack(Items.PRISMARINE_SHARD), 60, 1, 2),
                new AquariumDropEntry(new ItemStack(Items.PRISMARINE_CRYSTALS), 30, 1, 2),
                new AquariumDropEntry(new ItemStack(Items.NAUTILUS_SHELL), 5, 1, 1),
                new AquariumDropEntry(new ItemStack(Items.GLOW_INK_SAC), 5, 1, 1)
        ));
        addBuiltin(Items.PRISMARINE_CRYSTALS, List.of(
                new AquariumDropEntry(new ItemStack(Items.PRISMARINE_CRYSTALS), 65, 1, 2),
                new AquariumDropEntry(new ItemStack(Items.PRISMARINE_SHARD), 25, 1, 2),
                new AquariumDropEntry(new ItemStack(Items.SEA_LANTERN), 10, 1, 1)
        ));
        addBuiltin(Items.SEA_LANTERN, List.of(
                new AquariumDropEntry(new ItemStack(Items.SEA_LANTERN), 55, 1, 1),
                new AquariumDropEntry(new ItemStack(Items.PRISMARINE_CRYSTALS), 30, 1, 2),
                new AquariumDropEntry(new ItemStack(Items.PRISMARINE_SHARD), 15, 1, 2)
        ));

        // Nautilus Shell
        addBuiltin(Items.NAUTILUS_SHELL, List.of(
                new AquariumDropEntry(new ItemStack(Items.NAUTILUS_SHELL), 70, 1, 1),
                new AquariumDropEntry(new ItemStack(Items.PRISMARINE_CRYSTALS), 20, 1, 1),
                new AquariumDropEntry(new ItemStack(Items.GLOW_INK_SAC), 10, 1, 1)
        ));

        // Glow Ink Sac & Ink Sac
        addBuiltin(Items.GLOW_INK_SAC, List.of(
                new AquariumDropEntry(new ItemStack(Items.GLOW_INK_SAC), 75, 1, 2),
                new AquariumDropEntry(new ItemStack(Items.PRISMARINE_CRYSTALS), 15, 1, 1),
                new AquariumDropEntry(new ItemStack(Items.SEA_PICKLE), 10, 1, 1)
        ));
        addBuiltin(Items.INK_SAC, List.of(
                new AquariumDropEntry(new ItemStack(Items.INK_SAC), 80, 1, 2),
                new AquariumDropEntry(new ItemStack(Items.GLOW_INK_SAC), 20, 1, 1)
        ));

        // Lily Pad & Turtle Scute
        addBuiltin(Items.LILY_PAD, List.of(
                new AquariumDropEntry(new ItemStack(Items.LILY_PAD), 80, 1, 2),
                new AquariumDropEntry(new ItemStack(Items.SEAGRASS), 20, 1, 1)
        ));
        addBuiltin(Items.TURTLE_SCUTE, List.of(
                new AquariumDropEntry(new ItemStack(Items.TURTLE_SCUTE), 70, 1, 1),
                new AquariumDropEntry(new ItemStack(Items.SEAGRASS), 30, 1, 2)
        ));

        // Glow Lichen, Prismarine & Dark Prismarine
        addBuiltin(Items.GLOW_LICHEN, List.of(
                new AquariumDropEntry(new ItemStack(Items.GLOW_LICHEN), 80, 1, 2),
                new AquariumDropEntry(new ItemStack(Items.GLOW_INK_SAC), 20, 1, 1)
        ));
        addBuiltin(Items.PRISMARINE, List.of(
                new AquariumDropEntry(new ItemStack(Items.PRISMARINE), 65, 1, 2),
                new AquariumDropEntry(new ItemStack(Items.PRISMARINE_SHARD), 35, 1, 2)
        ));
        addBuiltin(Items.DARK_PRISMARINE, List.of(
                new AquariumDropEntry(new ItemStack(Items.DARK_PRISMARINE), 60, 1, 1),
                new AquariumDropEntry(new ItemStack(Items.PRISMARINE_SHARD), 25, 1, 2),
                new AquariumDropEntry(new ItemStack(Items.INK_SAC), 15, 1, 1)
        ));

        // Heart of the Sea (Requires minimum inner tank volume >= 27 m^3, e.g., outer 5x5x5)
        addBuiltin(Items.HEART_OF_THE_SEA, 27, List.of(
                new AquariumDropEntry(new ItemStack(Items.NAUTILUS_SHELL), 40, 1, 1),
                new AquariumDropEntry(new ItemStack(Items.PRISMARINE_CRYSTALS), 35, 1, 2),
                new AquariumDropEntry(new ItemStack(Items.PRISMARINE_SHARD), 20, 1, 2),
                new AquariumDropEntry(new ItemStack(Items.DIAMOND), 4, 1, 1),
                new AquariumDropEntry(new ItemStack(Items.HEART_OF_THE_SEA), 1, 1, 1)
        ));
    }

    private static void addBuiltin(Item specimen, List<AquariumDropEntry> entries) {
        addBuiltin(specimen, 1, entries);
    }

    private static void addBuiltin(Item specimen, int minVolume, List<AquariumDropEntry> entries) {
        if (specimen != null && specimen != Items.AIR) {
            BUILTIN_DROPS.put(specimen, entries);
            BUILTIN_MIN_VOLUMES.put(specimen, Math.max(1, minVolume));
        }
    }

    public static int getRequiredMinVolume(Item specimen, @Nullable Level level) {
        if (specimen == null || specimen == Items.AIR) {
            return 1;
        }
        ensureInitialized();
        if (level != null && RECIPE_CACHE.isEmpty()) {
            refreshRecipeCache(level.getRecipeManager());
        }
        AquariumDropRecipe recipe = RECIPE_CACHE.get(specimen);
        if (recipe != null) {
            return Math.max(1, recipe.minVolume());
        }
        return BUILTIN_MIN_VOLUMES.getOrDefault(specimen, 1);
    }

    public static boolean isValidSpecimen(Item item, @Nullable Level level) {
        if (item == null || item == Items.AIR) {
            return false;
        }
        ensureInitialized();
        if (RECIPE_CACHE.containsKey(item) || BUILTIN_DROPS.containsKey(item)) {
            return true;
        }
        if (level != null && RECIPE_CACHE.isEmpty()) {
            refreshRecipeCache(level.getRecipeManager());
            if (RECIPE_CACHE.containsKey(item)) {
                return true;
            }
        }
        boolean enableDynamic = true;
        try {
            if (AquariumConfig.SPEC.isLoaded()) {
                enableDynamic = AquariumConfig.ENABLE_DYNAMIC_FALLBACK.get();
            }
        } catch (Exception ignored) {
        }
        return enableDynamic && isDynamicMarineSpecimen(item);
    }

    private static boolean isDynamicMarineSpecimen(Item item) {
        ItemStack stack = new ItemStack(item);
        if (stack.is(CORAL_BLOCKS_TAG) || stack.is(CORALS_TAG)) {
            return true;
        }
        if (item instanceof BlockItem blockItem) {
            Block block = blockItem.getBlock();
            return block instanceof CoralBlock
                    || block instanceof BaseCoralPlantTypeBlock
                    || block instanceof SeaPickleBlock
                    || block instanceof SpongeBlock
                    || block instanceof WetSpongeBlock;
        }
        return false;
    }

    public static List<AquariumDropEntry> getDropEntries(Item specimen, @Nullable Level level, int innerVolume) {
        if (specimen == null || specimen == Items.AIR) {
            return Collections.emptyList();
        }
        ensureInitialized();

        if (level != null && RECIPE_CACHE.isEmpty()) {
            refreshRecipeCache(level.getRecipeManager());
        }

        AquariumDropRecipe recipe = RECIPE_CACHE.get(specimen);
        if (recipe != null) {
            if (innerVolume >= recipe.minVolume()) {
                return recipe.drops();
            }
            return Collections.emptyList();
        }

        List<AquariumDropEntry> builtin = BUILTIN_DROPS.get(specimen);
        if (builtin != null) {
            int minVol = BUILTIN_MIN_VOLUMES.getOrDefault(specimen, 1);
            if (innerVolume >= minVol) {
                return builtin;
            }
            return Collections.emptyList();
        }

        if ( isDynamicMarineSpecimen(specimen)) {
            return DYNAMIC_CACHE.computeIfAbsent(specimen, item -> List.of(
                    new AquariumDropEntry(new ItemStack(item), 85, 1, 2),
                    new AquariumDropEntry(new ItemStack(Items.SEA_PICKLE), 15, 1, 1)
            ));
        }

        return Collections.emptyList();
    }

    public static List<Item> getAllKnownSpecimens(@Nullable Level level) {
        ensureInitialized();
        if (level != null && RECIPE_CACHE.isEmpty()) {
            refreshRecipeCache(level.getRecipeManager());
        }
        List<Item> result = new ArrayList<>(BUILTIN_DROPS.keySet());
        for (Item item : RECIPE_CACHE.keySet()) {
            if (!result.contains(item)) {
                result.add(item);
            }
        }
        for (Item item : BuiltInRegistries.ITEM) {
            if (!result.contains(item) && isDynamicMarineSpecimen(item)) {
                result.add(item);
            }
        }
        return result;
    }

    public static RolledDrop rollDropWithIndex(List<AquariumDropEntry> entries, RandomSource random) {
        if (entries == null || entries.isEmpty()) {
            return new RolledDrop(ItemStack.EMPTY, false);
        }
        int totalWeight = 0;
        for (AquariumDropEntry entry : entries) {
            totalWeight += Math.max(1, entry.weight());
        }
        if (totalWeight <= 0) {
            return new RolledDrop(ItemStack.EMPTY, false);
        }

        int roll = random.nextInt(totalWeight);
        int cumulative = 0;
        for (int i = 0; i < entries.size(); i++) {
            AquariumDropEntry entry = entries.get(i);
            cumulative += Math.max(1, entry.weight());
            if (roll < cumulative) {
                int count = entry.rollCount(random);
                ItemStack stack = entry.item().copy();
                stack.setCount(count);
                return new RolledDrop(stack, i > 0);
            }
        }
        AquariumDropEntry fallback = entries.get(0);
        ItemStack stack = fallback.item().copy();
        stack.setCount(fallback.rollCount(random));
        return new RolledDrop(stack, false);
    }
}

package de.project.ae2aquarium.registry;

import de.project.ae2aquarium.AE2Aquarium;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(AE2Aquarium.MODID);

    public static final DeferredItem<BlockItem> AQUARIUM_CONTROLLER =
            ITEMS.registerSimpleBlockItem("aquarium_controller", ModBlocks.AQUARIUM_CONTROLLER);

    public static final DeferredItem<BlockItem> AQUARIUM_CASING =
            ITEMS.registerSimpleBlockItem("aquarium_casing", ModBlocks.AQUARIUM_CASING);

    public static final DeferredItem<BlockItem> AQUARIUM_GLASS =
            ITEMS.registerSimpleBlockItem("aquarium_glass", ModBlocks.AQUARIUM_GLASS);

    public static final DeferredItem<Item> VOID_SECONDARY_CARD =
            ITEMS.registerSimpleItem("void_secondary_card", new Item.Properties().stacksTo(64));
}

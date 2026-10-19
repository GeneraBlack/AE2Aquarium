package de.project.ae2aquarium;

import appeng.api.AECapabilities;
import appeng.api.upgrades.Upgrades;
import appeng.blockentity.AEBaseBlockEntity;
import appeng.core.definitions.AEItems;
import de.project.ae2aquarium.client.AE2AquariumClient;
import de.project.ae2aquarium.config.AquariumConfig;
import de.project.ae2aquarium.network.AquariumNetworking;
import de.project.ae2aquarium.recipe.AquariumDropRegistry;
import de.project.ae2aquarium.registry.ModBlockEntities;
import de.project.ae2aquarium.registry.ModBlocks;
import de.project.ae2aquarium.registry.ModCreativeTabs;
import de.project.ae2aquarium.registry.ModDataComponents;
import de.project.ae2aquarium.registry.ModItems;
import de.project.ae2aquarium.registry.ModMenus;
import de.project.ae2aquarium.registry.ModRecipes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.TagsUpdatedEvent;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

@Mod(AE2Aquarium.MODID)
public class AE2Aquarium {
    public static final String MODID = "ae2aquarium";
    public static final Logger LOGGER = LoggerFactory.getLogger(MODID);

    public AE2Aquarium(IEventBus modEventBus, ModContainer modContainer) {
        LOGGER.info("Initializing AE2 Aquarium");

        // Register Config
        modContainer.registerConfig(ModConfig.Type.COMMON, AquariumConfig.SPEC);

        // Register Registries
        ModBlocks.BLOCKS.register(modEventBus);
        ModBlockEntities.BLOCK_ENTITIES.register(modEventBus);
        ModItems.ITEMS.register(modEventBus);
        ModMenus.MENUS.register(modEventBus);
        ModCreativeTabs.CREATIVE_MODE_TABS.register(modEventBus);
        ModRecipes.SERIALIZERS.register(modEventBus);
        ModRecipes.RECIPE_TYPES.register(modEventBus);
        ModDataComponents.DATA_COMPONENTS.register(modEventBus);

        // Register Listeners
        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::registerCapabilities);
        modEventBus.addListener(AquariumNetworking::onRegisterPayloadHandlers);

        if (FMLEnvironment.dist.isClient()) {
            modEventBus.addListener(AE2AquariumClient::onRegisterMenuScreens);
            modEventBus.addListener(AE2AquariumClient::onRegisterRenderers);
            modEventBus.addListener(AE2AquariumClient::onClientSetup);
        }

        NeoForge.EVENT_BUS.addListener((TagsUpdatedEvent event) -> {
            AquariumDropRegistry.clearCache();
            var server = ServerLifecycleHooks.getCurrentServer();
            if (server != null) {
                AquariumDropRegistry.refreshRecipeCache(server.getRecipeManager());
                LOGGER.info("AE2 Aquarium: Refreshed recipe cache");
            }
        });
    }

    private void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                ModBlockEntities.AQUARIUM_CONTROLLER.get(),
                (be, side) -> be.getItemHandler(side)
        );
        event.registerBlockEntity(
                AECapabilities.IN_WORLD_GRID_NODE_HOST,
                ModBlockEntities.AQUARIUM_CONTROLLER.get(),
                (be, context) -> be
        );
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            AEBaseBlockEntity.registerBlockEntityItem(
                    ModBlockEntities.AQUARIUM_CONTROLLER.get(),
                    ModItems.AQUARIUM_CONTROLLER.get()
            );

            Upgrades.add(AEItems.SPEED_CARD.asItem(), ModBlocks.AQUARIUM_CONTROLLER.get(), 4);
            Upgrades.add(ModItems.VOID_SECONDARY_CARD.get(), ModBlocks.AQUARIUM_CONTROLLER.get(), 1);
            Upgrades.add(AEItems.VOID_CARD.asItem(), ModBlocks.AQUARIUM_CONTROLLER.get(), 1);

            for (String ns : List.of("ae2virtualgarden", "ae2virtualmine", "ae2virtualbattle", "ae2virtualwell")) {
                ResourceLocation id = ResourceLocation.fromNamespaceAndPath(ns, "void_secondary_card");
                if (BuiltInRegistries.ITEM.containsKey(id)) {
                    Item sisterCard = BuiltInRegistries.ITEM.get(id);
                    if (sisterCard != Items.AIR) {
                        Upgrades.add(sisterCard, ModBlocks.AQUARIUM_CONTROLLER.get(), 1);
                    }
                }
            }
        });
    }
}

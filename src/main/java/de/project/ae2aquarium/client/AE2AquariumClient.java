package de.project.ae2aquarium.client;

import de.project.ae2aquarium.client.gui.AquariumControllerScreen;
import de.project.ae2aquarium.client.render.AquariumBlockEntityRenderer;
import de.project.ae2aquarium.registry.ModBlockEntities;
import de.project.ae2aquarium.registry.ModBlocks;
import de.project.ae2aquarium.registry.ModMenus;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

public class AE2AquariumClient {

    public static void onRegisterMenuScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenus.AQUARIUM_CONTROLLER_MENU.get(), AquariumControllerScreen::new);
    }

    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ModBlockEntities.AQUARIUM_CONTROLLER.get(), AquariumBlockEntityRenderer::new);
    }

    @SuppressWarnings("deprecation")
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.AQUARIUM_GLASS.get(), RenderType.cutout());
        });
    }
}

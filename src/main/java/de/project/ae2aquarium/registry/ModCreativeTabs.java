package de.project.ae2aquarium.registry;

import de.project.ae2aquarium.AE2Aquarium;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, AE2Aquarium.MODID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> TAB =
            CREATIVE_MODE_TABS.register("ae2aquarium_tab", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.ae2aquarium"))
                    .icon(() -> new ItemStack(ModItems.AQUARIUM_CONTROLLER.get()))
                    .displayItems((parameters, output) -> {
                        output.accept(ModItems.AQUARIUM_CONTROLLER.get());
                        output.accept(ModItems.AQUARIUM_CASING.get());
                        output.accept(ModItems.AQUARIUM_GLASS.get());
                        output.accept(ModItems.VOID_SECONDARY_CARD.get());
                    })
                    .build());
}

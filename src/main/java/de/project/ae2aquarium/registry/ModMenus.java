package de.project.ae2aquarium.registry;

import de.project.ae2aquarium.AE2Aquarium;
import de.project.ae2aquarium.menu.AquariumControllerMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModMenus {
    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, AE2Aquarium.MODID);

    public static final DeferredHolder<MenuType<?>, MenuType<AquariumControllerMenu>> AQUARIUM_CONTROLLER_MENU =
            MENUS.register("aquarium_controller", () -> IMenuTypeExtension.create(AquariumControllerMenu::new));
}

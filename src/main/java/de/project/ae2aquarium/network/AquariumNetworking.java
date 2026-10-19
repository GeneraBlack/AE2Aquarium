package de.project.ae2aquarium.network;

import de.project.ae2aquarium.AE2Aquarium;
import de.project.ae2aquarium.menu.AquariumControllerMenu;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

public class AquariumNetworking {
    public static void onRegisterPayloadHandlers(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar(AE2Aquarium.MODID);
        registrar.playToServer(
                SetAquariumPartitionsPayload.TYPE,
                SetAquariumPartitionsPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> {
                    Player player = context.player();
                    if (player.containerMenu instanceof AquariumControllerMenu menu) {
                        menu.applyPartitions(player, payload.partitions());
                    }
                })
        );
    }
}

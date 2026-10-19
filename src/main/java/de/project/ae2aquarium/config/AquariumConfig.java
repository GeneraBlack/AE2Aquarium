package de.project.ae2aquarium.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public class AquariumConfig {
    public static final ModConfigSpec SPEC;

    public static final ModConfigSpec.IntValue BASE_TICK_INTERVAL;
    public static final ModConfigSpec.DoubleValue ENERGY_PER_DROP;
    public static final ModConfigSpec.BooleanValue REQUIRE_AE_ENERGY;
    public static final ModConfigSpec.BooleanValue ENABLE_DYNAMIC_FALLBACK;
    public static final ModConfigSpec.BooleanValue ENFORCE_INVENTORY_CHECK;

    public static final ModConfigSpec.IntValue MAX_TANK_WIDTH;
    public static final ModConfigSpec.IntValue MAX_TANK_HEIGHT;
    public static final ModConfigSpec.IntValue MAX_TANK_DEPTH;
    public static final ModConfigSpec.DoubleValue VOLUME_DROP_MULTIPLIER;
    public static final ModConfigSpec.IntValue MAX_DROPS_PER_CYCLE;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

        builder.comment("General AE2 Aquarium Settings").push("general");

        BASE_TICK_INTERVAL = builder
                .comment("Interval in world ticks between coral/marine drop generation cycles (20 ticks = 1 second, default 60 = 3 seconds)")
                .defineInRange("baseTickInterval", 60, 5, 72000);

        REQUIRE_AE_ENERGY = builder
                .comment("Whether generating drops requires AE energy from the ME Network")
                .define("requireAeEnergy", true);

        ENERGY_PER_DROP = builder
                .comment("AE energy consumed per generated item drop")
                .defineInRange("energyPerDrop", 12.0, 0.0, 100000.0);

        ENABLE_DYNAMIC_FALLBACK = builder
                .comment("Whether to automatically discover drops for unknown modded coral/aquatic blocks matching coral/marine tags.")
                .define("enableDynamicFallback", true);

        ENFORCE_INVENTORY_CHECK = builder
                .comment("When true, players can only configure a specimen in the controller if they currently possess it in their inventory.")
                .define("enforceInventoryCheck", false);

        builder.pop();

        builder.comment("Multiblock Scaling Settings").push("multiblock");

        MAX_TANK_WIDTH = builder
                .comment("Maximum outer width (X-axis) of the 3D Aquarium multiblock (minimum is always 3)")
                .defineInRange("maxTankWidth", 16, 3, 32);

        MAX_TANK_HEIGHT = builder
                .comment("Maximum outer height (Y-axis) of the 3D Aquarium multiblock (minimum is always 3)")
                .defineInRange("maxTankHeight", 8, 3, 16);

        MAX_TANK_DEPTH = builder
                .comment("Maximum outer depth (Z-axis) of the 3D Aquarium multiblock (minimum is always 3)")
                .defineInRange("maxTankDepth", 16, 3, 32);

        VOLUME_DROP_MULTIPLIER = builder
                .comment("Multiplier applied to the inner tank volume (in blocks^3) to determine generated drops per cycle")
                .defineInRange("volumeDropMultiplier", 1.0, 0.1, 64.0);

        MAX_DROPS_PER_CYCLE = builder
                .comment("Upper cap on drops per cycle for massive aquariums")
                .defineInRange("maxDropsPerCycle", 1024, 1, 16384);

        builder.pop();

        SPEC = builder.build();
    }
}

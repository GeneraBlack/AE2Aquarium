# 🐠 AE2 Aquarium

**Cultivate living coral reefs, coral blocks, and deep-ocean resources inside a scalable, fully 3D-animated Applied Energistics 2 Multiblock Aquarium!**

Requires **Applied Energistics 2** and **NeoForge (Minecraft 1.21.1)**.

---

### 🌟 What is AE2 Aquarium?

In vanilla Minecraft, **Coral Blocks** are non-renewable, and farming ocean resources often requires sprawling water structures that hurt server TPS and FPS. **AE2 Aquarium**—part of the **AE2 Virtual** series—solves this by introducing a sleek, panoramic **3D Multiblock Reef Aquarium** that integrates directly into your **Applied Energistics 2** ME Network!

Build a glass aquarium of any size from **3×3×3 up to 16×8×16**, place a standard **ME Item Storage Cell (`1k`–`256k`)** into the **ME Aquarium Tank Controller**, and configure up to **5 Coral or Marine Specimens**. Inside the slim panoramic glass frame, a living 3D reef ecosystem comes to life with animated tropical fish schools, swaying coral colonies, and rising bubbles—while passively cultivating coral blocks, fans, polyps, sponges, prismarine, and sea lanterns directly into the controller's internal storage cell!

---

### ✨ Key Features

* 🏛️ **Scalable 3D Panoramic Multiblock (`3×3×3` to `16×8×16`):**
  * Build custom aquariums tailored to your base architecture—from a compact **3×3×3** desktop nano-reef up to a monumental **16×8×16** centerpiece aquarium!
  * When formed, **Aquarium Structural Casings** automatically transform into **slim 4px base pedestals, 3px LED top canopies, and sleek corner pillars**, maximizing the crystal-clear panoramic glass view.
  * Generation rates scale directly with the **inner water volume** of your multiblock!
* 🎨 **Living 3D Reef & Animated Tropical Fish:**
  * Custom 3D BlockEntity Renderer displays a sandy seabed, translucent water volume, rising bubble particles, and **3D coral colonies** that dynamically reflect the exact specimens configured in your controller.
  * **Animated 3D tropical fish** swim smoothly along figure-8 paths with tail-fin animation and banking turns, scaling in number and size with your aquarium's volume.
* 💾 **Internal ME Storage Cell Slot (Zero Network Overflow!):**
  * Insert any standard **AE2 Item Storage Cell (`1k`, `4k`, `16k`, `64k`, or `256k`)** directly into the controller's dedicated **Storage Cell Slot**.
  * The controller mounts the cell onto your ME Network via `IStorageProvider` as **read/extract-only**: all cultivated resources are immediately available in ME Terminals, Export Buses, and Autocrafting, while normal network items can never pollute the aquarium cell.
  * Drops are generated **strictly inside the controller's cell** and **never** spill over into your external ME Drives!
  * Once the cell (or a specimen's allocated byte quota) is full, generation automatically pauses and consumes **zero AE power** for unproduced drops.
* 📊 **GParted-Style Habitat Partition Bar:**
  * Cultivate up to **5 different Coral or Marine Specimens** simultaneously in a single aquarium.
  * Use the interactive **Habitat Allocation Bar** (`-5%` / `+5%` and `Equalize`) to split both the **drop output** and the **storage cell byte capacity** across your active specimens.
  * If one coral reaches its allocated byte limit inside the storage cell, your other specimens keep producing smoothly up to their own limits!
* 🪸 **Complete Coral & Ocean Resource Support (Out of the Box):**
  * **All 5 Coral Families (Tube, Brain, Bubble, Fire, Horn):** Generates full **Coral Blocks** (60%), **Coral Plants** (16%), **Coral Fans** (14%), and **Dead Coral Blocks / Plants / Fans** (10%).
  * **Reef Flora & Ocean Blocks:** **Sea Pickles**, **Kelp**, **Seagrass**, **Glow Lichen**, **Prismarine**, **Dark Prismarine**, and **Sea Lanterns**.
  * **Rare Marine Treasures:** **Wet & Dry Sponges**, **Nautilus Shells**, **Turtle Scutes**, and **Heart of the Sea** (yields Nautilus Shells, Prismarine Crystals/Shards, Diamonds, and rare Hearts of the Sea).
  * **Automatic Modded Coral Detection:** Automatically discovers modded coral blocks, plants, and fans via standard `#minecraft:corals` and `#minecraft:coral_blocks` tags!
* ⚡ **Upgrade Cards (2 Upgrade Slots):**
  * **AE2 Acceleration Card (up to 2x):** Boosts drop output per cycle (`1 Card = 2x`, `2 Cards = 4x`).
  * **AE2 Inverter Card:** Converts coral cultivation into **100% Dead Coral Blocks, Dead Coral Plants, and Dead Coral Fans** for gray/stone building palettes.
  * **Void Secondary Card:** Automatically voids secondary byproducts (Dead Coral variants and Coral Fans) and saves their AE power cost so your cell stores only primary **Coral Blocks**.
* 🤝 **Full `AE2Virtual` Ecosystem Compatibility:**
  * Designed to run side-by-side with **AE2 Virtual Garden**, **AE2 Virtual Mine**, **AE2 Virtual Battle**, and **AE2 Virtual Well** without any conflicts.

---

### 📐 Multiblock Sizes & Production Rates

By default, generation cycles run every **60 ticks (3.0 seconds)** based on the interior volume $(W - 2) \times (H - 2) \times (D - 2)$:

* **3×3×3 Nano Tank (1 m³ interior):** **1 drop** every 3 seconds (up to **4 drops** with 2× Acceleration Cards)
* **5×4×5 Medium Tank (18 m³ interior):** **18 drops** every 3 seconds (up to **72 drops** with 2× Acceleration Cards)
* **7×5×7 Large Reef (75 m³ interior):** **75 drops** every 3 seconds (up to **300 drops** with 2× Acceleration Cards)
* **11×6×11 Grand Aquarium (324 m³ interior):** **324 drops** every 3 seconds (up to **1,024 drops** with 2× Acceleration Cards)
* **16×8×16 Leviathan Tank (1,176 m³ interior):** **1,024 drops** every 3 seconds

---

### 🔨 Crafting Recipes

#### 1. Aquarium Structural Casing (4x)
```
[ Iron Ingot   ] [ Quartz Glass ] [ Iron Ingot   ]
[ Quartz Glass ] [ Kelp         ] [ Quartz Glass ]
[ Iron Ingot   ] [ Copper Ingot ] [ Iron Ingot   ]
```

#### 2. Reinforced Aquarium Glass
* **Standard (4x):** Surround **1x Aquarium Structural Casing** + **1x Prismarine Crystals** with **7x AE2 Quartz Glass**.
* **Early-Game Alternative (2x):** Combine **Aquarium Structural Casing** + **Quartz Glass** + **Glow Ink Sac** + **Sea Pickle**.

#### 3. ME Aquarium Tank Controller
* **Standard:** Combine **4x Aquarium Structural Casings**, **2x Fluix Crystals**, **1x Engineering Processor**, **1x Logic Processor**, and **1x Heart of the Sea**.
* **Early-Game Alternative:** Uses **Calculation Processor + Water Bucket + Kelp** in place of the Heart of the Sea.

#### 4. Void Secondary Card (Shapeless)
* Combine **1x AE2 Basic Card** + **1x Cactus** + **1x Flint**.

---

### ⚙️ Configuration Options

All gameplay parameters are customizable in `config/ae2aquarium-common.toml`:
* **Generation Interval:** Adjust ticks between production cycles (default: `60` ticks / 3.0 seconds).
* **AE Power Cost:** Toggle power requirements or adjust AE energy per drop (default: `12.0` AE/drop).
* **Multiblock Limits:** Customize maximum width (`16`), height (`8`), depth (`16`), volume multiplier (`1.0`), and max drops per cycle (`1024`).
* **Datapack Extensible:** Add or customize marine drop tables via standard JSON recipes (`ae2aquarium:aquarium_drop`).

---

### ❓ Frequently Asked Questions (FAQ)

**Q: Can I use AE2 Aquarium in my modpack?**
> **Yes, absolutely!** You are welcome to include AE2 Aquarium in any public or private modpack on CurseForge, Modrinth, or custom launchers.

**Q: Will this mod flood my ME Drives with coral fans or dead coral?**
> **Never!** All drops are generated strictly into the standard AE2 Item Storage Cell placed inside the **ME Aquarium Tank Controller**. They are never pushed into external ME Drives. Furthermore, each specimen respects its `%` allocation bar inside that cell, and you can install a **Void Secondary Card** to keep only primary Coral Blocks.

**Q: How do I extract the cultivated corals for autocrafting or export?**
> Simply connect the **ME Aquarium Tank Controller** to your ME Network with any AE2 cable! Because the controller mounts its internal storage cell to the network as read/extract-only, your ME Terminals, Export Buses, and Molecular Assemblers can pull items from it automatically.

**Q: What happens if I break the controller or resize the aquarium?**
> Breaking the controller safely drops both the controller block and any installed **ME Storage Cell** and **Upgrade Cards** into the world. All multiblock blocks require a **Pickaxe** and drop themselves cleanly when mined.

---

### 📦 Dependencies

* **Minecraft 1.21.1**
* **NeoForge 21.1.172+**
* **Applied Energistics 2 (AE2) 19.2.10+**

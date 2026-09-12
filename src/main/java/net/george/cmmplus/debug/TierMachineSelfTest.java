package net.george.cmmplus.debug;

import com.simibubi.create.AllBlockEntityTypes;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.api.stress.BlockStressValues;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.belt.transport.TransportedItemStack;
import com.simibubi.create.content.kinetics.crusher.CrushingWheelControllerBlock;
import com.simibubi.create.content.kinetics.crusher.CrushingWheelControllerBlockEntity;
import com.simibubi.create.content.kinetics.motor.CreativeMotorBlockEntity;
import com.simibubi.create.content.logistics.depot.DepotBehaviour;
import com.simibubi.create.content.logistics.depot.DepotBlockEntity;
import com.simibubi.create.foundation.item.TooltipModifier;
import net.george.cmmplus.CMMPlus;
import net.george.cmmplus.CMMPlusConfig;
import com.simibubi.create.content.kinetics.belt.BeltBlock;
import com.simibubi.create.content.kinetics.belt.BeltPart;
import com.simibubi.create.content.kinetics.belt.BeltSlope;
import com.simibubi.create.content.kinetics.belt.BeltBlockEntity;
import net.george.cmmplus.CMMPlusTier;
import net.george.cmmplus.content.TieredBeltBlockEntity;
import net.george.cmmplus.content.TieredBeltInventory;
import net.george.cmmplus.content.TieredFanBlockEntity;
import net.george.cmmplus.content.TieredWheelBlockEntity;
import net.george.cmmplus.registration.ModBlockEntities;
import net.george.cmmplus.registration.ModBlocks;
import net.george.cmmplus.registration.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Opt-in functional test for the tier machines, enabled with {@code -Dcmmplus.selftest=true}
 * (Gradle: {@code ./gradlew runSelfTest}).  It builds complete machines next to the world spawn
 * and measures the real thing instead of trusting the code:
 *
 * <ul>
 *   <li>a fan rig per tier plus a vanilla fan rig, each blowing over a lit campfire onto a depot
 *       holding raw beef - the number of ticks a rig needs to turn it into a steak is that fan's
 *       processing speed;</li>
 *   <li>a crushing wheel rig per tier plus a vanilla pair, checking that the crushing controller is
 *       created and valid, that it is fed the tier's speed, and that an item really comes out
 *       crushed.</li>
 * </ul>
 *
 * The server stops itself once every rig reported, so the test runs unattended.
 */
@EventBusSubscriber(modid = CMMPlus.MOD_ID)
public final class TierMachineSelfTest {
    public static final String ENABLE_PROPERTY = "cmmplus.selftest";

    private static final int MOTOR_SPEED = 64;
    /** Create's {@code maxRotationSpeed} default - the fastest a network may run. */
    private static final int MAX_MOTOR_SPEED = 256;
    private static final int TIMEOUT_TICKS = 600;

    private static Run run;

    private TierMachineSelfTest() {
    }

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        if (!Boolean.getBoolean(ENABLE_PROPERTY) || run != null) {
            return;
        }
        CMMPlus.LOGGER.info("[self-test] building tier machine rigs");
        run = new Run(event.getServer());
        run.build();
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        if (run != null && run.tick()) {
            run = null;
        }
    }

    private static final class Run {
        private final MinecraftServer server;
        private final ServerLevel level;
        private final BlockPos origin;
        private final List<FanRig> fanRigs = new ArrayList<>();
        private final List<CrusherRig> crusherRigs = new ArrayList<>();
        private boolean beltChecked = false;
        private final List<String> failures = new ArrayList<>();
        private final List<String> stressReport = new ArrayList<>();
        private String legacyFan = "not tested";
        private String legacyWheel = "not tested";
        private boolean itemTooltips;
        private int ticks;

        Run(MinecraftServer server) {
            this.server = server;
            this.level = server.overworld();
            BlockPos spawn = level.getSharedSpawnPos();
            // Just above the surface right next to the spawn point, so the rigs are easy to find
            // when the same world is opened in single player.
            int y = level.getHeight(Heightmap.Types.WORLD_SURFACE, spawn.getX(), spawn.getZ()) + 4;
            this.origin = new BlockPos(spawn.getX() + 8, y, spawn.getZ() + 8);
        }

        void build() {
            checkBlockEntityValidity();
            checkLegacyUpgrade();
            checkItemTooltips();

            int row = 0;
            fanRigs.add(buildFanRig(row, null));
            for (CMMPlusTier tier : CMMPlusTier.values()) {
                fanRigs.add(buildFanRig(row += 6, tier));
            }

            int crusherRow = 0;
            crusherRigs.add(buildCrusherRig(16, crusherRow, null, MOTOR_SPEED));
            for (CMMPlusTier tier : CMMPlusTier.values()) {
                crusherRigs.add(buildCrusherRig(16, crusherRow += 6, tier, MOTOR_SPEED));
            }
            // Same machine at Create's maximum rotation speed, to measure what a top tier wheel
            // pair really asks for when it runs flat out.
            crusherRigs.add(buildCrusherRig(32, 0, CMMPlusTier.BEYOND, MAX_MOTOR_SPEED));

            buildBeltRig(-10, 0, CMMPlusTier.BRASS);
            buildBeltRig(-10, 8, CMMPlusTier.BEYOND);
        }

        /**
         * Minecraft asks a block entity type whether it accepts a block state on every chunk load,
         * so this is exactly the question that decides whether a tier machine keeps its block
         * entity: our own types must accept their block, and Create's types must still accept it
         * for worlds saved by builds that reused them - while remaining closed for anything else.
         */
        private void checkBlockEntityValidity() {
            BlockState fanState = ModBlocks.fan(CMMPlusTier.BRASS).get().defaultBlockState();
            BlockState wheelState = ModBlocks.wheel(CMMPlusTier.BRASS).get().defaultBlockState();

            if (!ModBlockEntities.fan(CMMPlusTier.BRASS).isValid(fanState)) {
                failures.add("cmmplus:encased_fan_brass does not accept its own block");
            }
            if (!ModBlockEntities.wheel(CMMPlusTier.BRASS).isValid(wheelState)) {
                failures.add("cmmplus:crushing_wheel_brass does not accept its own block");
            }
            if (!AllBlockEntityTypes.ENCASED_FAN.get().isValid(fanState)) {
                failures.add("legacy create:encased_fan id would be dropped on chunk load");
            }
            if (!AllBlockEntityTypes.CRUSHING_WHEEL.get().isValid(wheelState)) {
                failures.add("legacy create:crushing_wheel id would be dropped on chunk load");
            }
            if (AllBlockEntityTypes.MOTOR.get().isValid(fanState)) {
                failures.add("unrelated block entity types must not accept tier machine blocks");
            }
        }

        /**
         * Simulates exactly what chunk loading does with data written by an older build of this
         * mod: the stored id is Create's machine, the block is a tier machine.  The restored block
         * entity has to be the tier one, otherwise an already placed machine would keep working at
         * the vanilla tier.
         */
        private void checkLegacyUpgrade() {
            BlockState fanState = ModBlocks.fan(CMMPlusTier.BRASS).get().defaultBlockState();
            BlockState wheelState = ModBlocks.wheel(CMMPlusTier.BRASS).get().defaultBlockState();

            BlockEntity fan = loadLegacy(origin, fanState, "create:encased_fan");
            legacyFan = String.valueOf(fan == null ? null : fan.getClass().getSimpleName());
            if (!(fan instanceof TieredFanBlockEntity tieredFan) || tieredFan.getTier() != CMMPlusTier.BRASS) {
                failures.add("a saved create:encased_fan was not restored as a tier fan but as " + fan);
            }

            BlockEntity wheel = loadLegacy(origin, wheelState, "create:crushing_wheel");
            legacyWheel = String.valueOf(wheel == null ? null : wheel.getClass().getSimpleName());
            if (!(wheel instanceof TieredWheelBlockEntity tieredWheel) || tieredWheel.getTier() != CMMPlusTier.BRASS) {
                failures.add("a saved create:crushing_wheel was not restored as a tier wheel but as " + wheel);
            }
        }

        private BlockEntity loadLegacy(BlockPos pos, BlockState state, String legacyId) {
            CompoundTag tag = new CompoundTag();
            tag.putString("id", legacyId);
            tag.putInt("x", pos.getX());
            tag.putInt("y", pos.getY());
            tag.putInt("z", pos.getZ());
            return BlockEntity.loadStatic(pos, state, tag, level.registryAccess());
        }

        /**
         * Create More Machines gives its machines Create's kinetic item tooltip (the "Stress Impact"
         * block, numeric while wearing goggles); the tier machines register the same modifier, so
         * check the registry actually has one for every tier item.
         */
        private void checkItemTooltips() {
            for (CMMPlusTier tier : CMMPlusTier.values()) {
                for (Item item : new Item[]{ModItems.fan(tier), ModItems.wheel(tier)}) {
                    if (TooltipModifier.REGISTRY.get(item) == null) {
                        failures.add(item.getDescriptionId() + " has no kinetic stats item tooltip");
                    }
                }
            }
            itemTooltips = TooltipModifier.REGISTRY.get(ModItems.fan(CMMPlusTier.BRASS)) != null;
        }

        private FanRig buildFanRig(int dz, CMMPlusTier tier) {
            BlockPos motorPos = origin.offset(0, 0, dz);
            BlockPos fanPos = motorPos.east();
            BlockPos firePos = fanPos.east();
            BlockPos depotPos = firePos.east();
            String label = tier == null ? "vanilla" : tier.id;

            Block fan = tier == null ? AllBlocks.ENCASED_FAN.get() : ModBlocks.fan(tier).get();
            level.setBlock(motorPos, withFacing(AllBlocks.CREATIVE_MOTOR.get(), Direction.EAST), 3);
            level.setBlock(fanPos, withFacing(fan, Direction.EAST), 3);
            level.setBlock(firePos, Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, true), 3);
            level.setBlock(depotPos, AllBlocks.DEPOT.get().defaultBlockState(), 3);

            if (level.getBlockEntity(motorPos) instanceof CreativeMotorBlockEntity motor) {
                motor.generatedSpeed.setValue(MOTOR_SPEED);
            }

            if (tier == null) {
                if (level.getBlockEntity(fanPos) == null
                        || level.getBlockEntity(fanPos).getType() != AllBlockEntityTypes.ENCASED_FAN.get()) {
                    failures.add(label + " fan: unexpected block entity " + level.getBlockEntity(fanPos));
                }
            } else if (!(level.getBlockEntity(fanPos) instanceof TieredFanBlockEntity)) {
                failures.add(label + " fan: block entity is " + level.getBlockEntity(fanPos)
                        + " instead of TieredFanBlockEntity");
            }

            DepotBehaviour depot = level.getBlockEntity(depotPos) instanceof DepotBlockEntity depotBE
                    ? depotBE.getBehaviour(DepotBehaviour.TYPE)
                    : null;
            if (depot == null) {
                failures.add(label + " fan: no depot behaviour at " + depotPos);
            } else {
                depot.setHeldItem(new TransportedItemStack(new ItemStack(Items.BEEF)));
            }
            return new FanRig(label, tier, depot, fanPos);
        }

        private CrusherRig buildCrusherRig(int dx, int dz, CMMPlusTier tier, int motorSpeed) {
            BlockPos wheelPosA = origin.offset(dx, 0, dz);
            BlockPos wheelPosB = wheelPosA.east(2);
            BlockPos controllerPos = wheelPosA.east();
            String label = tier == null ? "vanilla" : tier.id;
            if (motorSpeed != MOTOR_SPEED) {
                label += "@" + motorSpeed;
            }

            Block wheel = tier == null ? AllBlocks.CRUSHING_WHEEL.get() : ModBlocks.wheel(tier).get();
            level.setBlock(wheelPosA, withAxis(wheel, Direction.Axis.Z), 3);
            level.setBlock(wheelPosB, withAxis(wheel, Direction.Axis.Z), 3);
            // Motors on opposite sides of the pair, so the two wheels turn against each other and
            // Create considers the pair valid.
            level.setBlock(wheelPosA.north(), withFacing(AllBlocks.CREATIVE_MOTOR.get(), Direction.SOUTH), 3);
            level.setBlock(wheelPosB.south(), withFacing(AllBlocks.CREATIVE_MOTOR.get(), Direction.NORTH), 3);
            for (BlockPos motorPos : new BlockPos[]{wheelPosA.north(), wheelPosB.south()}) {
                if (level.getBlockEntity(motorPos) instanceof CreativeMotorBlockEntity motor) {
                    motor.generatedSpeed.setValue(motorSpeed);
                }
            }

            if (tier != null && !(level.getBlockEntity(wheelPosA) instanceof TieredWheelBlockEntity)) {
                failures.add(label + " wheels: block entity is " + level.getBlockEntity(wheelPosA)
                        + " instead of TieredWheelBlockEntity");
            }
            return new CrusherRig(label, tier, level, wheelPosA, controllerPos, motorSpeed);
        }

        /**
         * A four block straight belt with a creative motor on the pulley side.  Put in place at
         * build time (the kinetic network needs a tick or two to form before it can be read).
         */
        private void buildBeltRig(int dx, int dz, CMMPlusTier tier) {
            Block belt = ModBlocks.belt(tier).get();
            BlockPos start = origin.offset(dx, 0, dz);
            for (int i = 0; i < BELT_RIG_LENGTH; i++) {
                BeltPart part = i == 0 ? BeltPart.START
                        : i == BELT_RIG_LENGTH - 1 ? BeltPart.END : BeltPart.MIDDLE;
                level.setBlock(start.offset(i, 0, 0), belt.defaultBlockState()
                        .setValue(BeltBlock.SLOPE, BeltSlope.HORIZONTAL)
                        .setValue(BeltBlock.PART, part)
                        .setValue(BeltBlock.CASING, false)
                        .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.EAST)
                        .setValue(BlockStateProperties.WATERLOGGED, false), 3);
            }

            // Facing EAST makes the belt's shaft axis Z, so the motor goes north of a pulley block
            // and points south into it.
            BlockPos motorPos = start.north();
            level.setBlock(motorPos, withFacing(AllBlocks.CREATIVE_MOTOR.get(), Direction.SOUTH), 3);
            if (level.getBlockEntity(motorPos) instanceof CreativeMotorBlockEntity motor) {
                motor.generatedSpeed.setValue(MOTOR_SPEED);
            }

            // Casing is applied here, at build time: Create rebuilds belt states while the belt
            // initialises, so applying it in the same tick it is asserted would race that.
            if (level.getBlockEntity(start) instanceof BeltBlockEntity beltBE) {
                beltBE.setCasingType(BeltBlockEntity.CasingType.ANDESITE);
                CMMPlus.LOGGER.info("[self-test] {} casing right after set: state={} field={}",
                        tier.id, level.getBlockState(start).getValue(BeltBlock.CASING), beltBE.casing);
            }
        }

        /**
         * The tier belts' own assertions.  These are the ones that would silently regress if the
         * Registrate mixin, the block entity registration or the load accounting broke.
         */
        private void verifyBeltRigs() {
            for (int i = 0; i < 2; i++) {
                CMMPlusTier tier = i == 0 ? CMMPlusTier.BRASS : CMMPlusTier.BEYOND;
                int dz = i == 0 ? 0 : 8;
                BlockPos start = origin.offset(-10, 0, dz);
                String label = "belt " + tier.id;

                if (!(level.getBlockEntity(start) instanceof TieredBeltBlockEntity be)) {
                    failures.add(label + ": block entity is " + level.getBlockEntity(start)
                            + " instead of TieredBeltBlockEntity (Registrate mixin or BE type broken)");
                    continue;
                }

                if (be.beltLength != BELT_RIG_LENGTH) {
                    failures.add(label + ": beltLength is " + be.beltLength + ", expected " + BELT_RIG_LENGTH);
                }
                if (!be.isController()) {
                    failures.add(label + ": first block is not the belt controller");
                }

                float expectImpact = BELT_RIG_LENGTH * tier.beltStressFactor;
                if (Math.abs(be.calculateStressApplied() - expectImpact) > 0.001F) {
                    failures.add(label + ": stress impact is " + be.calculateStressApplied()
                            + ", expected length x factor = " + expectImpact);
                }

                if (be.hasNetwork()) {
                    float expectStress = MOTOR_SPEED * expectImpact;
                    float actual = be.getOrCreateNetwork().calculateStress();
                    if (Math.abs(actual - expectStress) > 0.5F) {
                        failures.add(label + ": network stress is " + actual + ", expected rpm x length x factor = "
                                + expectStress);
                    } else {
                        CMMPlus.LOGGER.info("[self-test] " + label + ": network stress " + actual + " SU at " + MOTOR_SPEED
                                + " rpm over " + BELT_RIG_LENGTH + " blocks (factor " + tier.beltStressFactor + ")");
                    }
                } else {
                    failures.add(label + ": belt did not join a kinetic network (pulley/shaft connection)");
                }

                if (!(be.getInventory() instanceof TieredBeltInventory inventory)) {
                    failures.add(label + ": inventory is not a TieredBeltInventory");
                } else {
                    int expectSlots = tier.beltCapacity / 64;
                    if (inventory.slotsPerBlock() != expectSlots) {
                        failures.add(label + ": " + inventory.slotsPerBlock() + " slots per block, expected " + expectSlots);
                    }

                    var handler = be.segmentItemHandler();
                    if (handler == null || handler.getSlots() != expectSlots) {
                        failures.add(label + ": segment item handler exposes "
                                + (handler == null ? "nothing" : handler.getSlots()) + " slots, expected " + expectSlots);
                    } else {
                        handler.insertItem(0, new ItemStack(Items.DIRT, 8), false);
                        ItemStack second = handler.insertItem(0, new ItemStack(Items.STONE, 8), false);
                        boolean mixed = second.isEmpty();
                        if (mixed != tier.mixedBeltLoad()) {
                            failures.add(label + ": inserting a second item type "
                                    + (mixed ? "was accepted" : "was refused") + " but mixed load is "
                                    + tier.mixedBeltLoad());
                        } else {
                            CMMPlus.LOGGER.info("[self-test] " + label + ": mixed item types in one block "
                                    + (mixed ? "accepted (Beyond only)" : "refused"));
                        }
                    }
                }

                if (!level.getBlockState(start).getValue(BeltBlock.CASING)) {
                    failures.add(label + ": casing state did not turn on (BE casing field = " + be.casing + ")");
                } else {
                    CMMPlus.LOGGER.info("[self-test] " + label + ": casing applied");
                }
            }
        }
        private static final int BELT_RIG_LENGTH = 4;

        private static BlockState withFacing(Block block, Direction facing) {
            return block.defaultBlockState().setValue(BlockStateProperties.FACING, facing);
        }

        private static BlockState withAxis(Block block, Direction.Axis axis) {
            return block.defaultBlockState().setValue(BlockStateProperties.AXIS, axis);
        }

        /** @return true when the whole run is over */
        boolean tick() {
            ticks++;
            for (FanRig rig : fanRigs) {
                rig.tick(ticks);
            }
            for (CrusherRig rig : crusherRigs) {
                rig.tick(ticks);
            }

            if (!beltChecked && ticks > 40) {
                beltChecked = true;
                verifyBeltRigs();
            }

            boolean allDone = beltChecked && fanRigs.stream().allMatch(rig -> rig.done)
                    && crusherRigs.stream().allMatch(rig -> rig.done);
            if (!allDone && ticks < TIMEOUT_TICKS) {
                return false;
            }

            if (!allDone) {
                failures.add("timed out after " + TIMEOUT_TICKS + " ticks");
                fanRigs.stream().filter(rig -> !rig.done)
                        .forEach(rig -> failures.add(rig.label + " fan: never processed its item"));
                crusherRigs.stream().filter(rig -> !rig.done)
                        .forEach(rig -> failures.add(rig.label + " wheels: never crushed its item"));
            }

            verify();
            report();
            server.halt(false);
            return true;
        }

        private void verify() {
            FanRig vanilla = fanRigs.get(0);
            for (FanRig rig : fanRigs) {
                if (rig.tier == null || !rig.done || !vanilla.done) {
                    continue;
                }
                int expected = vanilla.cookedAtTicks / rig.tier.processingMultiplier;
                if (rig.cookedAtTicks > expected * 1.5 + 5) {
                    failures.add(rig.label + " fan processed in " + rig.cookedAtTicks
                            + " ticks, expected about " + expected + " (vanilla took " + vanilla.cookedAtTicks + ")");
                }
            }

            for (CrusherRig rig : crusherRigs) {
                float multiplier = rig.tier == null ? 1.0f : rig.tier.processingMultiplier;
                float expected = rig.motorSpeed * multiplier / 50.0f;
                if (!rig.controllerExists || !rig.controllerValid) {
                    failures.add(rig.label + " wheels: controller missing or invalid (exists="
                            + rig.controllerExists + ", valid=" + rig.controllerValid + ")");
                } else if (Math.abs(rig.crushingSpeed - expected) > expected * 0.05f) {
                    failures.add(rig.label + " wheels: crushing speed " + rig.crushingSpeed
                            + " but expected " + expected);
                }
                if (Math.abs(rig.networkSpeed) != rig.motorSpeed) {
                    failures.add(rig.label + " wheels: network speed " + rig.networkSpeed
                            + " but the motor provides " + rig.motorSpeed);
                }
            }

            checkStressImpacts();
        }

        /**
         * What the placed machines actually ask the network for.  Create resolves stress per block
         * through {@code BlockStressValues}, and {@code KineticBlockEntity#calculateStressApplied}
         * is what feeds the network's numbers, so check the value the machines themselves report -
         * and that it follows the tier, like Create More Machines' per-tier machine impacts do.
         */
        private void checkStressImpacts() {
            for (FanRig rig : fanRigs) {
                double expected = rig.tier == null
                        ? BlockStressValues.getImpact(AllBlocks.ENCASED_FAN.get())
                        : CMMPlusConfig.fanImpact(rig.tier);
                if (!(level.getBlockEntity(rig.fanPos) instanceof KineticBlockEntity fan)) {
                    failures.add(rig.label + " fan: no kinetic block entity to measure");
                    continue;
                }
                float actual = fan.calculateStressApplied();
                // What the network really takes: impact x the speed it runs at.
                float drawn = fan.hasNetwork() ? fan.getOrCreateNetwork().getActualStressOf(fan) : Float.NaN;
                stressReport.add(String.format(Locale.ROOT, "%-9s fan   %6.1f SU/rpm at %4.1f rpm  ->  %8.1f SU",
                        rig.label, actual, fan.getTheoreticalSpeed(), drawn));
                if (Math.abs(actual - expected) > 1.0e-4) {
                    failures.add(rig.label + " fan reports " + actual + " SU/rpm but " + expected + " is configured");
                }
                if (Math.abs(drawn - expected * Math.abs(fan.getTheoreticalSpeed())) > 1.0e-3) {
                    failures.add(rig.label + " fan draws " + drawn + " SU from its network, expected "
                            + (expected * Math.abs(fan.getTheoreticalSpeed())));
                }
            }

            for (CrusherRig rig : crusherRigs) {
                double expected = rig.tier == null
                        ? BlockStressValues.getImpact(AllBlocks.CRUSHING_WHEEL.get())
                        : CMMPlusConfig.wheelImpact(rig.tier);
                if (!(level.getBlockEntity(rig.wheelPosA) instanceof KineticBlockEntity wheel)) {
                    failures.add(rig.label + " wheels: no kinetic block entity to measure");
                    continue;
                }
                float actual = wheel.calculateStressApplied();
                float drawn = wheel.hasNetwork() ? wheel.getOrCreateNetwork().getActualStressOf(wheel) : Float.NaN;
                // What the motor driving this rig can supply, so the numbers can be compared.
                float provided = level.getBlockEntity(rig.wheelPosA.north()) instanceof KineticBlockEntity motor
                        && motor.hasNetwork()
                        ? motor.getOrCreateNetwork().getActualCapacityOf(motor)
                        : Float.NaN;
                stressReport.add(String.format(Locale.ROOT,
                        "%-11s wheel %6.1f SU/rpm at %5.1f rpm  ->  %9.1f SU per wheel   (motor supplies %.0f SU)",
                        rig.label, actual, wheel.getTheoreticalSpeed(), drawn, provided));
                if (Math.abs(actual - expected) > 1.0e-4) {
                    failures.add(rig.label + " wheel reports " + actual + " SU/rpm but " + expected
                            + " is configured");
                }
                if (Math.abs(drawn - expected * Math.abs(wheel.getTheoreticalSpeed())) > 1.0e-3) {
                    failures.add(rig.label + " wheel draws " + drawn + " SU from its network, expected "
                            + (expected * Math.abs(wheel.getTheoreticalSpeed())));
                }
            }
        }

        private void report() {
            CMMPlus.LOGGER.info("[self-test] ===== results after {} ticks (motor {} rpm) =====", ticks, MOTOR_SPEED);
            CMMPlus.LOGGER.info("[self-test] legacy save ids restored: create:encased_fan -> {}, create:crushing_wheel -> {}",
                    legacyFan, legacyWheel);
            CMMPlus.LOGGER.info("[self-test] kinetic stress tooltip registered on the tier items: {}", itemTooltips);
            for (FanRig rig : fanRigs) {
                int multiplier = rig.tier == null ? 1 : rig.tier.processingMultiplier;
                CMMPlus.LOGGER.info("[self-test] {} fan: {} ticks per raw beef -> steak (tier x{})",
                        rig.label, rig.cookedAtTicks, multiplier);
            }
            for (CrusherRig rig : crusherRigs) {
                int multiplier = rig.tier == null ? 1 : rig.tier.processingMultiplier;
                CMMPlus.LOGGER.info(
                        "[self-test] {} wheels: controller={} valid={} crushingspeed={} networkSpeed={} result={} (tier x{})",
                        rig.label, rig.controllerExists, rig.controllerValid, fmt(rig.crushingSpeed),
                        fmt(rig.networkSpeed), rig.output, multiplier);
            }
            stressReport.forEach(line -> CMMPlus.LOGGER.info("[self-test] stress: {}", line));
            if (failures.isEmpty()) {
                CMMPlus.LOGGER.info("[self-test] RESULT: PASS");
            } else {
                failures.forEach(failure -> CMMPlus.LOGGER.error("[self-test] FAILURE: {}", failure));
                CMMPlus.LOGGER.error("[self-test] RESULT: FAIL ({} problem(s))", failures.size());
            }
        }

        private static String fmt(float value) {
            return String.format(Locale.ROOT, "%.2f", value);
        }
    }

    private static final class FanRig {
        private final String label;
        private final CMMPlusTier tier;
        private final DepotBehaviour depot;
        private final BlockPos fanPos;
        private int cookedAtTicks = -1;
        private boolean done;

        FanRig(String label, CMMPlusTier tier, DepotBehaviour depot, BlockPos fanPos) {
            this.label = label;
            this.tier = tier;
            this.depot = depot;
            this.fanPos = fanPos;
        }

        void tick(int ticks) {
            if (done || depot == null) {
                return;
            }
            if (depot.getHeldItemStack().is(Items.COOKED_BEEF)) {
                cookedAtTicks = ticks;
                done = true;
            }
        }
    }

    private static final class CrusherRig {
        private final String label;
        private final CMMPlusTier tier;
        private final ServerLevel level;
        private final BlockPos wheelPosA;
        private final BlockPos controllerPos;
        private final int motorSpeed;
        private boolean controllerExists;
        private boolean controllerValid;
        private float crushingSpeed;
        private float networkSpeed;
        private String output = "nothing inserted";
        private boolean inserted;
        private boolean done;

        CrusherRig(String label, CMMPlusTier tier, ServerLevel level, BlockPos wheelPosA, BlockPos controllerPos,
                   int motorSpeed) {
            this.label = label;
            this.tier = tier;
            this.level = level;
            this.wheelPosA = wheelPosA;
            this.controllerPos = controllerPos;
            this.motorSpeed = motorSpeed;
        }

        void tick(int ticks) {
            if (done) {
                return;
            }

            if (level.getBlockEntity(wheelPosA) instanceof KineticBlockEntity wheel) {
                networkSpeed = wheel.getTheoreticalSpeed();
            }

            BlockState controllerState = level.getBlockState(controllerPos);
            controllerExists = AllBlocks.CRUSHING_WHEEL_CONTROLLER.has(controllerState);
            controllerValid = controllerExists && controllerState.getValue(CrushingWheelControllerBlock.VALID);

            if (!(level.getBlockEntity(controllerPos) instanceof CrushingWheelControllerBlockEntity controller)) {
                return;
            }
            crushingSpeed = controller.crushingspeed;

            if (!inserted && controllerValid && crushingSpeed > 0) {
                controller.inventory.setStackInSlot(0, new ItemStack(Items.COPPER_ORE));
                controller.inventory.remainingTime = 40;
                controller.inventory.appliedRecipe = false;
                output = "inserted ore";
                inserted = true;
                return;
            }

            if (inserted && controller.inventory.getStackInSlot(0).isEmpty()) {
                output = "ore crushed away";
                done = true;
                return;
            }
            if (inserted && ticks > 500) {
                output = "ore still in the crusher after 500 ticks";
                done = true;
            }
        }
    }
}

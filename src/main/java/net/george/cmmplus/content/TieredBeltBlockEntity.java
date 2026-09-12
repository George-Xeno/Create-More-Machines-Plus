package net.george.cmmplus.content;

import com.simibubi.create.content.kinetics.KineticNetwork;
import com.simibubi.create.content.kinetics.belt.BeltBlock;
import com.simibubi.create.content.kinetics.belt.BeltBlockEntity;
import com.simibubi.create.content.kinetics.belt.transport.BeltInventory;
import com.simibubi.create.content.kinetics.belt.transport.BeltMovementHandler;
import net.george.cmmplus.CMMPlusTier;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;

import java.util.HashMap;
import java.util.Map;

/**
 * A tiered belt's block entity.
 *
 * <p>The belt runs at <b>Create's own speed</b> - nothing here touches {@code getSpeed()} or the
 * belt movement speed.  What the tier changes is:
 *
 * <ul>
 *   <li><b>stress</b>: {@code rpm x length x tier.beltStressFactor}, see
 *       {@link #calculateStressApplied()}.  Create multiplies whatever this returns by the
 *       network's rotation speed, so returning {@code length x factor} is exactly the spec.
 *       Only the belt's controller draws it, the other segments return 0 like Create's do.
 *       The network learns the value from Create's own {@code attachKinetics} call inside
 *       {@code BeltBlock.initBelt}; {@link #tick()} only corrects it when the network is
 *       genuinely holding a different number.</li>
 *   <li><b>capacity</b>: how many items one belt block carries, and (Beyond only) whether a
 *       single block may hold several different item types at once.</li>
 * </ul>
 */
public class TieredBeltBlockEntity extends BeltBlockEntity {
    private final CMMPlusTier tier;
    /**
     * Last belt length this controller re-checked its network entry for (see {@link #tick()}).
     * A plain runtime field on purpose: it guards "the belt's length changed while it was alive",
     * not anything that has to survive a save.
     */
    private int lastKnownLength = -1;

    /**
     * Create's {@code BeltBlockEntity.passengers} holds every living entity the belt is currently
     * carrying, and it is <b>the</b> gate on entity transport: {@code BeltBlock.entityInside} adds
     * an entity to that map only {@code if (controller != null && controller.passengers != null)},
     * and {@code BeltBlockEntity.tick} runs {@code BeltMovementHandler.transportEntity} only for
     * entries that are already in it.
     *
     * <p>Create leaves the field {@code null} and allocates it lazily - but only inside the
     * {@code if (this.getSpeed() != 0.0F)} branch of its tick.  A belt whose speed is not yet
     * established (a belt that was just connected, or one whose network is momentarily overstressed,
     * where {@code KineticBlockEntity.getSpeed()} reports 0) therefore has a null map, every
     * {@code entityInside} call hits that early return, and no entity is ever registered - not even
     * after the belt starts turning, because registration only ever happens on a collision and the
     * map is still null when the collision comes.  Transport then appears to "switch on" only once
     * something else has made the belt tick with a non-zero speed first, which is exactly the
     * reported "entities move only after an item has ridden the belt" behaviour: item transport
     * goes through {@code BeltInventory.tick} and never touches this map.
     *
     * <p>The map is therefore initialised up front.  {@code null} is not a meaningful state for it
     * (Create's own tick treats {@code null} and "empty" identically), so pre-allocating it cannot
     * change any behaviour other than letting the first collision register its entity.
     */
    @SuppressWarnings("unused")
    protected Map<Entity, BeltMovementHandler.TransportedEntityInfo> passengers = new HashMap<>();

    public TieredBeltBlockEntity(CMMPlusTier tier, BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        this.tier = tier;
        this.color = java.util.Optional.of(tier.dyeColor());
    }

    public CMMPlusTier tier() {
        return tier;
    }

    /**
     * Create's belts draw no stress ({@code CStress.setNoImpact()}); a tiered belt is charged
     * {@code rpm x length x factor}.  Returning 0 for every non-controller segment mirrors
     * Create's own belt so a run of N blocks is only charged once per belt, not N times.
     */
    @Override
    public float calculateStressApplied() {
        if (!isController()) {
            return 0.0F;
        }

        float impact = Math.max(1, beltLength) * (float) tier.beltStressFactor;
        lastStressApplied = impact;
        return impact;
    }

    @Override
    public BeltInventory getInventory() {
        if (!isController()) {
            return super.getInventory();
        }

        if (inventory == null) {
            inventory = new TieredBeltInventory(this, tier);
        }

        return inventory;
    }

    /**
     * The item capability for one belt segment.  Create registers this capability only for its own
     * belt block entity type, so tiered belts have to expose it themselves; the handler is our own
     * multi-slot one (see {@link TieredBeltSegmentHandler}).
     */
    public IItemHandler segmentItemHandler() {
        if (!BeltBlock.canTransportObjects(getBlockState())) {
            return null;
        }

        if (!isRemoved() && itemHandler == null) {
            initializeItemHandler();
        }

        return itemHandler;
    }

    @Override
    protected void initializeItemHandler() {
        // NOTE: unlike Create's belt we also build the handler on the client.  Jade's generic item
        // provider decides whether to ask for a container by looking the item capability up on the
        // CLIENT level; Create's own belt returns null there, which is why no belt shows its
        // contents in Jade at all.  The riding item is synced, so a client-side handler is accurate
        // for slot 0 and simply empty for the hidden slots.
        if (level == null || itemHandler != null || beltLength == 0
                || controller == null || !level.isLoaded(controller)) {
            return;
        }

        if (level.getBlockEntity(controller) instanceof TieredBeltBlockEntity controllerBE
                && controllerBE.getInventory() instanceof TieredBeltInventory tieredInventory) {
            itemHandler = new TieredBeltSegmentHandler(tieredInventory, index);
            invalidateCapabilities();
        }
    }

    /**
     * Restores the {@code CASING} property Create's chain rebuild can drop, and - only when the
     * network is actually holding a wrong stress value for this belt - re-checks the belt's stress
     * entry.
     *
     * <p><b>Why the stress side is written the way it is.</b>  A belt's stress depends on its length
     * ({@link #calculateStressApplied()}), so the network has to be told when Create's
     * {@code BeltBlock.initBelt} rebuilds the chain.  Create does tell it: {@code initBelt} sets
     * {@code beltLength} on every segment and then calls {@code attachKinetics()}
     * ({@code BeltBlock.java:428-431}), which joins the segment to its network through
     * {@code KineticBlockEntity.setNetwork} → {@code KineticNetwork.add}, and {@code add} stores
     * {@code be.calculateStressApplied()} - our value, computed from the final length - in the
     * network's {@code members} map, marking the network {@code networkDirty} so the next
     * {@code KineticBlockEntity.tick} runs {@code updateNetwork()} and re-syncs everything.
     *
     * <p>What must <b>not</b> happen is an unconditional push from here.  {@code updateStressFor}
     * and {@code updateStress} do not just record a number: {@code KineticNetwork.updateStress()}
     * recomputes the network's totals and hands them to <b>every</b> member through
     * {@code KineticBlockEntity.updateFromNetwork}, which derives each member's {@code overStressed}
     * from {@code capacity < stress} ({@code KineticBlockEntity.java:136-149}) - and
     * {@code getSpeed()} returns 0 for any member that reads as overstressed
     * ({@code KineticBlockEntity.java:277-279}), which stops belts, items and entities.  Forcing
     * that whole-network sync from a belt tick, at a moment of the belt's own choosing, is what made
     * a healthy network report "应力过载 / Overstressed" right when a belt was connected.
     *
     * <p>So the value is only pushed when the network's stored entry genuinely differs from what
     * this belt should cost, and only once per length change.  On the normal path the comparison
     * fails - Create already stored the right number - and the network is left untouched; after a
     * load, or when this belt's length changed while it stayed in the network, the entry is
     * corrected exactly once.
     */
    @Override
    public void tick() {
        super.tick();

        // A casing can be applied while the belt is still initialising, and Create rebuilds the
        // belt's states during that, which drops the CASING property setCasingType just wrote.
        // The block entity field is the authority (it is what the casing models key off), so put
        // the state back when the two disagree.
        if (casing != CasingType.NONE && !getBlockState().getValue(BeltBlock.CASING)) {
            level.setBlock(worldPosition, getBlockState().setValue(BeltBlock.CASING, true), 2);
        }

        if (level == null || level.isClientSide || !isController()) {
            return;
        }

        if (beltLength <= 0 || beltLength == lastKnownLength) {
            return;
        }

        lastKnownLength = beltLength;

        float expected = calculateStressApplied();
        if (!hasNetwork()) {
            return;
        }

        KineticNetwork network = getOrCreateNetwork();
        Float stored = network.members.get(this);
        if (stored == null || stored == expected) {
            return;
        }

        network.updateStressFor(this, expected);
        network.updateStress();
    }
}

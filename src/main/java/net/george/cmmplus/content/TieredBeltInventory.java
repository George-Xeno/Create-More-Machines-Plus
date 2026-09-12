package net.george.cmmplus.content;

import com.simibubi.create.content.kinetics.belt.BeltBlockEntity;
import com.simibubi.create.content.kinetics.belt.transport.BeltInventory;
import com.simibubi.create.content.kinetics.belt.transport.TransportedItemStack;
import net.george.cmmplus.CMMPlusTier;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * A tiered belt's inventory: Create's belt transport plus a per-block load.
 *
 * <p>Create's belt keeps exactly one {@code TransportedItemStack} per belt block - the spacing
 * between two items is a hardcoded 1.0 belt position ({@code BeltInventory#tick}) - and a single
 * {@code ItemStack} can never hold more than 99 items (1.21.1's {@code ItemStack.CODEC} is
 * {@code intRange(1, 99)}, anything larger throws when the chunk is saved).  A tier belt therefore
 * cannot carry "128 items in one stack"; what it carries is <b>several 64-position slots per
 * block</b>, which is the same thing expressed in the unit the tier spec uses:
 *
 * <pre>
 *   brass      2 slots  = 128 positions
 *   netherite  4 slots  = 256 positions
 *   end        8 slots  = 512 positions
 *   beyond    16 slots  = 1024 positions
 * </pre>
 *
 * <p>One position holds one item that stacks to 64; an item that stacks to 16 costs 4 positions,
 * an unstackable item costs 64 (see {@link CMMPlusTier#beltPositionsFor}).  Slot 0 of a block is
 * the item that visibly rides the belt, the other slots ride along invisibly with it - they are
 * real inventory however: the block exposes them through its item handler, so funnels, hoppers
 * and pipes can fill and empty them.
 *
 * <p>Every tier may only hold <b>one item type</b> per block; Beyond is the only tier whose block
 * accepts several types at once, sharing its 1024 positions between them.
 */
public class TieredBeltInventory extends BeltInventory {
    private static final String LOADS = "CmmplusBeltLoads";

    private final CMMPlusTier tier;
    private final BeltBlockEntity beltEntity;
    /** Loads of items sharing a belt block, keyed by the stack that carries them along the belt. */
    private final Map<TransportedItemStack, NonNullList<ItemStack>> loads = new IdentityHashMap<>();
    /** Stacks Create has queued but not yet moved into the belt; their load must exist already. */
    private final Map<Integer, TransportedItemStack> pendingCarriers = new java.util.HashMap<>();

    public TieredBeltInventory(BeltBlockEntity be, CMMPlusTier tier) {
        super(be);
        this.beltEntity = be;
        this.tier = tier;
    }

    /** Whether this block's load (including a queued one) may take one more item of this type. */
    public boolean canAbsorbAt(int segment, ItemStack stack) {
        NonNullList<ItemStack> load = loadAt(segment);
        return load != null && isUniform(load, stack) && positionsUsed(load) < tier.beltCapacity;
    }

    /** The stack Create has queued for this block but not moved onto the belt yet. */
    public TransportedItemStack pendingCarrierAt(int segment) {
        return pendingCarriers.get(segment);
    }

    public CMMPlusTier tier() {
        return tier;
    }

    /** How many 64-position slots one belt block of this tier has. */
    public int slotsPerBlock() {
        return tier.beltCapacity / 64;
    }

    private NonNullList<ItemStack> loadOf(TransportedItemStack carrier) {
        return loads.computeIfAbsent(carrier, s -> {
            NonNullList<ItemStack> list = NonNullList.withSize(slotsPerBlock(), ItemStack.EMPTY);
            list.set(0, s.stack);
            return list;
        });
    }

    /** The load carried by the belt block that currently holds {@code carrier}, or null. */
    public NonNullList<ItemStack> loadFor(TransportedItemStack carrier) {
        return loads.get(carrier);
    }

    /** Finds the block's load by the block it currently occupies. */
    /** Finds the block's load by the block it currently occupies (including queued stacks). */
    public NonNullList<ItemStack> loadAt(int segment) {
        TransportedItemStack carrier = getStackAtOffset(segment);
        if (carrier == null) {
            carrier = pendingCarriers.get(segment);
        }
        return carrier == null ? null : loads.get(carrier);
    }

    private static int positionsOf(ItemStack stack) {
        return stack.isEmpty() ? 0 : stack.getCount() * CMMPlusTier.beltPositionsFor(stack);
    }

    private static int positionsUsed(NonNullList<ItemStack> load) {
        int used = 0;
        for (ItemStack stack : load) {
            used += positionsOf(stack);
        }
        return used;
    }

    private boolean isUniform(NonNullList<ItemStack> load, ItemStack candidate) {
        if (tier.mixedBeltLoad()) {
            return true;
        }

        for (ItemStack stack : load) {
            if (!stack.isEmpty() && !ItemStack.isSameItemSameComponents(stack, candidate)) {
                return false;
            }
        }

        return true;
    }

    /**
     * How many items of {@code stack} one load slot holds: a slot is 64 positions, so an item
     * costing {@code positions} each fills its slot at {@code 64 / positions} items.  This is the
     * slot's <b>capacity</b>, and a slot already holding that many must not be grown any further.
     */
    private static int itemsPerSlot(ItemStack stack) {
        return 64 / Math.max(1, CMMPlusTier.beltPositionsFor(stack));
    }

    /**
     * Tries to put {@code source} into a block's load.  Returns the number of items that did not
     * fit (never a partial stack loss: either it is absorbed here or the caller falls back to
     * Create's normal behaviour of giving it its own belt position).
     */
    private int absorbInto(NonNullList<ItemStack> load, ItemStack source) {
        int room = tier.beltCapacity - positionsUsed(load);
        if (room <= 0 || !isUniform(load, source)) {
            return source.getCount();
        }

        int perSlot = itemsPerSlot(source);
        int remaining = source.getCount();
        for (int i = 0; i < load.size() && remaining > 0; i++) {
            ItemStack slot = load.get(i);
            if (slot.isEmpty()) {
                int fit = Math.min(remaining, perSlot);
                load.set(i, source.copyWithCount(fit));
                remaining -= fit;
            } else if (ItemStack.isSameItemSameComponents(slot, source)) {
                // Never fill a slot past its own 64 positions.  Slot 0 *is* the stack that rides the
                // belt, so growing it regardless of what it already held turned a legitimate
                // "insert 64 more into this block" into a 128 count on a 64-stack item - a count
                // ItemStack.CODEC (intRange(1, 99)) refuses to serialize.
                int fit = Math.min(remaining, perSlot - slot.getCount());
                if (fit <= 0) {
                    continue;
                }

                slot.grow(fit);
                remaining -= fit;
            }
        }

        return remaining;
    }

    /** Item handler entry point: fills the block's load, returns the remainder. */
    public ItemStack insertIntoLoad(TransportedItemStack carrier, ItemStack source, boolean simulate) {
        NonNullList<ItemStack> load = loadOf(carrier);

        if (simulate) {
            NonNullList<ItemStack> copy = NonNullList.withSize(load.size(), ItemStack.EMPTY);
            for (int i = 0; i < load.size(); i++) {
                copy.set(i, load.get(i).copy());
            }
            return source.copyWithCount(absorbInto(copy, source));
        }

        int remaining = absorbInto(load, source);
        if (remaining != source.getCount()) {
            beltEntity.setChanged();
            beltEntity.sendData();
        }

        return source.copyWithCount(remaining);
    }

    /** Item handler entry point: takes items out of the block's load. */
    public ItemStack extractFromLoad(TransportedItemStack carrier, int amount, boolean simulate) {
        NonNullList<ItemStack> load = loads.get(carrier);
        if (load == null || amount <= 0) {
            return ItemStack.EMPTY;
        }

        for (int i = load.size() - 1; i >= 0; i--) {          // take from the back first
            ItemStack slot = load.get(i);
            if (slot.isEmpty()) {
                continue;
            }

            ItemStack extracted = simulate ? slot.copyWithCount(Math.min(amount, slot.getCount()))
                    : slot.split(Math.min(amount, slot.getCount()));
            if (!simulate) {
                beltEntity.setChanged();
                beltEntity.sendData();
            }

            return extracted;
        }

        return ItemStack.EMPTY;
    }

    @Override
    public void addItem(TransportedItemStack newStack) {
        int segment = Math.max(0, (int) newStack.beltPosition);
        TransportedItemStack carrier = getStackAtOffset(segment);
        if (carrier != null && carrier != newStack) {
            int remaining = absorbInto(loadOf(carrier), newStack.stack);
            if (remaining <= 0) {
                return;                       // fully absorbed into the block's load
            }
            newStack.stack = newStack.stack.copyWithCount(remaining);
        }

        pendingCarriers.put(segment, newStack);
        loadOf(newStack);
        super.addItem(newStack);
    }

    /**
     * Create blocks a second insertion into the same belt block from the same side.  A tier belt
     * must accept it when the block still has room, which is exactly how a block accumulates up to
     * its 128/256/512/1024 positions instead of pushing every stack one block further.
     */
    @Override
    public boolean canInsertAtFromSide(int segment, Direction side) {
        if (super.canInsertAtFromSide(segment, side)) {
            return true;
        }

        NonNullList<ItemStack> load = loadAt(segment);
        return load != null && positionsUsed(load) < tier.beltCapacity;
    }

    /** Keeps a load's slot 0 in sync with the stack that actually rides the belt. */
    @Override
    public void tick() {
        super.tick();

        loads.keySet().removeIf(carrier -> !getTransportedItems().contains(carrier) && carrier != pendingCarriers.get((int) carrier.beltPosition));
        pendingCarriers.values().removeIf(carrier -> getTransportedItems().contains(carrier));
        loads.forEach((carrier, load) -> {
            if (!carrier.stack.isEmpty()) {
                load.set(0, carrier.stack);
                return;
            }

            // The riding stack is gone: a copy may still sit in slot 0 (written when the block had
            // no carrier).  Never let the next tick wipe it - move it into a free hidden slot so it
            // keeps being saved and riding along.
            ItemStack orphan = load.get(0);
            if (!orphan.isEmpty()) {
                for (int i = 1; i < load.size(); i++) {
                    if (load.get(i).isEmpty()) {
                        load.set(i, orphan);
                        break;
                    }
                }
                load.set(0, ItemStack.EMPTY);
            }
        });
    }

    @Override
    public CompoundTag write(HolderLookup.Provider registries) {
        clampToSaveableCounts();
        CompoundTag tag = super.write(registries);
        List<TransportedItemStack> items = getTransportedItems();
        ListTag loadsTag = new ListTag();

        for (int index = 0; index < items.size(); index++) {
            NonNullList<ItemStack> load = loads.get(items.get(index));
            if (load == null) {
                continue;
            }

            CompoundTag entry = new CompoundTag();
            entry.putInt("Index", index);
            ListTag stacks = new ListTag();
            for (int slot = 1; slot < load.size(); slot++) {     // slot 0 is the riding stack itself
                ItemStack stack = load.get(slot);
                if (!stack.isEmpty()) {
                    stacks.add(stack.save(registries));
                }
            }
            if (stacks.isEmpty()) {
                continue;
            }

            entry.put("Stacks", stacks);
            loadsTag.add(entry);
        }

        tag.put(LOADS, loadsTag);
        return tag;
    }

    /**
     * The largest count 1.21.1 can serialize: {@code ItemStack.CODEC} encodes the count as
     * {@code intRange(1, 99)} and throws out of {@code ItemStack.save} for anything larger.
     */
    private static int saveableCount(ItemStack stack) {
        return Math.min(99, Math.max(1, stack.getMaxStackSize()));
    }

    /**
     * Last line of defence against a stack the item codec cannot encode.  A count above the item's
     * own stack size is never legitimate, and left alone it throws from {@code ItemStack.save} the
     * next time this belt is serialized - for a chunk save or for the client update packet, which
     * is what took the server down.  The excess is moved into the block's hidden load slots while
     * they have room and dropped otherwise; either way the riding stack is left saveable.
     */
    private void clampToSaveableCounts() {
        for (TransportedItemStack carried : getTransportedItems()) {
            ItemStack riding = carried.stack;
            if (riding.isEmpty()) {
                continue;
            }

            int limit = saveableCount(riding);
            if (riding.getCount() <= limit) {
                continue;
            }

            NonNullList<ItemStack> load = loads.get(carried);
            if (load != null) {
                spillIntoHiddenSlots(load, riding, limit, riding.getCount() - limit);
            }

            riding.setCount(limit);
        }
    }

    /**
     * Moves up to {@code amount} items of {@code source} into a load's hidden slots (1 and up),
     * honouring both a slot's own capacity and the tier's, with the riding stack counted at
     * {@code ridingCount} (what it will hold once clamped).
     */
    private void spillIntoHiddenSlots(NonNullList<ItemStack> load, ItemStack source, int ridingCount, int amount) {
        int cost = Math.max(1, CMMPlusTier.beltPositionsFor(source));
        int used = ridingCount * cost;
        for (int i = 1; i < load.size(); i++) {
            used += positionsOf(load.get(i));
        }

        int perSlot = itemsPerSlot(source);
        int remaining = Math.min(amount, Math.max(0, (tier.beltCapacity - used) / cost));
        for (int i = 1; i < load.size() && remaining > 0; i++) {
            ItemStack slot = load.get(i);
            if (slot.isEmpty()) {
                int fit = Math.min(remaining, perSlot);
                load.set(i, source.copyWithCount(fit));
                remaining -= fit;
            } else if (ItemStack.isSameItemSameComponents(slot, source)) {
                int fit = Math.min(remaining, perSlot - slot.getCount());
                if (fit <= 0) {
                    continue;
                }

                slot.grow(fit);
                remaining -= fit;
            }
        }
    }

    @Override
    public void read(CompoundTag nbt, HolderLookup.Provider registries) {
        super.read(nbt, registries);
        loads.clear();
        loads.keySet().removeIf(carrier -> true);

        List<TransportedItemStack> items = getTransportedItems();
        ListTag loadsTag = nbt.getList(LOADS, Tag.TAG_COMPOUND);

        for (int i = 0; i < loadsTag.size(); i++) {
            CompoundTag entry = loadsTag.getCompound(i);
            int index = entry.getInt("Index");
            if (index < 0 || index >= items.size()) {
                continue;
            }

            NonNullList<ItemStack> load = loadOf(items.get(index));
            ListTag stacks = entry.getList("Stacks", Tag.TAG_COMPOUND);
            for (int slot = 0; slot < stacks.size() && slot + 1 < load.size(); slot++) {
                load.set(slot + 1, ItemStack.parseOptional(registries, stacks.getCompound(slot)));
            }
        }
    }
}

package net.george.cmmplus.content;

import com.simibubi.create.content.kinetics.belt.transport.TransportedItemStack;
import com.simibubi.create.foundation.item.ItemHelper;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;

/**
 * The item handler of one belt block of a tiered belt.
 *
 * <p>Slot 0 is the item that visibly rides the belt; the remaining slots are the rest of the
 * block's load, which travels with it (see {@link TieredBeltInventory}).  This is what makes the
 * per-block counts real: a brass belt block exposes 2 slots (128 positions), a netherite 4, an end
 * 8 and a Beyond 16 - and only Beyond may hold more than one item type at a time.
 *
 * <p>Create's own {@code ItemHandlerBeltSegment} only ever exposes a single slot, which is why
 * tiered belts need their own.
 */
public class TieredBeltSegmentHandler implements IItemHandler {
    private final TieredBeltInventory inventory;
    private final int segment;

    public TieredBeltSegmentHandler(TieredBeltInventory inventory, int segment) {
        this.inventory = inventory;
        this.segment = segment;
    }

    @Override
    public int getSlots() {
        return inventory.slotsPerBlock();
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        if (slot < 0 || slot >= getSlots()) {
            return ItemStack.EMPTY;
        }

        TransportedItemStack carrier = inventory.getStackAtOffset(segment);
        if (carrier == null) {
            return ItemStack.EMPTY;
        }

        NonNullList<ItemStack> load = inventory.loadFor(carrier);
        if (load == null) {
            return slot == 0 ? carrier.stack : ItemStack.EMPTY;
        }

        return slot < load.size() ? load.get(slot) : ItemStack.EMPTY;
    }

    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        if (stack.isEmpty()) {
            return ItemStack.EMPTY;
        }

        TransportedItemStack carrier = inventory.getStackAtOffset(segment);
        if (carrier == null && inventory.loadAt(segment) != null) {
            // The block already holds a load, or Create still has a stack queued for it (addItem
            // only queues; the belt moves it in on its next tick).  Either way this item has to
            // obey the block's rules before a second stack is put on the belt - which is what
            // keeps a non-Beyond belt single-type while a Beyond belt shares its positions.
            if (!inventory.canAbsorbAt(segment, stack)) {
                return stack;
            }

            TransportedItemStack pending = inventory.pendingCarrierAt(segment);
            if (pending != null) {
                return inventory.insertIntoLoad(pending, stack, simulate);
            }
        }

        if (carrier != null) {
            return inventory.insertIntoLoad(carrier, stack, simulate);
        }

        // No load in this block yet: put a new stack on the belt, positioned exactly like Create's
        // own belt segment handler does (BeltBlockEntity's block is 1.0 belt positions wide).
        if (!inventory.canInsertAtFromSide(segment, Direction.UP)) {
            return stack;
        }

        ItemStack remainder = ItemHelper.limitCountToMaxStackSize(stack, simulate);
        if (!simulate) {
            TransportedItemStack newStack = new TransportedItemStack(stack.copyWithCount(stack.getCount() - remainder.getCount()));
            newStack.insertedAt = segment;
            newStack.insertedFrom = Direction.UP;
            newStack.beltPosition = (float) segment + 0.5F;
            newStack.prevBeltPosition = newStack.beltPosition;
            inventory.addItem(newStack);
        }

        return remainder;
    }

    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        if (amount <= 0 || slot < 0 || slot >= getSlots()) {
            return ItemStack.EMPTY;
        }

        TransportedItemStack carrier = inventory.getStackAtOffset(segment);
        if (carrier == null) {
            return ItemStack.EMPTY;
        }

        if (slot == 0) {
            // the riding stack itself, taken exactly like Create's belt segment handler does
            int taken = Math.min(amount, carrier.stack.getCount());
            if (taken <= 0) {
                return ItemStack.EMPTY;
            }

            ItemStack extracted = simulate ? carrier.stack.copyWithCount(taken) : carrier.stack.split(taken);
            if (!simulate && carrier.stack.isEmpty()) {
                inventory.getTransportedItems().remove(carrier);
            }

            return extracted;
        }

        NonNullList<ItemStack> load = inventory.loadFor(carrier);
        if (load == null || slot >= load.size()) {
            return ItemStack.EMPTY;
        }

        ItemStack inSlot = load.get(slot);
        int taken = Math.min(amount, inSlot.getCount());
        if (taken <= 0) {
            return ItemStack.EMPTY;
        }

        return simulate ? inSlot.copyWithCount(taken) : inSlot.split(taken);
    }

    @Override
    public int getSlotLimit(int slot) {
        return 64;
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }

        if (inventory.tier().mixedBeltLoad()) {
            return true;
        }

        TransportedItemStack carrier = inventory.getStackAtOffset(segment);
        if (carrier == null) {
            return true;
        }

        NonNullList<ItemStack> load = inventory.loadFor(carrier);
        if (load == null) {
            return true;
        }

        for (ItemStack present : load) {
            if (!present.isEmpty() && !ItemStack.isSameItemSameComponents(present, stack)) {
                return false;
            }
        }

        return true;
    }
}

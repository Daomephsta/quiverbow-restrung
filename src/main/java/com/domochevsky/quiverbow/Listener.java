package com.domochevsky.quiverbow;

import com.domochevsky.quiverbow.weapons.ERA;
import com.domochevsky.quiverbow.weapons.base.WeaponBase;

import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;

public class Listener
{
	@SubscribeEvent
	public void ItemCraftedEvent(PlayerEvent.ItemCraftedEvent event)
	{
		// System.out.println("[EVENT] Player crafted something.");

		if (!event.crafting.isEmpty() && event.crafting.getItem() instanceof ERA)
		{
			ItemStack stack = event.craftMatrix.getStackInSlot(1);

			if (!stack.isEmpty() && stack.getCount() > 1)
			{
				stack.shrink(26);
				if (stack.getCount() <= 0)
				{
					event.craftMatrix.setInventorySlotContents(1, ItemStack.EMPTY);
				} // Nothing left
			}
			// else, nothing in there or only a single rail, meaning this is a
			// repairing event. I'm fine with that
		}
		// else, not mine, so don't care

		else if (!event.crafting.isEmpty() && event.crafting.getItem() instanceof WeaponBase) // More
		// generic
		// weapon
		// check
		{
			this.copyName(event.craftMatrix, event.crafting);
		}
	}

	private void copyName(IInventory craftMatrix, ItemStack newItem) // Does the
	// weapon
	// have a
	// custom
	// name? If
	// so then
	// we're
	// transfering
	// that to
	// the new
	// item
	{
		// Step 1, find the actual item (It's possible that this is not a
		// reloading action, meaning there is no weapon to copy the name from)

		int slot = 0;

		while (slot < 9)
		{
			ItemStack stack = craftMatrix.getStackInSlot(slot);

			if (!stack.isEmpty() && stack.getItem() instanceof WeaponBase) // Found
			// it.
			// Does
			// it
			// have
			// a
			// name
			// tag?
			{
				if (stack.hasDisplayName() && !newItem.hasDisplayName())
				{
					newItem.setStackDisplayName(stack.getDisplayName());
				}
				// else, has no custom display name or the new item already has
				// one. Fine with me either way.

				return; // Either way, we're done here
			}
			// else, either doesn't exist or not what I'm looking for

			slot += 1;
		}
	}
}

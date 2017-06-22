package com.domochevsky.quiverbow.recipes;

import com.domochevsky.quiverbow.miscitems.PackedUpAA;

import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.ShapedRecipes;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;

public class Recipe_AA_Weapon extends ShapedRecipes implements IRecipe
{
    private ItemStack result;

    public Recipe_AA_Weapon(int sizeX, int sizeY, ItemStack[] components, ItemStack result)
    {
	super(sizeX, sizeY, components, result);

	this.result = result;
    }

    @Override
    public boolean matches(InventoryCrafting matrix, World world) // Returns
								  // true if
								  // these
								  // components
								  // are what
								  // I'm looking
								  // for to make
								  // my item
    {
	return RecipeHelper.doesRecipeMatch(this.recipeItems, matrix, world);
    }

    @Override
    public ItemStack getCraftingResult(InventoryCrafting matrix)
    {
	ItemStack stack = this.result.copy();
	ItemStack previousAA = this.getAAFromMatrix(matrix);

	if (!previousAA.isEmpty() && previousAA.hasTagCompound()) // Copying
							       // existing
							       // properties
	{
	    stack.setTagCompound((NBTTagCompound) previousAA.getTagCompound().copy());
	}
	else // ...or just applying new ones
	{
	    stack.setTagCompound(new NBTTagCompound());
	}

	// Apply the new upgrade now
	stack.getTagCompound().setBoolean("hasWeaponUpgrade", true);

	if (stack.getTagCompound().getInteger("currentHealth") == 0) // Just
								     // making
								     // sure
								     // it's not
								     // shown
								     // with 0
								     // health
	{
	    if (stack.getTagCompound().getBoolean("hasArmorUpgrade"))
	    {
		stack.getTagCompound().setInteger("currentHealth", 40);
	    }
	    else
	    {
		stack.getTagCompound().setInteger("currentHealth", 20);
	    } // Fresh turret, so setting some health
	}

	return stack;
    }

    private ItemStack getAAFromMatrix(InventoryCrafting matrix)
    {
	int counter = 0;

	while (counter < matrix.getSizeInventory())
	{
	    if (!matrix.getStackInSlot(counter).isEmpty()
		    && matrix.getStackInSlot(counter).getItem() instanceof PackedUpAA)
	    {
		return matrix.getStackInSlot(counter); // Found it
	    }

	    counter += 1;
	}

	return ItemStack.EMPTY;
    }
}

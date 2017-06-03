package com.domochevsky.quiverbow.ammo;

import com.domochevsky.quiverbow.util.InventoryHelper;
import com.domochevsky.quiverbow.util.Utils;

import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;

public class RedstoneMagazine extends AmmoMagazine
{
    public RedstoneMagazine()
    {
	super(1, 8);
	this.setMaxDamage(64); // Filled with gold nuggets (8 shots with 9
			       // scatter, 24 with 3 scatter)
	this.setCreativeTab(CreativeTabs.COMBAT); // On the combat tab by
						  // default, since this is
						  // amunition
    }

    @Override
    public String getIconPath()
    {
	return "RedstoneAmmo";
    }

    @Override
    public void addRecipes()
    {
	GameRegistry.addRecipe(new ItemStack(this, 1, this.getMaxDamage()), "x x", "x x", "xgx", 'x', Items.IRON_INGOT,
		'g', Items.REDSTONE);
    }

    @Override
    protected boolean hasComponentItems(EntityPlayer player, int amount)
    {
	return InventoryHelper.hasItem(player, Items.REDSTONE, amount);
    }

    @Override
    protected boolean consumeComponentItems(EntityPlayer player, int amount)
    {
	Utils.playSoundAtEntityPos(player, SoundEvents.BLOCK_WOOD_BUTTON_CLICK_ON, 0.5F, 0.3F);
	return InventoryHelper.consumeItem(player, Items.REDSTONE, amount);
    }
}

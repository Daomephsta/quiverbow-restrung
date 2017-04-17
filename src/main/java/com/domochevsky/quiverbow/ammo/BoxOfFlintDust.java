package com.domochevsky.quiverbow.ammo;

import cpw.mods.fml.common.registry.GameRegistry;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;

public class BoxOfFlintDust extends _AmmoBase
{	
	public BoxOfFlintDust()
	{
		this.setMaxDamage(16);
		this.setCreativeTab(CreativeTabs.tabTools);
		
		this.setHasSubtypes(true);
	}
	
	
	@Override
	public String getIconPath() { return "Bundle_Flint"; }
	
	@Override
	public void addRecipes() 
	{ 
		// A box of flint dust (4 dust per flint, meaning 32 per box), merged with wooden planks
        GameRegistry.addShapelessRecipe(new ItemStack(this),
                Items.flint,
                Items.flint,
                Items.flint,
                Items.flint,
                Items.flint,
                Items.flint,
                Items.flint,
                Items.flint,
                Blocks.planks
        ); 
	}
}
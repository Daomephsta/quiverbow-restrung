package com.domochevsky.quiverbow.weapons;

import com.domochevsky.quiverbow.Helper;
import com.domochevsky.quiverbow.Main;
import com.domochevsky.quiverbow.ammo.ArrowBundle;
import com.domochevsky.quiverbow.net.NetHelper;
import com.domochevsky.quiverbow.projectiles.Sabot_Arrow;
import com.domochevsky.quiverbow.recipes.RecipeLoadAmmo;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.*;
import net.minecraft.item.ItemStack;
import net.minecraft.util.*;
import net.minecraft.world.World;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.registry.GameRegistry;

public class Mortar_Arrow extends _WeaponBase
{
    public Mortar_Arrow()
    {
	super("arrow_mortar", 8);
    }

    // @SideOnly(Side.CLIENT)
    // @Override
    // public void registerIcons(IIconRegister par1IconRegister)
    // {
    // this.Icon =
    // par1IconRegister.registerIcon("quiverchevsky:weapons/ArrowMortar");
    // }
    //
    // @Override
    // public IIcon getIconFromDamage(int meta) // This is for inventory
    // display.
    // // Comes in with metadata. Only
    // // gets called on client side
    // {
    // return this.Icon; // Full, default
    // }

    public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand)
    {
	ItemStack stack = player.getHeldItem(hand);
	if (this.getDamage(stack) >= stack.getMaxDamage())
	{
	    return ActionResult.<ItemStack>newResult(EnumActionResult.FAIL, stack);
	} // Is empty

	this.doSingleFire(stack, world, player); // Handing it over to the
						 // neutral firing function
	return ActionResult.<ItemStack>newResult(EnumActionResult.SUCCESS, stack);
    }

    @Override
    public void doSingleFire(ItemStack stack, World world, Entity entity) // Server
									  // side
    {
	// Good to go (already verified)
	if (this.getCooldown(stack) > 0)
	{
	    return;
	} // Hasn't cooled down yet

	Helper.knockUserBack(entity, this.Kickback); // Kickback

	// Random Damage
	int dmg_range = this.DmgMax - this.DmgMin; // If max dmg is 20 and min
						   // is 10, then the range will
						   // be 10
	int dmg = world.rand.nextInt(dmg_range + 1); // Range will be between 0
						     // and 10
	dmg += this.DmgMin; // Adding the min dmg of 10 back on top, giving us
			    // the proper damage range (10-20)

	// Firing
	Sabot_Arrow projectile = new Sabot_Arrow(world, entity, (float) this.Speed);
	projectile.damage = dmg;

	world.spawnEntity(projectile); // Firing!

	// SFX
	Helper.playSoundAtEntityPos(entity, SoundEvents.BLOCK_PISTON_EXTEND, 1.0F, 2.0F);

	NetHelper.sendParticleMessageToAllPlayers(world, entity.getEntityId(), EnumParticleTypes.SMOKE_LARGE, (byte) 1);

	this.consumeAmmo(stack, entity, 1);
	this.setCooldown(stack, this.Cooldown);
    }

    @Override
    void doCooldownSFX(World world, Entity entity) // Server side
    {
	Helper.playSoundAtEntityPos(entity, SoundEvents.BLOCK_WOOD_BUTTON_CLICK_ON, 0.6F, 2.0F);
    }

    @Override
    public void addProps(FMLPreInitializationEvent event, Configuration config)
    {
	this.Enabled = config.get(this.name, "Am I enabled? (default true)", true).getBoolean(true);

	this.DmgMin = config.get(this.name, "What damage are my arrows dealing, at least? (default 2)", 2).getInt();
	this.DmgMax = config.get(this.name, "What damage are my arrows dealing, tops? (default 10)", 10).getInt();

	this.Speed = config.get(this.name, "How fast are my projectiles? (default 1.5 BPT (Blocks Per Tick))", 1.5)
		.getDouble();
	this.Kickback = (byte) config.get(this.name, "How hard do I kick the user back when firing? (default 3)", 3)
		.getInt();

	this.Cooldown = config.get(this.name, "How long until I can fire again? (default 20 ticks)", 20).getInt();

	this.isMobUsable = config.get(this.name, "Can I be used by QuiverMobs? (default true.)", true).getBoolean(true);
    }

    @Override
    public void addRecipes()
    {
	if (this.Enabled)
	{
	    // One Arrow Mortar (Empty)
	    GameRegistry.addRecipe(Helper.createEmptyWeaponOrAmmoStack(this, 1), "ipi", "isr", "tsr", 't',
		    Blocks.TRIPWIRE_HOOK, 'i', Items.IRON_INGOT, 's', Blocks.STICKY_PISTON, 'p', Blocks.PISTON, 'r',
		    Items.REPEATER);
	}
	else if (Main.noCreative)
	{
	    this.setCreativeTab(null);
	} // Not enabled and not allowed to be in the creative menu

	ItemStack ammo = Helper.getAmmoStack(ArrowBundle.class, 0);
	GameRegistry.addRecipe(new RecipeLoadAmmo(this).addComponent(ammo.getItem(), 1));
    }

    @Override
    public String getModelTexPath(ItemStack stack) // The model texture path
    {
	return "MortarArrow"; // Regular
    }
}

package com.domochevsky.quiverbow.weapons;

import java.util.List;

import com.domochevsky.quiverbow.Helper;
import com.domochevsky.quiverbow.Main;
import com.domochevsky.quiverbow.ammo.AmmoBase;
import com.domochevsky.quiverbow.ammo.ObsidianMagazine;
import com.domochevsky.quiverbow.net.NetHelper;
import com.domochevsky.quiverbow.projectiles.OWRShot;
import com.domochevsky.quiverbow.projectiles.ProjectileBase;
import com.domochevsky.quiverbow.weapons.base.MagazineFedWeapon;
import com.domochevsky.quiverbow.weapons.base.firingbehaviours.SingleShotFiringBehaviour;

import net.minecraft.client.resources.I18n;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.*;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.world.World;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class OWR extends MagazineFedWeapon
{
	public OWR(AmmoBase ammo)
	{
		super("wither_rifle", ammo, 16);
		setFiringBehaviour(new SingleShotFiringBehaviour<OWR>(this, (world, weaponStack, entity, data) ->
		{
			OWR weapon = (OWR) weaponStack.getItem();
			ProjectileBase projectile = new OWRShot(world, entity, (float) weapon.speed,
					new PotionEffect(MobEffects.WITHER, weapon.witherDuration, weapon.witherStrength));

			// Random Damage
			int dmg_range = weapon.damageMax - weapon.damageMin; // If max dmg is 20
															// and min
			// is 10, then the range will
			// be 10
			int dmg = world.rand.nextInt(dmg_range + 1); // Range will be
															// between 0
			// and 10
			dmg += weapon.damageMin; // Adding the min dmg of 10 back on top,
									// giving us
			// the proper damage range (10-20)

			projectile.damage = dmg;

			// Random Magic Damage
			dmg_range = weapon.damageMagicMax - weapon.damageMagicMin; // If max dmg
																	// is 20 and
			// min is 10, then the
			// range will be 10
			dmg = world.rand.nextInt(dmg_range + 1); // Range will be between 0
														// and
			// 10
			dmg += weapon.damageMagicMin; // Adding the min dmg of 10 back on top,
										// giving
			// us the proper damage range (10-20)

			((OWRShot) projectile).damageMagic = dmg;
			return projectile;
		}));
	}

	public int damageMagicMin;
	public int damageMagicMax;

	public int witherDuration; // 20 ticks to a second, let's start with 3
	// seconds
	public int witherStrength; // 2 dmg per second for 3 seconds = 6 dmg total

	@Override
	protected void doUnloadFX(World world, Entity entity)
	{
		Helper.playSoundAtEntityPos(entity, SoundEvents.BLOCK_WOOD_BUTTON_CLICK_ON, 1.7F, 0.3F);
	}

	@Override
	protected void doCooldownSFX(World world, Entity entity)
	{
		NetHelper.sendParticleMessageToAllPlayers(world, entity.getEntityId(), EnumParticleTypes.SMOKE_LARGE, (byte) 4); // large
		// smoke
		Helper.playSoundAtEntityPos(entity, SoundEvents.BLOCK_FIRE_EXTINGUISH, 1.0F, 1.2F);
	}

	@Override
	public void doFireFX(World world, Entity entity)
	{
		Helper.playSoundAtEntityPos(entity, SoundEvents.ENTITY_GENERIC_EXPLODE, 0.5F, 1.5F);
		NetHelper.sendParticleMessageToAllPlayers(world, entity.getEntityId(), EnumParticleTypes.SPELL_INSTANT,
				(byte) 4); // instant
		// spell
	}

	@SideOnly(Side.CLIENT)
	@Override
	public void addInformation(ItemStack stack, EntityPlayer player, List<String> list, boolean par4)
	{
		super.addInformation(stack, player, list, par4);
		if (this.getCooldown(stack) > 0)
			list.add(I18n.format(getUnlocalizedName() + ".cooldown", this.displayInSec(this.getCooldown(stack))));
	}

	@Override
	public void addProps(FMLPreInitializationEvent event, Configuration config)
	{
		this.enabled = config.get(this.name, "Am I enabled? (default true)", true).getBoolean(true);

		this.damageMin = config.get(this.name, "What damage am I dealing, at least? (default 7)", 7).getInt();
		this.damageMax = config.get(this.name, "What damage am I dealing, tops? (default 13)", 13).getInt();

		this.damageMagicMin = config.get(this.name, "What magic damage am I dealing, at least? (default 6)", 6).getInt();
		this.damageMagicMax = config.get(this.name, "What magic damage am I dealing, tops? (default 14)", 14).getInt();

		this.speed = config.get(this.name, "How fast are my projectiles? (default 3.0 BPT (Blocks Per Tick))", 3.0)
				.getDouble();

		this.knockback = config.get(this.name, "How hard do I knock the target back when firing? (default 2)", 2)
				.getInt();
		this.kickback = (byte) config.get(this.name, "How hard do I kick the user back when firing? (default 6)", 6)
				.getInt();

		this.cooldown = config.get(this.name, "How long until I can fire again? (default 60 ticks)", 60).getInt();

		this.witherStrength = config.get(this.name, "How strong is my Wither effect? (default 3)", 3).getInt();
		this.witherDuration = config.get(this.name, "How long does my Wither effect last? (default 61 ticks)", 61)
				.getInt();

		this.isMobUsable = config
				.get(this.name, "Can I be used by QuiverMobs? (default false. Too high-power for them.)", false)
				.getBoolean();
	}

	@Override
	public void addRecipes()
	{
		if (this.enabled)
		{
			// One wither rifle (empty)
			GameRegistry.addRecipe(Helper.createEmptyWeaponOrAmmoStack(this, 1), "odo", "owo", "oso", 'o',
					Blocks.OBSIDIAN, 'd', Items.DIAMOND, 's', Items.NETHER_STAR, 'w',
					Helper.getWeaponStackByClass(OSR.class, true));
		}
		else if (Main.noCreative)
		{
			this.setCreativeTab(null);
		} // Not enabled and not allowed to be in the creative menu

		// Reloading with obsidian magazine, setting its ammo metadata as ours
		// (Need to be empty for that)
		Helper.registerAmmoRecipe(ObsidianMagazine.class, this);
	}
}

package com.domochevsky.quiverbow.weapons;

import org.apache.commons.lang3.ArrayUtils;

import com.domochevsky.quiverbow.Helper;
import com.domochevsky.quiverbow.config.WeaponProperties;
import com.domochevsky.quiverbow.util.InventoryHelper;
import com.domochevsky.quiverbow.weapons.base.CommonProperties;
import com.domochevsky.quiverbow.weapons.base.WeaponBow;

import net.minecraft.client.Minecraft;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.projectile.EntityArrow;
import net.minecraft.init.Items;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.*;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.ArrowLooseEvent;
import net.minecraftforge.event.entity.player.ArrowNockEvent;

public class EnderBow extends WeaponBow
{
	public EnderBow()
	{
		super("ender_bow", 256);
	}

	private int shotCounter = 0;

	private String playerName = ""; // Holds the name of the firing player, so
	// only they can see it firing
	private int Ticks;

	private int defaultFOV;

	@Override
	protected WeaponProperties createDefaultProperties()
	{
		return WeaponProperties.builder()
				.intProperty(CommonProperties.PROP_MAX_ZOOM, CommonProperties.COMMENT_MAX_ZOOM, 30).build();
	}

	@Override
	public void onUpdate(ItemStack stack, World world, Entity entity, int itemSlot, boolean isSelected)
	{
		if (stack.getTagCompound() == null)
		{
			stack.setTagCompound(new NBTTagCompound());

			stack.getTagCompound().setBoolean("isZoomed", false);
			stack.getTagCompound().setInteger("defaultFOV", 0); // FOV is now
			// using full
			// numbers.
			// We're
			// recording the
			// current
			// default FOV
			// here
		}

		// let's check zoom here
		if (entity instanceof EntityPlayer) // Step 1, is this a player?
		{
			EntityPlayer entityplayer = (EntityPlayer) entity;

			if (isSelected) // Step
			// 2,
			// are
			// they
			// holding
			// the
			// bow?
			{
				if (entityplayer.getActiveHand() != null) // step 3, are they
				// using the
				// bow?
				{
					this.setCurrentZoom(stack, true); // We need to zoom in!

					if (world.isRemote)
					{
						// System.out.println("[ENDER BOW] Current FOV: " +
						// Minecraft.getMinecraft().gameSettings.fovSetting);

						// We're gonna zoom in each tick. Is it bigger than the
						// max zoom? Then keep zooming.
						if (Minecraft.getMinecraft().gameSettings.fovSetting > getProperties().getInt(CommonProperties.PROP_MAX_ZOOM))
						{
							// FOV is now using full numbers. 70 is 70, 120 is
							// 120. Nice of them.
							Minecraft.getMinecraft().gameSettings.fovSetting -= 2; // 2
							// less,
							// until
							// we've
							// reached
							// zoomMax
						}
					}
					// else, server side. Has no deal with game settings

				}
				else // Not using this item currently
				{
					if (this.isCurrentlyZoomed(stack)) // Are we currently
					// zoomed in?
					{
						this.setCurrentZoom(stack, false);
						if (world.isRemote)
						{
							this.restoreFOV(stack);
						} // Begone with that then
					}
					// else, not zoomed in currently
				}
			}
			else // Not holding the bow
			{
				if (this.isCurrentlyZoomed(stack)) // Are we currently zoomed
				// in?
				{
					this.setCurrentZoom(stack, false);
					if (world.isRemote)
					{
						this.restoreFOV(stack);
					} // Begone with that then

				}
			}

			// step 0, recording the current zoom level, if we're not zoomed in.
			// Need to do this AFTER we reset
			if (!this.isCurrentlyZoomed(stack)) // Not zoomed in currently
			{
				if (world.isRemote)
				{
					this.recordCurrentFOV(stack);
				} // Client side only
			}
		}
		// else, not a player holding this thing
	}

	// Records the current FOV setting
	void recordCurrentFOV(ItemStack stack)
	{
		this.defaultFOV = (int) Minecraft.getMinecraft().gameSettings.fovSetting;
	} // Client-side only

	// Sets the FOV setting back to the recorded default
	void restoreFOV(ItemStack stack)
	{
		Minecraft.getMinecraft().gameSettings.fovSetting = this.defaultFOV;
	} // Client-side only

	void setCurrentZoom(ItemStack stack, boolean zoom)
	{
		if (stack.isEmpty())
		{
			return;
		} // Not a valid item
		if (!stack.hasTagCompound())
		{
			return;
		} // No tag

		stack.getTagCompound().setBoolean("isZoomed", zoom);
	}

	boolean isCurrentlyZoomed(ItemStack stack)
	{
		if (stack.isEmpty())
		{
			return false;
		} // Not a valid item
		if (!stack.hasTagCompound())
		{
			return false;
		} // No tag

		return stack.getTagCompound().getBoolean("isZoomed");
	}

	@Override
	public void onPlayerStoppedUsing(ItemStack stack, World world, EntityLivingBase entityLiving, int timeLeft)
	{
		int chargeTime = this.getMaxItemUseDuration(stack) - timeLeft;

		if (entityLiving instanceof EntityPlayer)
		{
			EntityPlayer player = (EntityPlayer) entityLiving;
			// Either creative mode or infinity enchantment is higher than 0.
			// Not
			// using arrows
			boolean freeShot = player.capabilities.isCreativeMode;

			ArrowLooseEvent event = new ArrowLooseEvent(player, stack, world, chargeTime, false);
			MinecraftForge.EVENT_BUS.post(event);

			if (event.isCanceled())
			{
				return;
			} // Not having it

			chargeTime = event.getCharge();

			if (freeShot || player.inventory.hasItemStack(new ItemStack(Items.ARROW)))
			{
				float f = (float) chargeTime / 20.0F;
				f = (f * f + f * 2.0F) / 3.0F;

				if ((double) f < 0.1D)
				{
					return;
				}
				if (f > 1.0F)
				{
					f = 1.0F;
				}

				EntityArrow entityarrow = Helper.createArrow(world, player);
				entityarrow.shoot(entityLiving, entityLiving.rotationPitch, entityLiving.rotationYaw, 0.0F, f * 3.0F,
						0.5F);

				if (f == 1.0F)
				{
					entityarrow.setIsCritical(true);
				}

				stack.damageItem(1, player);
				Helper.playSoundAtEntityPos(player, SoundEvents.ENTITY_ARROW_SHOOT, 1.0F,
						1.0F / (itemRand.nextFloat() * 0.4F + 1.2F) + f * 0.5F);

				if (freeShot)
				{
					entityarrow.pickupStatus = EntityArrow.PickupStatus.CREATIVE_ONLY;
				}
				else
				{
					InventoryHelper.consumeItem(player, Items.ARROW, 1);
				}

				if (!world.isRemote)
				{
					world.spawnEntity(entityarrow);
				} // pew.
			}
		}
	}

	@Override
	public void onUsingTick(ItemStack stack, EntityLivingBase player, int count)
	{
		if (player.world.isRemote)
		{
			// The projectile is only allowed to shoot every X ticks, so we're
			// gonna make this happen here
			this.shotCounter += 1;
			if (this.shotCounter >= Ticks)
			{
				// TODO: predictive OGL rendering
				this.shotCounter = 0;
			}
		}
	}

	@Override
	public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand)
	{
		ItemStack stack = player.getHeldItem(hand);
		ArrowNockEvent event = new ArrowNockEvent(player, stack, hand, world, player.capabilities.isCreativeMode);
		MinecraftForge.EVENT_BUS.post(event);
		if (event.isCanceled())
		{
			return event.getAction();
		}

		if (player.capabilities.isCreativeMode || player.inventory.hasItemStack(new ItemStack(Items.ARROW)))
		{
			player.setActiveHand(hand);
			this.playerName = player.getDisplayName().getUnformattedText(); // Recording
			// the
			// player
			// name here, so only
			// they can see the
			// projectile
		}

		return ActionResult.<ItemStack>newResult(EnumActionResult.SUCCESS, stack);
	}

	@Override
	public boolean onDroppedByPlayer(ItemStack stack, EntityPlayer player)
	{
		stack.getTagCompound().setBoolean("zoom", false); // Dropped the bow
		stack.getTagCompound().setFloat("currentzoom", 0);

		if (player.world.isRemote)
		{
			Minecraft.getMinecraft().gameSettings.fovSetting = stack.getTagCompound().getFloat("zoomlevel"); // Begone
			// with
			// the
			// zoom
		}

		return true;
	}

	@Override
	public void getSubItems(CreativeTabs tab, NonNullList<ItemStack> subItems)
	{
		if(!ArrayUtils.contains(this.getCreativeTabs(), tab)) return;
		subItems.add(new ItemStack(this, 1, 0));
	}
}

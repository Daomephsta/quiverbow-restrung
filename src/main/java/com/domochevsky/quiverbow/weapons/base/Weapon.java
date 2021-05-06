package com.domochevsky.quiverbow.weapons.base;

import java.util.List;

import com.domochevsky.quiverbow.AmmoContainer;
import com.domochevsky.quiverbow.QuiverbowMain;
import com.domochevsky.quiverbow.config.WeaponProperties;
import com.domochevsky.quiverbow.weapons.base.trigger.Trigger;

import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.EnumAction;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.*;
import net.minecraft.world.World;

/**
 * Composition-style weapon class that delegates functionality to reusable
 * components such as triggers, fire effects, cooldown effects, etc.
 * Should be considered final, but extension is allowed if it more cleanly
 * solves an issue.
 * @author Daomephsta
 */
public class Weapon extends Item implements AmmoContainer
{
    private final WeaponProperties properties;
    private final Trigger trigger;
    private Effect[] fireEffects;
    private Effect[] cooldownEffects;
    private EnumAction useAction = EnumAction.NONE;
    private int maxUseTicks = 0;

    public Weapon(String name, WeaponProperties.Builder propertiesBuilder, Trigger trigger)
    {
        propertiesBuilder.setId(new ResourceLocation(QuiverbowMain.MODID, name));
        this.properties = propertiesBuilder.build();
        this.trigger = trigger;
        setRegistryName(QuiverbowMain.MODID, name);
        setUnlocalizedName(QuiverbowMain.MODID + ".weapon." + name);
        setCreativeTab(QuiverbowMain.QUIVERBOW_TAB);
        setMaxStackSize(1);
        trigger.adjustItemProperties(this);
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand)
    {
        ItemStack stack = player.getHeldItem(hand);
        ActionResult<ItemStack> triggerResult = trigger.usePressed(world, player, stack, hand, properties);
        if (triggerResult.getType() == EnumActionResult.SUCCESS)
            applyEffects(fireEffects, world, player, stack);
        return triggerResult;
    }

    @Override
    public void onUsingTick(ItemStack stack, EntityLivingBase player, int count)
    {
        if (trigger.useTick(player.getEntityWorld(), stack, player, properties, count))
            applyEffects(fireEffects, player.getEntityWorld(), player, stack);
    }

    @Override
    public void onPlayerStoppedUsing(ItemStack stack, World world, EntityLivingBase entityLiving, int timeLeft)
    {
        if(trigger.useReleased(world, entityLiving, stack, properties))
            applyEffects(fireEffects, world, entityLiving, stack);
    }

    @Override
    public boolean onEntitySwing(EntityLivingBase living, ItemStack stack)
    {
        // onEntitySwing is called again while swinging for some reason
        if (living.isSwingInProgress)
            return false;
        if (trigger.attackPressed(living.getEntityWorld(), living, stack, properties))
            applyEffects(fireEffects, living.getEntityWorld(), living, stack);
        return false;
    }

    @Override
    public void onUpdate(ItemStack stack, World world, Entity entity, int animTick, boolean holdingItem)
    {
        if (getCooldown(stack) > 0)
            setCooldown(stack, getCooldown(stack) - 1);
        if (getCooldown(stack) == 1 && entity instanceof EntityLivingBase) // Check at 1 to avoid infinite effects
            applyEffects(cooldownEffects, world, (EntityLivingBase) entity, stack);
        if (entity instanceof EntityLivingBase &&
            trigger.weaponTick(world, (EntityLivingBase) entity, stack, properties))
        {
            applyEffects(fireEffects, world, (EntityLivingBase) entity, stack);
        }
    }

    public static void setCooldown(ItemStack stack, int cooldown)
    {
        if (!stack.hasTagCompound())
            stack.setTagCompound(new NBTTagCompound());
        stack.getTagCompound().setInteger("cooldown", cooldown); // Done
    }

    public static int getCooldown(ItemStack stack)
    {
        if (!stack.hasTagCompound())
            return 0;
        return stack.getTagCompound().getInteger("cooldown");
    }

    @Override
    public boolean canContinueUsing(ItemStack oldStack, ItemStack newStack)
    {
        // QB:R weapons are not differentiated by damage or NBT
        return oldStack.isItemEqualIgnoreDurability(newStack);
    }

    @Override
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged)
    {
        // QB:R weapons are not differentiated by damage or NBT
        return !oldStack.isItemEqualIgnoreDurability(newStack);
    }

    @Override
    public void addInformation(ItemStack stack, World world, List<String> list, ITooltipFlag flags)
    {
        list.add(I18n.format(getUnlocalizedName() + ".ammostatus", getAmmo(stack), getAmmoCapacity(stack)));
    }

    @Override
    public CreativeTabs getCreativeTab()
    {
        if (!properties.isEnabled())
            return null;
        return super.getCreativeTab();
    }

    @Override
    public void getSubItems(CreativeTabs tab, NonNullList<ItemStack> items)
    {
        if (this.isInCreativeTab(tab))
            items.add(createFull());
    }

    @Override
    public boolean isBookEnchantable(ItemStack stack, ItemStack book)
    {
        return false;
    }

    @Override
    public double getDurabilityForDisplay(ItemStack stack)
    {
        return (double) (getAmmoCapacity(stack) - getAmmo(stack)) / (double) getAmmoCapacity(stack);
    }

    @Override
    public boolean showDurabilityBar(ItemStack stack)
    {
        return true;
    }

    @Override
    public EnumAction getItemUseAction(ItemStack stack)
    {
        return useAction;
    }

    @Override
    public int getMaxItemUseDuration(ItemStack stack)
    {
        return maxUseTicks;
    }

    public WeaponProperties getProperties()
    {
        return properties;
    }

    public Trigger getTrigger()
    {
        return trigger;
    }

    public Weapon fireEffects(Effect... fireEffects)
    {
        this.fireEffects = fireEffects;
        return this;
    }

    public Weapon cooldownEffects(Effect... cooldownEffects)
    {
        this.cooldownEffects = cooldownEffects;
        return this;
    }

    private void applyEffects(Effect[] effects, World world, EntityLivingBase entity, ItemStack stack)
    {
        if (effects != null)
        {
            for (Effect effect : effects)
                effect.apply(world, entity, stack, properties);
        }
    }

    public Weapon setUseParameters(EnumAction action, int maxUseTicks)
    {
        this.useAction = action;
        this.maxUseTicks = maxUseTicks;
        return this;
    }

    public boolean hasAmmo(EntityLivingBase shooter, ItemStack stack, WeaponProperties properties)
    {
        return trigger.getAmmoSource().hasAmmo(shooter, stack, properties);
    }

    public int getAmmo(ItemStack stack)
    {
        return trigger.getAmmoSource().getAmmo(stack);
    }

    public void addAmmo(ItemStack stack, int increment)
    {
        trigger.getAmmoSource().addAmmo(stack, increment);
    }

    public void removeAmmo(ItemStack stack, int increment)
    {
        trigger.getAmmoSource().removeAmmo(stack, increment);
    }

    public int getAmmoCapacity(ItemStack stack)
    {
        return trigger.getAmmoSource().getAmmoCapacity(stack);
    }

    @Override
    public ItemStack createFull()
    {
        ItemStack stack = new ItemStack(this);
        addAmmo(stack, getAmmoCapacity(stack));
        return stack;
    }

    public static interface Effect
    {
        public void apply(World world, EntityLivingBase shooter, ItemStack stack, WeaponProperties properties);
    }
}

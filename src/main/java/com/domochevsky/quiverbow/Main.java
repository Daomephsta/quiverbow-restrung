package com.domochevsky.quiverbow;

import java.util.ArrayList;

import com.domochevsky.quiverbow.Main.Constants;
import com.domochevsky.quiverbow.ArmsAssistant.Entity_AA;
import com.domochevsky.quiverbow.ammo.*;
import com.domochevsky.quiverbow.blocks.FenLight;
import com.domochevsky.quiverbow.miscitems.*;
import com.domochevsky.quiverbow.models.*;
import com.domochevsky.quiverbow.net.PacketHandler;
import com.domochevsky.quiverbow.projectiles.*;
import com.domochevsky.quiverbow.recipes.*;
import com.domochevsky.quiverbow.util.RegistryHelper;
import com.domochevsky.quiverbow.weapons.*;

import net.minecraftforge.fml.common.*;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.Mod.Instance;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.registry.EntityRegistry;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.model.ModelBase;
import net.minecraft.entity.Entity;
import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.oredict.RecipeSorter;

@Mod(modid = Constants.MODID, name = Constants.NAME, version = "b102")
public class Main
{
    public static class Constants
    {
	public static final String MODID = "quiverchevsky";
	public static final String NAME = "QuiverBow";
    }

    @Instance(Constants.MODID)
    public static Main instance;

    @SidedProxy(clientSide = "com.domochevsky.quiverbow.ClientProxy", serverSide = "com.domochevsky.quiverbow.CommonProxy")
    public static CommonProxy proxy;

    protected Configuration config; // Accessible from other files this way

    public static ArrayList<_WeaponBase> weapons = new ArrayList<_WeaponBase>(); // Holder
										 // array
										 // for
										 // all
										 // (fully
										 // set
										 // up)
										 // possible
										 // weapons
    public static ArrayList<_AmmoBase> ammo = new ArrayList<_AmmoBase>(); // Same
									  // with
									  // ammo,
									  // since
									  // they
									  // got
									  // recipes
									  // as
									  // well
    // private static String[] weaponType = new String[60]; // For Battle Gear 2

    @SideOnly(Side.CLIENT)
    public static ArrayList<ModelBase> models; // Client side only

    public static Item gatlingBody;
    public static Item gatlingBarrel;
    public static Item packedAA;
    public static Item packedBB;

    public static Block fenLight;

    private static int projectileCount = 1; // A running number, to register
					    // projectiles by

    // Config
    public static boolean breakGlass; // If this is false then we're not allowed
				      // to break blocks with projectiles (Don't
				      // care about TNT)
    public static boolean useModels; // If this is false then we're reverting
				     // back to held icons
    public static boolean noCreative; // If this is true then disabled weapons
				      // won't show up in the creative menu
				      // either
    public static boolean allowTurret; // If this is false then the Arms
				       // Assistant will not be available
    public static boolean allowTurretPlayerAttacks; // If this is false then the
						    // AA is not allowed to
						    // attack players (ignores
						    // them)
    public static boolean restrictTurretRange; // If this is false then we're
					       // not capping the targeting
					       // range at 32 blocks
    public static boolean sendBlockBreak; // If this is false then
					  // Helper.tryBlockBreak() won't send a
					  // BlockBreak event. Used by
					  // protection plugins.

    @EventHandler
    public void preInit(FMLPreInitializationEvent event)
    {
	this.config = new Configuration(event.getSuggestedConfigurationFile()); // Starting
										// config

	this.config.load(); // And loading it up

	breakGlass = this.config.get("generic",
		"Can we break glass and other fragile things with our projectiles? (default true)", true).getBoolean();
	sendBlockBreak = this.config
		.get("generic",
			"Do we send a BlockBreak event when breaking things with our projectiles? (default true)", true)
		.getBoolean();
	useModels = this.config.get("generic",
		"Are we using models or icons for held weapons? (default true for models. False for icons)", true)
		.getBoolean();
	noCreative = this.config.get("generic",
		"Are we removing disabled weapons from the creative menu too? (default false. On there, but uncraftable)",
		false).getBoolean();

	allowTurret = this.config.get("Arms Assistant", "Am I enabled? (default true)", true).getBoolean();
	restrictTurretRange = this.config.get("Arms Assistant",
		"Is my firing range limited to a maximum of 32 blocks? (default true. Set false for 'Shoot as far as your weapon can handle'.)",
		true).getBoolean();

	// Item registry here
	this.registerAmmo();
	this.registerWeapons(event.getSide().isClient());
	this.registerProjectiles();
	this.registerBlocks();
	this.registerMiscItems();

	addAllProps(event, this.config); // All items are registered now. Making
					 // recipes and recording props

	this.config.save(); // Done with config, saving it

	PacketHandler.initPackets(); // Used for sending particle packets, so I
				     // can do my thing purely on the server
				     // side

	// Registering the Arms Assistant
	EntityRegistry.registerModEntity(new ResourceLocation(Constants.MODID, "turret"), Entity_AA.class, "turret", 0,
		this, 80, 1, true);
	// EntityRegistry.registerModEntity(Entity_BB.class,
	// "quiverchevsky_flyingBB", 1, this, 80, 1, true);

	proxy.registerTurretRenderer();

	RecipeSorter.register(Constants.MODID + ":ender_rail_accelerator", Recipe_ERA.class,
		RecipeSorter.Category.SHAPED, "after:minecraft:shapeless");
	RecipeSorter.register(Constants.MODID + ":era_upgrade", Recipe_Weapon.class, RecipeSorter.Category.SHAPED,
		"after:minecraft:shapeless");
	RecipeSorter.register(Constants.MODID + ":load_magazine", RecipeLoadMagazine.class,
		RecipeSorter.Category.SHAPELESS, "after:minecraft:shapeless");

	Listener listener = new Listener();

	FMLCommonHandler.instance().bus().register(listener);
	MinecraftForge.EVENT_BUS.register(listener);

	if (event.getSide().isServer())
	{
	    return;
	} // Client-only from this point.

	ListenerClient listenerClient = new ListenerClient();

	FMLCommonHandler.instance().bus().register(listenerClient);
	MinecraftForge.EVENT_BUS.register(listenerClient);
    }

    void registerAmmo() // Items.WITH which weapons can be reloaded
    {
	this.addAmmo(new ArrowBundle(), "arrow_bundle");
	this.addAmmo(new RocketBundle(), "rocket_bundle");

	this.addAmmo(new GatlingAmmo(), "sugar_magazine");

	this.addAmmo(new LargeRocket(), "large_rocket");
	this.addAmmo(new ColdIronClip(), "cold_iron_clip");
	this.addAmmo(new BoxOfFlintDust(), "box_of_flint_dust");
	this.addAmmo(new SeedJar(), "seed_jar");

	this.addAmmo(new ObsidianMagazine(), "obsidian_magazine");
	this.addAmmo(new GoldMagazine(), "gold_magazine");
	this.addAmmo(new NeedleMagazine(), "thorn_magazine");
	this.addAmmo(new LapisMagazine(), "lapis_magazine");
	this.addAmmo(new RedstoneMagazine(), "redstone_magazine");

	this.addAmmo(new LargeNetherrackMagazine(), "large_netherrack_magazine");
	this.addAmmo(new LargeRedstoneMagazine(), "large_redstone_magazine");

	this.addAmmo(new EnderQuartzClip(), "ender_quartz_magazine");
    }

    void registerWeapons(boolean isClient) // The weapons themselves
    {
	this.addWeapon(new Crossbow_Compact(), new Crossbow_Model(), isClient, "dual");
	this.addWeapon(new Crossbow_Double(), new CrossbowDouble_Model(), isClient, "mainhand");
	this.addWeapon(new Crossbow_Blaze(), new Crossbow_Model(), isClient, "mainhand");
	this.addWeapon(new Crossbow_Auto(), new CrossbowAuto_Model(), isClient, "mainhand");
	this.addWeapon(new Crossbow_AutoImp(), new CrossbowAutoImp_Model(), isClient, "mainhand");

	this.addWeapon(new CoinTosser(), new CoinTosser_Model(), isClient, "mainhand");
	this.addWeapon(new CoinTosser_Mod(), new CoinTosser_Mod_Model(), isClient, "mainhand");

	this.addWeapon(new DragonBox(), new DragonBox_Model(), isClient, "mainhand");
	this.addWeapon(new DragonBox_Quad(), new QuadBox_Model(), isClient, "mainhand");

	this.addWeapon(new LapisCoil(), new LapisCoil_Model(), isClient, "mainhand");
	this.addWeapon(new ThornSpitter(), new ThornSpitter_Model(), isClient, "dual");
	this.addWeapon(new ProximityNeedler(), new PTT_Model(), isClient, "mainhand");
	this.addWeapon(new SugarEngine(), new SugarEngine_Model(), isClient, "mainhand");

	this.addWeapon(new RPG(), new RPG_Model(), isClient, "mainhand");
	this.addWeapon(new RPG_Imp(), new RPG_Model(), isClient, "mainhand");

	this.addWeapon(new Mortar_Arrow(), new Mortar_Model(), isClient, "mainhand");
	this.addWeapon(new Mortar_Dragon(), new Mortar_Model(), isClient, "mainhand");

	this.addWeapon(new Seedling(), new Seedling_Model(), isClient, "dual");
	this.addWeapon(new Potatosser(), new Potatosser_Model(), isClient, "mainhand");
	this.addWeapon(new SnowCannon(), new SnowCannon_Model(), isClient, "dual");

	this.addWeapon(new QuiverBow(), null, isClient, "mainhand");

	this.addWeapon(new EnderBow(), null, isClient, "mainhand");
	this.addWeapon(new EnderRifle(), new EnderRifle_Model(), isClient, "mainhand");
	this.addWeapon(new FrostLancer(), new FrostLancer_Model(), isClient, "mainhand");

	this.addWeapon(new OSP(), new OSP_Model(), isClient, "dual");
	this.addWeapon(new OSR(), new OSR_Model(), isClient, "mainhand");
	this.addWeapon(new OWR(), new OWR_Model(), isClient, "mainhand");

	this.addWeapon(new FenFire(), new FenFire_Model(), isClient, "dual");
	this.addWeapon(new FlintDuster(), new FlintDuster_Model(), isClient, "mainhand");

	this.addWeapon(new LightningRed(), new LightningRed_Model(), isClient, "mainhand");
	this.addWeapon(new Sunray(), new Sunray_Model(), isClient, "mainhand");

	this.addWeapon(new PowderKnuckle(), null, isClient, "dual");
	this.addWeapon(new PowderKnuckle_Mod(), null, isClient, "dual");

	this.addWeapon(new NetherBellows(), new NetherBellows_Model(), isClient, "mainhand");
	this.addWeapon(new RedSprayer(), new RedSprayer_Model(), isClient, "mainhand");

	this.addWeapon(new SoulCairn(), new SoulCairn_Model(), isClient, "dual");
	this.addWeapon(new AquaAccelerator(), new AquaAcc_Model(), isClient, "dual");
	this.addWeapon(new SilkenSpinner(), new AquaAcc_Model(), isClient, "dual");

	this.addWeapon(new SeedSweeper(), new SeedSweeper_Model(), isClient, "mainhand");
	this.addWeapon(new MediGun(), new MediGun_Model(), isClient, "mainhand");

	this.addWeapon(new ERA(), new ERA_Model(), isClient, "mainhand");

	this.addWeapon(new AA_Targeter(), new AATH_Model(), isClient, "dual");

	this.addWeapon(new Endernymous(), new EnderNymous_Model(), isClient, "dual");
    }

    void registerProjectiles() // Entities that get shot out of weapons as
			       // projectiles
    {
	this.addProjectile(RegularArrow.class, true, "arrow");
	this.addProjectile(BlazeShot.class, true, "blaze");
	this.addProjectile(CoinShot.class, true, "coin");
	this.addProjectile(SmallRocket.class, true, "rocket_small");

	this.addProjectile(LapisShot.class, true, "lapis");
	this.addProjectile(Thorn.class, true, "thorn");
	this.addProjectile(ProxyThorn.class, true, "proximity_thorn");
	this.addProjectile(SugarRod.class, true, "sugar");

	this.addProjectile(BigRocket.class, true, "rocket_big");

	this.addProjectile(Sabot_Arrow.class, true, "sabot_arrow");
	this.addProjectile(Sabot_Rocket.class, true, "sabot_rocket");

	this.addProjectile(Seed.class, true, "seed");
	this.addProjectile(PotatoShot.class, true, "potato");
	this.addProjectile(SnowShot.class, true, "snow");

	this.addProjectile(ScopedPredictive.class, true, "predictive");
	this.addProjectile(EnderShot.class, true, "ender");
	this.addProjectile(ColdIron.class, true, "cold_iron");

	this.addProjectile(OSP_Shot.class, true, "osp_shot");
	this.addProjectile(OSR_Shot.class, true, "osr_shot");
	this.addProjectile(OWR_Shot.class, true, "owr_shot");

	this.addProjectile(FenGoop.class, true, "fen_light");
	this.addProjectile(FlintDust.class, true, "flint_dust");

	this.addProjectile(RedLight.class, true, "red_light");
	this.addProjectile(SunLight.class, true, "sunlight");

	this.addProjectile(NetherFire.class, true, "nether_fire");
	this.addProjectile(RedSpray.class, true, "red_spray");

	this.addProjectile(SoulShot.class, true, "soul");

	this.addProjectile(WaterShot.class, true, "water");
	this.addProjectile(WebShot.class, true, "web");

	this.addProjectile(HealthBeam.class, true, "health");

	this.addProjectile(EnderAccelerator.class, true, "era_shot");
	this.addProjectile(EnderAno.class, true, "ano");
    }

    private void registerBlocks() // Blocks.WE can place
    {
	fenLight = new FenLight(Material.GLASS);
	fenLight.setRegistryName(Constants.MODID, "fen_light");
	fenLight.setUnlocalizedName(Constants.MODID + ".fen_light");
	GameRegistry.register(fenLight);
    }

    private void registerMiscItems()
    {
	gatlingBody = RegistryHelper.registerItem(new Part_GatlingBody(), ".misc.", "part_se_body");
	gatlingBarrel = RegistryHelper.registerItem(new Part_GatlingBarrel(), ".misc.", "part_se_barrel");
	packedAA = RegistryHelper.registerItem(new PackedUpAA(), ".misc.", "turret_spawner");
	// packedBB = RegistryHelper.registerItem(new PackedUpBB(),
	// "FlyingAASpawner");
    }

    private void addAmmo(_AmmoBase ammoBase, String name)
    {
	Main.ammo.add(ammoBase);
	ammoBase.setUnlocalizedName(Constants.MODID + ".ammo." + name);
	RegistryHelper.registerItem(ammoBase, ".ammo.", name);
    }

    // Helper function for taking care of weapon registration
    private void addWeapon(_WeaponBase weapon, ModelBase model, boolean isClient, String handedness)
    {
	if (Main.weapons == null)
	{
	    Main.weapons = new ArrayList<_WeaponBase>();
	}

	Main.weapons.add(weapon);

	RegistryHelper.registerItem(weapon, ".weapon.", weapon.getName());

	if (isClient && useModels && model != null) // Do we care about models?
						    // And if we do, do we got a
						    // custom weapon model? :O
	{
	    if (Main.models == null)
	    {
		Main.models = new ArrayList<ModelBase>();
	    } // Init

	    Main.models.add(model); // Keeping track of it
	    proxy.registerWeaponRenderer(weapon, (byte) Main.models.indexOf(model)); // And
										     // registering
										     // its
										     // renderer
	}
    }

    private void addProjectile(Class<? extends Entity> entityClass, boolean hasRenderer, String name)
    {
	EntityRegistry.registerModEntity(new ResourceLocation(Constants.MODID, name), entityClass,
		"projectilechevsky_" + name, projectileCount, this, 80, 1, true);

	if (hasRenderer)
	{
	    proxy.registerProjectileRenderer(entityClass);
	} // Entity-specific renderer

	projectileCount += 1;
    }

    // Adding props and recipes for all registered weapons now
    private static void addAllProps(FMLPreInitializationEvent event, Configuration config)
    {
	// Ammo first
	for (_AmmoBase ammunition : ammo)
	{
	    ammunition.addRecipes();
	}

	// Weapons last
	for (_WeaponBase weapon : weapons)
	{
	    weapon.addProps(event, config);
	    weapon.addRecipes();
	}
    }
}
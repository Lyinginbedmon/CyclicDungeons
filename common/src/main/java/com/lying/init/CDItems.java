package com.lying.init;

import static com.lying.reference.Reference.ModInfo.prefix;

import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

import com.google.common.collect.Lists;
import com.lying.CyclicDungeons;
import com.lying.item.WiringGunItem;
import com.lying.reference.Reference;

import dev.architectury.registry.CreativeTabRegistry;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.block.Block;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.Item.Settings;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.Rarity;

@SuppressWarnings("unused")
public class CDItems
{
	public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Reference.ModInfo.MOD_ID, RegistryKeys.ITEM);
	public static final DeferredRegister<ItemGroup> TABS = DeferredRegister.create(Reference.ModInfo.MOD_ID, RegistryKeys.ITEM_GROUP);
	private static int itemTally = 0, blockTally = 0;
	
	public static final List<RegistrySupplier<Item>> BASIC_BLOCK_ITEMS = Lists.newArrayList(), ALL_BLOCKS = Lists.newArrayList();
	
	public static final RegistrySupplier<ItemGroup> CYDUN_TAB = TABS.register(Reference.ModInfo.MOD_ID, () -> CreativeTabRegistry.create(
			Text.translatable("itemGroup."+Reference.ModInfo.MOD_ID+".item_group"), 
			() -> new ItemStack(Items.SPAWNER)));
	
	public static final RegistrySupplier<Item> WIRING_GUN	= register("wiring_gun", s -> new WiringGunItem(s.maxCount(1).fireproof().rarity(Rarity.EPIC)));
	
	// Block items
	public static final RegistrySupplier<Item> TRAP_LOGIC		= registerRareBlock("trap_logic", CDBlocks.TRAP_LOGIC, Rarity.EPIC);
	public static final RegistrySupplier<Item> TRAP_LOGIC_DECOY	= registerBlockNoItem("trap_logic_decoy", CDBlocks.TRAP_LOGIC_DECOY, s -> s.rarity(Rarity.RARE));
	
	public static final RegistrySupplier<Item> SENSOR_REDSTONE	= registerRareBlock("redstone_sensor", CDBlocks.SENSOR_REDSTONE, Rarity.RARE);
	public static final RegistrySupplier<Item> SENSOR_COLLISION	= registerRareBlock("collision_sensor", CDBlocks.SENSOR_COLLISION, Rarity.RARE);
	public static final RegistrySupplier<Item> SENSOR_SOUND		= registerRareBlock("sound_sensor", CDBlocks.SENSOR_SOUND, Rarity.RARE);
	public static final RegistrySupplier<Item> SENSOR_SIGHT		= registerRareBlock("sight_sensor", CDBlocks.SENSOR_SIGHT, Rarity.RARE);
	public static final RegistrySupplier<Item> SENSOR_PROXIMITY	= registerRareBlock("proximity_sensor", CDBlocks.SENSOR_PROXIMITY, Rarity.RARE);
	
	public static final RegistrySupplier<Item> PIT					= registerRareBlockNoItem("pit", CDBlocks.PIT, Rarity.RARE);
	public static final RegistrySupplier<Item> CRUMBLING_STONE				= registerBlock("crumbling_stone", CDBlocks.CRUMBLING_STONE);
	public static final RegistrySupplier<Item> CRUMBLING_COBBLESTONE		= registerBlock("crumbling_cobblestone", CDBlocks.CRUMBLING_COBBLESTONE);
	public static final RegistrySupplier<Item> CRUMBLING_MOSSY_COBBLESTONE	= registerBlock("crumbling_mossy_cobblestone", CDBlocks.CRUMBLING_MOSSY_COBBLESTONE);
	public static final RegistrySupplier<Item> CRUMBLING_SANDSTONE			= registerBlock("crumbling_sandstone", CDBlocks.CRUMBLING_SANDSTONE);
	public static final RegistrySupplier<Item> CRUMBLING_RED_SANDSTONE		= registerBlock("crumbling_red_sandstone", CDBlocks.CRUMBLING_RED_SANDSTONE);
	public static final RegistrySupplier<Item> CRUMBLING_STONE_BRICK		= registerBlock("crumbling_stone_bricks", CDBlocks.CRUMBLING_STONE_BRICKS);
	public static final RegistrySupplier<Item> ACTOR_REDSTONE		= registerRareBlock("redstone_actor", CDBlocks.ACTOR_REDSTONE, Rarity.RARE);
	public static final RegistrySupplier<Item> STONE_BRICK_HATCH	= registerRareBlock("stone_brick_hatch", CDBlocks.STONE_BRICK_HATCH, Rarity.RARE);
	public static final RegistrySupplier<Item> STONE_HATCH			= registerRareBlock("stone_hatch", CDBlocks.STONE_HATCH, Rarity.RARE);
	public static final RegistrySupplier<Item> COBBLESTONE_HATCH	= registerRareBlock("cobblestone_hatch", CDBlocks.COBBLESTONE_HATCH, Rarity.RARE);
	public static final RegistrySupplier<Item> MOSSY_COBBLESTONE_HATCH	= registerRareBlock("mossy_cobblestone_hatch", CDBlocks.MOSSY_COBBLESTONE_HATCH, Rarity.RARE);
	public static final RegistrySupplier<Item> GRASS_HATCH			= registerRareBlock("grass_hatch", CDBlocks.GRASS_HATCH, Rarity.RARE);
	public static final RegistrySupplier<Item> SANDSTONE_HATCH		= registerRareBlock("sandstone_hatch", CDBlocks.SANDSTONE_HATCH, Rarity.RARE);
	public static final RegistrySupplier<Item> RED_SANDSTONE_HATCH	= registerRareBlock("red_sandstone_hatch", CDBlocks.RED_SANDSTONE_HATCH, Rarity.RARE);
	public static final RegistrySupplier<Item> DIRT_HATCH			= registerRareBlock("dirt_hatch", CDBlocks.DIRT_HATCH, Rarity.RARE);
	public static final RegistrySupplier<Item> SWINGING_BLADE		= registerRareBlockNoItem("swinging_blade", CDBlocks.SWINGING_BLADE, Rarity.RARE);
	public static final RegistrySupplier<Item> FLAME_JET			= registerRareBlock("flame_jet", CDBlocks.FLAME_JET, Rarity.RARE);
	
	private static RegistrySupplier<Item> registerBlock(String nameIn, RegistrySupplier<Block> blockIn)
	{
		return registerBlock(nameIn, blockIn, UnaryOperator.identity());
	}
	
	private static RegistrySupplier<Item> registerRareBlock(String nameIn, RegistrySupplier<Block> blockIn, Rarity rarity)
	{
		return registerBlock(nameIn, blockIn, s -> s.rarity(rarity));
	}
	
	private static RegistrySupplier<Item> registerRareBlockNoItem(String nameIn, RegistrySupplier<Block> blockIn, Rarity rarity)
	{
		return registerBlockNoItem(nameIn, blockIn, s -> s.rarity(rarity));
	}
	
	private static RegistrySupplier<Item> registerBlock(String nameIn, RegistrySupplier<Block> blockIn, UnaryOperator<Item.Settings> settingsOp)
	{
		RegistrySupplier<Item> registry = registerBlockNoItem(nameIn, blockIn, settingsOp);
		BASIC_BLOCK_ITEMS.add(registry);
		return registry;
	}
	
	private static RegistrySupplier<Item> registerBlockNoItem(String nameIn, RegistrySupplier<Block> blockIn)
	{
		return registerBlockNoItem(nameIn, blockIn, UnaryOperator.identity());
	}
	
	private static RegistrySupplier<Item> registerBlockNoItem(String nameIn, RegistrySupplier<Block> blockIn, UnaryOperator<Item.Settings> settingsOp)
	{
		Identifier id = prefix(nameIn);
		RegistryKey<Item> key = RegistryKey.of(RegistryKeys.ITEM, id);
		return registerBlockItem(nameIn, () -> new BlockItem(blockIn.get(), settingsOp.apply(new Item.Settings().useBlockPrefixedTranslationKey().registryKey(key).arch$tab(CYDUN_TAB))));
	}
	
	private static RegistrySupplier<Item> registerBlockItem(String nameIn, Supplier<Item> supplier)
	{
		RegistrySupplier<Item> registry = register(prefix(nameIn), supplier);
		ALL_BLOCKS.add(registry);
		blockTally++;
		return registry;
	}
	
	private static RegistrySupplier<Item> register(String nameIn, Function<Settings,Item> supplierIn)
	{
		Identifier id = Reference.ModInfo.prefix(nameIn);
		RegistryKey<Item> key = RegistryKey.of(RegistryKeys.ITEM, id);
		Item.Settings settings = new Item.Settings().registryKey(key).arch$tab(CYDUN_TAB);
		return register(id, () -> supplierIn.apply(settings));
	}
	
	private static RegistrySupplier<Item> register(Identifier id, Supplier<Item> supplierIn)
	{
		itemTally++;
		return ITEMS.register(id, supplierIn);
	}
	
	public static void init()
	{
		TABS.register();
		ITEMS.register();
		
		CyclicDungeons.LOGGER.info(" # Initialised {} items ({} block items)", itemTally, blockTally);
	}
}

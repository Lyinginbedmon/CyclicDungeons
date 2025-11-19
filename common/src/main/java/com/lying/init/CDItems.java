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
	public static final RegistrySupplier<Item> SENSOR_REDSTONE	= registerRareBlockNoItem("redstone_sensor", CDBlocks.SENSOR_REDSTONE, Rarity.RARE);
	public static final RegistrySupplier<Item> SENSOR_COLLISION	= registerRareBlock("collision_sensor", CDBlocks.SENSOR_COLLISION, Rarity.RARE);
	public static final RegistrySupplier<Item> SENSOR_SOUND		= registerRareBlock("sound_sensor", CDBlocks.SENSOR_SOUND, Rarity.RARE);
	public static final RegistrySupplier<Item> SENSOR_SIGHT		= registerRareBlock("sight_sensor", CDBlocks.SENSOR_SIGHT, Rarity.RARE);
	public static final RegistrySupplier<Item> ACTOR_REDSTONE	= registerRareBlockNoItem("redstone_actor", CDBlocks.ACTOR_REDSTONE, Rarity.RARE);
	
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
		CyclicDungeons.LOGGER.info("# Initialised {} items ({} block items)", itemTally, blockTally);
	}
}

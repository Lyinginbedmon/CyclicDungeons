package com.lying.data;

import static com.lying.reference.Reference.ModInfo.prefix;

import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;

public class RCTags
{
	public static final TagKey<Block> RUST				= TagKey.of(RegistryKeys.BLOCK, prefix("rust_block"));
	public static final TagKey<Block> FADED_TERRACOTTA	= TagKey.of(RegistryKeys.BLOCK, prefix("faded_terracotta"));
	public static final TagKey<Block> CRACKED_CONCRETE	= TagKey.of(RegistryKeys.BLOCK, prefix("cracked_concrete"));
	public static final TagKey<Block> CONCRETE			= TagKey.of(RegistryKeys.BLOCK, Identifier.ofVanilla("concrete"));
	public static final TagKey<Block> MOLD_IMPERVIOUS	= TagKey.of(RegistryKeys.BLOCK, prefix("mold_impervious"));
	
	public static final TagKey<Item> IGNITER_ITEMS		= TagKey.of(RegistryKeys.ITEM, prefix("igniters"));
	public static final TagKey<Item> ROTTEN_FRUIT		= TagKey.of(RegistryKeys.ITEM, prefix("rotten_fruit"));
	public static final TagKey<Item> ROTTEN_PUMPKIN		= TagKey.of(RegistryKeys.ITEM, prefix("rotten_pumpkin"));
}

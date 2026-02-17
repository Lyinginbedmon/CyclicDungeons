package com.lying.data;

import static com.lying.reference.Reference.ModInfo.prefix;

import net.minecraft.block.Block;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;

public class CDTags
{
	public static final TagKey<Block> PLACER_AVOID		= TagKey.of(RegistryKeys.BLOCK, prefix("trap_avoid"));
}

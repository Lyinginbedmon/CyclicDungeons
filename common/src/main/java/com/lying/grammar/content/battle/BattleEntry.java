package com.lying.grammar.content.battle;

import com.lying.grammar.RoomMetadata;
import com.lying.grammar.content.IContentEntry;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

public record BattleEntry(Identifier registryName, Battle encounter) implements IContentEntry
{
	public static final Codec<BattleEntry> CODEC	= RecordCodecBuilder.create(instance -> instance.group(
			Identifier.CODEC.fieldOf("Name").forGetter(BattleEntry::registryName),
			Battle.CODEC.fieldOf("Encounter").forGetter(BattleEntry::encounter)
			).apply(instance, BattleEntry::new));
	
	public void apply(BlockPos min, BlockPos max, ServerWorld world, RoomMetadata meta)
	{
		encounter.apply(min, max, world, meta);
	}
}
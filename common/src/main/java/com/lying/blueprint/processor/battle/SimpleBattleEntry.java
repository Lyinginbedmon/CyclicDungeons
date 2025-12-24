package com.lying.blueprint.processor.battle;

import com.lying.blueprint.processor.BattleRoomProcessor.BattleEntry;
import com.lying.grammar.RoomMetadata;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;

/** Spawns a group of all the same mob */
public class SimpleBattleEntry<T extends MobEntity> extends BattleEntry
{
	private final EntityType<T> type;
	private final int maxCount, minCount;
	
	public SimpleBattleEntry(Identifier name, EntityType<T> typeIn, int min, int max)
	{
		super(name);
		type = typeIn;
		minCount = min;
		maxCount = max;
	}
	
	public SimpleBattleEntry(Identifier name, EntityType<T> typeIn, int count)
	{
		this(name, typeIn, count, count);
	}
	
	public void apply(BlockPos min, BlockPos max, ServerWorld world, RoomMetadata meta)
	{
		Random rand = world.random;
		final int mobs = rand.nextBetween(minCount, maxCount);
		for(int i=mobs; i>0; --i)
		{
			BlockPos pos = findSpawnablePosition(type, min, max, world, rand, SEARCH_ATTEMPTS);
			if(pos != null)
				trySpawn(type, pos, world);
		}
	}
}
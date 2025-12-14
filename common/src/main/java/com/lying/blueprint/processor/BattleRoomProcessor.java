package com.lying.blueprint.processor;

import com.lying.blueprint.processor.BattleRoomProcessor.BattleEntry;
import com.lying.init.CDThemes.Theme;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnLocationTypes;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.GlobalPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.Heightmap;

public class BattleRoomProcessor extends RegistryRoomProcessor<BattleEntry>
{
	public void buildRegistry(Theme theme)
	{
		register("zombie_crowd", new SimpleBattleEntry<>(EntityType.ZOMBIE, 4, 8));
		register("husk_crowd", new SimpleBattleEntry<>(EntityType.HUSK, 3, 5));
		register("coven", new SimpleBattleEntry<>(EntityType.WITCH, 2, 3));
	}
	
	protected abstract class BattleEntry implements IProcessorEntry
	{
		protected static <T extends Entity> boolean canPlaceAt(BlockPos pos, ServerWorld world, EntityType<T> type)
		{
			if(!SpawnLocationTypes.ON_GROUND.isSpawnPositionOk(world, pos, type))
				return false;
			
			if(!Heightmap.Type.MOTION_BLOCKING_NO_LEAVES.getBlockPredicate().test(world.getBlockState(pos.down())))
				return false;
			
			if(!world.isSpaceEmpty(type.getSpawnBox(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D)))
				return false;
			
			Entity entity = type.create(world, SpawnReason.STRUCTURE);
			entity.refreshPositionAndAngles(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, 0, 0);
			
			if(entity instanceof MobEntity)
			{
				MobEntity mob = (MobEntity)entity;
				if(!mob.canSpawn(world))
					return false;
				
				if(!mob.canSpawn(world, SpawnReason.STRUCTURE))
					return false;
			}
			
			return true;
		}
		
		protected static <T extends Entity> boolean trySpawn(EntityType<T> type, BlockPos pos, ServerWorld world)
		{
			Entity entity = type.create(world, SpawnReason.STRUCTURE);
			entity.refreshPositionAndAngles(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, 360F * world.random.nextFloat(), 0);
			if(entity instanceof MobEntity)
			{
				MobEntity mob = (MobEntity)entity;
				mob.setPersistent();
				mob.initialize(world, world.getLocalDifficulty(pos), SpawnReason.STRUCTURE, null);
				
				// Where possible, set the mob's home position to encourage them to stay in the room
				if(mob.getBrain().hasMemoryModule(MemoryModuleType.HOME))
					mob.getBrain().remember(MemoryModuleType.HOME, new GlobalPos(world.getRegistryKey(), new BlockPos(pos)));
				
				if(world.spawnNewEntityAndPassengers(entity))
					return true;
			}
			return false;
		}
	}
	
	/** Spawns a group of all the same mob */
	public class SimpleBattleEntry<T extends MobEntity> extends BattleEntry
	{
		private final EntityType<T> type;
		private final int maxCount, minCount;
		
		public SimpleBattleEntry(EntityType<T> typeIn, int min, int max)
		{
			type = typeIn;
			minCount = min;
			maxCount = max;
		}
		
		public void apply(BlockPos min, BlockPos max, ServerWorld world)
		{
			Random rand = world.random;
			final int mobs = rand.nextBetween(minCount, maxCount);
			for(int i=mobs; i>0; --i)
			{
				int searchAttempts = 20;
				BlockPos pos;
				do
				{
					pos = new BlockPos(
						rand.nextBetween(min.getX() + 1, max.getX() - 1), 
						rand.nextBetween(min.getY() + 1, max.getY() - 1), 
						rand.nextBetween(min.getZ() + 1, max.getZ() - 1));
				}
				while(--searchAttempts > 0 && !canPlaceAt(pos, world, type));
				if(!canPlaceAt(pos, world, type))
					continue;
				
				trySpawn(type, pos, world);
			}
		}
	}
}

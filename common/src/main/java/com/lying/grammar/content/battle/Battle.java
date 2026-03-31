package com.lying.grammar.content.battle;

import java.util.Optional;

import org.jetbrains.annotations.Nullable;

import com.lying.CyclicDungeons;
import com.lying.grammar.RoomMetadata;
import com.lying.init.CDBattleTypes;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnLocationTypes;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.GlobalPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.Heightmap;

public abstract class Battle
{
	public static final Codec<Battle> CODEC	= RecordCodecBuilder.create(instance -> instance.group(
			Identifier.CODEC.fieldOf("battle_type").forGetter(Battle::registryName),
			NbtCompound.CODEC.optionalFieldOf("settings").forGetter(Battle::getConfig)
			).apply(instance, (id,nbt) -> 
			{
				Battle type = CDBattleTypes.get(id).orElse(null);
				if(type == null)
				{
					CyclicDungeons.LOGGER.error(" # Unrecognised battle entry type {}", id.toString());
					return null;
				}
				
				if(nbt.isPresent())
				{
					type = type.readConfig(nbt.get());
					if(type == null)
						CyclicDungeons.LOGGER.error(" # Error whilst reading battle of type {}", id.toString());
				}
				return type;
			}));
	protected static final int SEARCH_ATTEMPTS = 20;
	protected final Identifier typeName;
	
	protected Battle(Identifier idIn)
	{
		typeName = idIn;
	}
	
	public Identifier registryName() { return typeName; }
	
	public abstract Text describe();
	
	public abstract void apply(BlockPos min, BlockPos max, ServerWorld world, RoomMetadata meta);
	
	@Nullable
	protected static BlockPos findSpawnablePosition(EntityType<? extends Entity> type, BlockPos min, BlockPos max, ServerWorld world, Random rand, int searchAttempts)
	{
		BlockPos pos;
		boolean isValid = false;
		do
		{
			pos = new BlockPos(
				rand.nextBetween(min.getX() + 1, max.getX() - 1), 
				rand.nextBetween(min.getY() + 1, max.getY() - 1), 
				rand.nextBetween(min.getZ() + 1, max.getZ() - 1));
			isValid = canPlaceAt(pos, world, type);
		}
		while(--searchAttempts > 0 && !isValid);
		return isValid ? pos : null;
	}
	
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
				mob.getBrain().remember(MemoryModuleType.HOME, new GlobalPos(world.getRegistryKey(), pos));
			
			if(world.spawnNewEntityAndPassengers(entity))
				return true;
		}
		return false;
	}
	
	public Optional<NbtCompound> getConfig()
	{
		NbtCompound nbt = writeConfig(new NbtCompound());
		return nbt.isEmpty() ? Optional.empty() : Optional.of(nbt);
	}
	
	/** Stores all configurable variables on the given NBT object */
	public abstract NbtCompound writeConfig(NbtCompound nbt);
	
	/** Loads all configurable variables from the given NBT object */
	public abstract Battle readConfig(NbtCompound nbt);
}
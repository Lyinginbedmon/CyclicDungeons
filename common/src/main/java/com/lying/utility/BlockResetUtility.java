package com.lying.utility;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.common.collect.Lists;
import com.lying.reference.Reference;

import net.minecraft.block.BlockState;
import net.minecraft.entity.LivingEntity;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;

public class BlockResetUtility
{
	// FIXME Ensure log can be read & written
	private Map<RegistryKey<World>,List<ResetRecord>> RESET_LOG = new HashMap<>();
	
	public List<ResetRecord> getRecordsFor(RegistryKey<World> keyIn)
	{
		return RESET_LOG.getOrDefault(keyIn, Lists.newArrayList());
	}
	
	public void logBlockToReset(BlockState blockIn, BlockPos posIn, RegistryKey<World> worldIn, boolean ifPlaceable)
	{
		logBlockToReset(blockIn, posIn, worldIn, ifPlaceable, 3);
	}
	
	public void logBlockToReset(BlockState blockIn, BlockPos posIn, RegistryKey<World> worldIn, boolean ifPlaceable, int initialCount)
	{
		List<ResetRecord> records = getRecordsFor(worldIn);
		records.add(new ResetRecord(blockIn, posIn, worldIn, ifPlaceable).setCount(initialCount));
		RESET_LOG.put(worldIn, records);
	}
	
	public void tickWorld(ServerWorld worldIn)
	{
		Random rand = worldIn.getRandom();
		if(rand.nextInt(Reference.Values.TICKS_PER_SECOND) > 0)
			return;
		
		List<ResetRecord> records = getRecordsFor(worldIn.getRegistryKey());
		if(records.isEmpty())
			return;
		
		boolean dirty = false;
		final int checked = records.size() == 1 ? 1 : rand.nextInt(records.size());
		for(int i=checked; i>0; i--)
		{
			int index = rand.nextInt(records.size());
			ResetRecord record = records.get(index);
			if(record.canDecrement(worldIn))
				record.val--;
			else if(record.canReset(worldIn))
			{
				record.reset(worldIn);
				records.remove(index);
				dirty = true;
			}
		}
		
		if(dirty)
			RESET_LOG.put(worldIn.getRegistryKey(), records);
	}
	
	private static class ResetRecord
	{
		private final BlockState block;
		private final BlockPos pos;
		private final RegistryKey<World> worldKey;
		private final boolean checkPlaceable;
		
		private int val = 3;
		
		public ResetRecord(BlockState blockIn, BlockPos posIn, RegistryKey<World> worldIn, boolean ifPlaceable)
		{
			block = blockIn;
			pos = posIn;
			worldKey = worldIn;
			checkPlaceable = ifPlaceable;
		}
		
		public ResetRecord setCount(int value)
		{
			val = value;
			return this;
		}
		
		@SuppressWarnings("deprecation")
		public boolean canDecrement(World world)
		{
			if(world.getRegistryKey() != worldKey)
				return false;
			
			BlockState state = world.getBlockState(pos);
			if(!(state.isAir() || state.isLiquid() || state.isReplaceable()) || state.isIn(BlockTags.FIRE))
				return false;
			
			if(checkPlaceable && !block.canPlaceAt(world, pos))
				return false;
			
			return val > 0;
		}
		
		public boolean canReset(World world)
		{
			if(world.getRegistryKey() != worldKey)
				return false;
			else if(checkPlaceable && !block.canPlaceAt(world, pos))
				return false;
			
			Box bounds = Box.enclosing(pos, pos);
			if(!world.getEntitiesByClass(LivingEntity.class, bounds, EntityPredicates.EXCEPT_SPECTATOR).isEmpty())
				return false;
			
			return val <= 0;
		}
		
		public void reset(World world)
		{
			world.setBlockState(pos, block, 3);
		}
	}
}

package com.lying.utility;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.common.collect.Lists;
import com.lying.reference.Reference;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.block.BlockState;
import net.minecraft.datafixer.DataFixTypes;
import net.minecraft.entity.LivingEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtOps;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryWrapper.WrapperLookup;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.PersistentState;
import net.minecraft.world.PersistentStateManager;
import net.minecraft.world.World;

public class BlockResetUtility extends PersistentState
{
	private static final String BLOCKS	= "block_reset";
	public static final PersistentState.Type<BlockResetUtility> TYPE = new PersistentState.Type<BlockResetUtility>(BlockResetUtility::new, (nbt, registries) -> createFromNbt(nbt), DataFixTypes.LEVEL);
	public static final Codec<BlockResetUtility> CODEC	= ResetRecord.CODEC.listOf().comapFlatMap(l -> 
	{
		BlockResetUtility utility = new BlockResetUtility();
		l.forEach(utility::add);
		return DataResult.success(utility);
	},
	u -> 
	{
		List<ResetRecord> records = Lists.newArrayList();
		u.resetLog.values().forEach(records::addAll);
		return records;
	});
	
	private final Map<RegistryKey<World>,List<ResetRecord>> resetLog = new HashMap<>();
	
	public static BlockResetUtility getBlockResetUtility(MinecraftServer server)
	{
		ServerWorld world = server.getWorld(World.OVERWORLD);
		PersistentStateManager manager = world.getPersistentStateManager();
		return manager.getOrCreate(BlockResetUtility.TYPE, String.join("_", Reference.ModInfo.MOD_ID, BLOCKS));
	}
	
	public static BlockResetUtility createFromNbt(NbtCompound nbt)
	{
		return CODEC.parse(NbtOps.INSTANCE, nbt.get("Records")).getOrThrow();
	}
	
	public NbtCompound writeNbt(NbtCompound nbt, WrapperLookup registries)
	{
		nbt.put("Records", CODEC.encodeStart(NbtOps.INSTANCE, this).getOrThrow());
		return nbt;
	}
	
	public List<ResetRecord> getRecordsFor(RegistryKey<World> keyIn)
	{
		return resetLog.getOrDefault(keyIn, Lists.newArrayList());
	}
	
	public void logBlockToReset(BlockState blockIn, BlockPos posIn, RegistryKey<World> worldIn, boolean ifPlaceable)
	{
		logBlockToReset(blockIn, posIn, worldIn, ifPlaceable, 3);
	}
	
	public void logBlockToReset(BlockState blockIn, BlockPos posIn, RegistryKey<World> worldIn, boolean ifPlaceable, int initialCount)
	{
		add(new ResetRecord(blockIn, posIn, worldIn, ifPlaceable).setCount(initialCount));
	}
	
	private void add(ResetRecord record)
	{
		List<ResetRecord> records = getRecordsFor(record.worldKey);
		records.add(record);
		resetLog.put(record.worldKey, records);
		markDirty();
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
			{
				record.val--;
				dirty = true;
			}
			else if(record.canReset(worldIn))
			{
				record.reset(worldIn);
				records.remove(index);
				dirty = true;
			}
		}
		
		if(dirty)
		{
			resetLog.put(worldIn.getRegistryKey(), records);
			markDirty();
		}
	}
	
	private static class ResetRecord
	{
		private static final Codec<ResetRecord> CODEC	= RecordCodecBuilder.create(instance -> instance.group(
				BlockState.CODEC.fieldOf("State").forGetter(r -> r.block),
				BlockPos.CODEC.fieldOf("Pos").forGetter(r -> r.pos),
				World.CODEC.fieldOf("World").forGetter(r -> r.worldKey),
				Codec.BOOL.fieldOf("CheckPlaceable").forGetter(r -> r.checkPlaceable),
				Codec.INT.fieldOf("Timer").forGetter(r -> r.val)
				).apply(instance, (state,pos,world,placeable,val) -> 
				{
					ResetRecord rec = new ResetRecord(state,pos,world,placeable);
					rec.setCount(val);
					return rec;
				}));
		
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

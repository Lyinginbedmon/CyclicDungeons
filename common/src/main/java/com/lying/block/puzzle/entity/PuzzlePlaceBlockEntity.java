package com.lying.block.puzzle.entity;

import java.util.Optional;

import com.lying.block.puzzle.PuzzleBreakBlock;
import com.lying.init.CDBlockEntityTypes;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtOps;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class PuzzlePlaceBlockEntity extends PuzzleBreakBlockEntity
{
	private BlockState state = Blocks.STONE.getDefaultState();
	private Optional<NbtCompound> tileData = Optional.empty();
	
	public PuzzlePlaceBlockEntity(BlockPos pos, BlockState state)
	{
		super(CDBlockEntityTypes.PUZZLE_PLACE.get(), pos, state);
	}
	
	protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup)
	{
		super.writeNbt(nbt, registryLookup);
		
		nbt.put("Block", BlockState.CODEC.encodeStart(NbtOps.INSTANCE, state).getOrThrow());
		tileData.ifPresent(n -> nbt.put("TileData", n));
	}
	
	protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup)
	{
		super.readNbt(nbt, registryLookup);
		
		state = BlockState.CODEC.parse(NbtOps.INSTANCE, nbt.get("Block")).getOrThrow();
		tileData = nbt.contains("TileData") ? Optional.of(nbt.getCompound("TileData")) : Optional.empty();
	}
	
	public static <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type)
	{
		return type != CDBlockEntityTypes.PUZZLE_PLACE.get() ? 
				null : 
				PuzzleBreakBlock.validateTicker(type, CDBlockEntityTypes.PUZZLE_PLACE.get(), 
					world.isClient() ? 
						PuzzleBreakBlockEntity::tickClient : 
						PuzzleBreakBlockEntity::tickServer);
	}
	
	protected void onTrigger()
	{
		final BlockPos pos = getPos();
		world.setBlockState(pos, state);
		tileData.ifPresent(nbt -> 
		{
			BlockEntity tile = world.getBlockEntity(pos);
			if(tile != null)
				tile.read(nbt, world.getRegistryManager());
		});
	}
}

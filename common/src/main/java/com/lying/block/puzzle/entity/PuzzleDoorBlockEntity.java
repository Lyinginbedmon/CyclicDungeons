package com.lying.block.puzzle.entity;

import com.lying.block.puzzle.PuzzleDoorBlock;
import com.lying.init.CDBlockEntityTypes;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.DoorBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class PuzzleDoorBlockEntity extends PuzzleBreakBlockEntity
{
	public PuzzleDoorBlockEntity(BlockPos pos, BlockState state)
	{
		super(CDBlockEntityTypes.PUZZLE_DOOR.get(), pos, state);
	}
	
	protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup)
	{
		super.writeNbt(nbt, registryLookup);
	}
	
	protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup)
	{
		super.readNbt(nbt, registryLookup);
	}
	
	public static <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type)
	{
		return type != CDBlockEntityTypes.PUZZLE_DOOR.get() ? 
				null : 
				PuzzleDoorBlock.validateTicker(type, CDBlockEntityTypes.PUZZLE_DOOR.get(), 
					world.isClient() ? 
						PuzzleBreakBlockEntity::tickClient : 
						PuzzleBreakBlockEntity::tickServer);
	}
	
	protected void onTrigger()
	{
		if(getCachedState().get(PuzzleDoorBlock.HALF) != DoubleBlockHalf.LOWER)
			return;
		
		final BlockPos pos = getPos();
		final BlockState reference = getCachedState();
		BlockState state = Blocks.OAK_DOOR.getDefaultState()
				.with(DoorBlock.FACING, reference.get(DoorBlock.FACING))
				.with(DoorBlock.HINGE, reference.get(DoorBlock.HINGE))
				.with(DoorBlock.OPEN, true);
		world.setBlockState(pos, state.with(DoorBlock.HALF, DoubleBlockHalf.LOWER));
		world.setBlockState(pos.up(), state.with(DoorBlock.HALF, DoubleBlockHalf.UPPER));
	}
}

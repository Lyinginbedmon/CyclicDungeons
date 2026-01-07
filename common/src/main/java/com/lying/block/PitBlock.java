package com.lying.block;

import com.lying.init.CDBlocks;
import com.lying.init.CDItems;

import dev.architectury.event.events.common.TickEvent;
import net.minecraft.block.Block;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.SideShapeType;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;
import net.minecraft.world.tick.ScheduledTickView;

public class PitBlock extends Block
{
	public PitBlock(Settings settings)
	{
		super(settings.nonOpaque().noCollision());
	}
	
	public static void registerEvent()
	{
		TickEvent.PLAYER_POST.register(p -> 
		{
			World world = p.getWorld();
			if(!world.isClient() && world.getBlockState(p.getBlockPos()).isOf(CDBlocks.PIT.get()))
				p.damage((ServerWorld)world, p.getDamageSources().fall(), 4F);
		});
	}
	
	protected VoxelShape getOutlineShape(BlockState state, BlockView World, BlockPos pos, ShapeContext context)
	{
		if(CDItems.PIT.isPresent())
			return context.isHolding(CDItems.PIT.get()) ? VoxelShapes.fullCube() : VoxelShapes.empty();
		return VoxelShapes.fullCube();
	}
	
	protected BlockRenderType getRenderType(BlockState state)
	{
		return BlockRenderType.INVISIBLE;
	}
	
	protected float getAmbientOcclusionLightLevel(BlockState state, BlockView world, BlockPos pos) {
		return 0.0F;
	}
	
	protected boolean canPlaceAt(BlockState state, WorldView world, BlockPos pos)
	{
		return world.getBlockState(pos.down()).isSideSolid(world, pos.down(), Direction.UP, SideShapeType.FULL);
	}
	
	@Override
	protected BlockState getStateForNeighborUpdate(
			BlockState state,
			WorldView world,
			ScheduledTickView tickView,
			BlockPos pos,
			Direction direction,
			BlockPos neighborPos,
			BlockState neighborState,
			Random random
		)
	{
		return !this.canPlaceAt(state, world, pos)
			? Blocks.AIR.getDefaultState()
			: super.getStateForNeighborUpdate(state, world, tickView, pos, direction, neighborPos, neighborState, random);
	}
}

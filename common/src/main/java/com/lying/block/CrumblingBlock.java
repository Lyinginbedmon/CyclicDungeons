package com.lying.block;

import java.util.function.Supplier;

import org.joml.Math;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.SideShapeType;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;
import net.minecraft.world.tick.ScheduledTickView;

public class CrumblingBlock extends Block
{
	public static final double VEL_MIN	= 0.003D;
	private final Supplier<Block> emulated;
	
	public CrumblingBlock(Settings settings, Supplier<Block> emulatedIn)
	{
		super(settings);
		emulated = emulatedIn;
	}
	
	public Block getEmulated() { return emulated.get(); }
	
	protected boolean canPlaceAt(BlockState state, WorldView world, BlockPos pos)
	{
		return Direction.stream().anyMatch(f -> 
		{
			BlockPos neighbour = pos.offset(f);
			BlockState stateAt = world.getBlockState(neighbour);
			return !stateAt.isAir() && stateAt.isSideSolid(world, neighbour, f.getOpposite(), SideShapeType.FULL);
		});
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
	
	public void onSteppedOn(World world, BlockPos pos, BlockState state, Entity entity)
	{
		tryBreak(world, pos, entity);
		super.onSteppedOn(world, pos, state, entity);
	}
	
	protected void tryBreak(World world, BlockPos pos, Entity entity)
	{
		if(world.isClient() || entity.bypassesSteppingEffects())
			return;
		
		Vec3d vec3d = entity.isControlledByPlayer() ? entity.getMovement() : entity.getLastRenderPos().subtract(entity.getPos());
		if(vec3d.horizontalLengthSquared() == 0D)
			return;
		
		double xLen = Math.abs(vec3d.getX());
		double zLen = Math.abs(vec3d.getZ());
		if(xLen < VEL_MIN && zLen < VEL_MIN)
			return;
		
		if(world.getRandom().nextInt(entity.isSprinting() ? 2 : 5) == 0)
		{
			recursiveBreak(world, pos, 4);
			return;
		}
	}
	
	protected void recursiveBreak(World world, BlockPos pos, int iteration)
	{
		crumble(world, pos);
		if(--iteration > 0)
			for(Direction face : Direction.values())
			{
				BlockPos offset = pos.offset(face);
				if(world.getBlockState(offset).getBlock() instanceof CrumblingBlock && world.getRandom().nextInt(3) == 0)
					recursiveBreak(world, offset, iteration);
			}
	}
	
	protected void crumble(World world, BlockPos pos)
	{
		world.breakBlock(pos, false);
	}
}

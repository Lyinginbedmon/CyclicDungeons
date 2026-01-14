package com.lying.block;

import java.util.function.Supplier;

import org.joml.Math;

import com.lying.init.CDItems;

import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.block.Block;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.SideShapeType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.Item;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.IntProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;
import net.minecraft.world.tick.ScheduledTickView;

public class CrumblingBlock extends Block
{
	public static final double VEL_MIN	= 0.003D;
	protected final Supplier<Block> emulated;
	
	public CrumblingBlock(Supplier<Block> emulatedIn, Settings settings)
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
	
	public boolean isBroken(BlockState state)
	{
		return !(state.getBlock() instanceof CrumblingBlock);
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
				BlockState state = world.getBlockState(offset);
				if(state.getBlock() instanceof CrumblingBlock && !((CrumblingBlock)state.getBlock()).isBroken(state) && world.getRandom().nextInt(3) == 0)
					((CrumblingBlock)state.getBlock()).recursiveBreak(world, offset, iteration);
			}
	}
	
	protected void crumble(World world, BlockPos pos)
	{
		// FIXME Ensure block only dropped when mined by entity
		world.breakBlock(pos, false);
	}
	
	public static class Resetting extends CrumblingBlock
	{
		public static final IntProperty BREAK	= Properties.AGE_3;
		
		public Resetting(Supplier<Block> emulatedIn, Settings settings)
		{
			super(emulatedIn, settings.ticksRandomly());
			setDefaultState(getDefaultState().with(BREAK, 0));
		}
		
		protected boolean hasRandomTicks(BlockState state) { return state.get(BREAK) > 0; }
		
		protected BlockRenderType getRenderType(BlockState state)
		{
			return isBroken(state) ? BlockRenderType.INVISIBLE : BlockRenderType.MODEL;
		}
		
		protected VoxelShape getCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context)
		{
			return isBroken(state) ? VoxelShapes.empty() : VoxelShapes.fullCube();
		}
		
		protected VoxelShape getOutlineShape(BlockState state, BlockView World, BlockPos pos, ShapeContext context)
		{
			RegistrySupplier<Item> item = CDItems.getBlockItem(state.getBlock());
			if(item.isPresent() && context.isHolding(item.get()))
				return VoxelShapes.fullCube();
			
			return isBroken(state) ? VoxelShapes.empty() : VoxelShapes.fullCube();
		}
		
		protected boolean isShapeFullCube(BlockState state, BlockView world, BlockPos pos)
		{
			return isBroken(state) ? false : super.isShapeFullCube(state, world, pos);
		}
		
		public boolean isBroken(BlockState state)
		{
			return state.get(BREAK) > 0;
		}
		
		protected void randomTick(BlockState state, ServerWorld world, BlockPos pos, Random random)
		{
			if(isBroken(state) && state.canPlaceAt(world, pos))
			{
				int val = state.get(BREAK) - 1;
				if(val == 0)
				{
					// If this decrement would restore the block, check we're not going to suffocate someone first
					Box bounds = Box.enclosing(pos, pos);
					if(!world.getEntitiesByClass(LivingEntity.class, bounds, EntityPredicates.EXCEPT_SPECTATOR).isEmpty())
						return;
				}
				
				world.setBlockState(pos, state.with(BREAK, val), 3);
			}
		}
		
		protected void crumble(World world, BlockPos pos)
		{
			BlockState state = world.getBlockState(pos);
			world.breakBlock(pos, false);
			world.setBlockState(pos, state.with(BREAK, 3), 3);
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
				? this.getDefaultState().with(BREAK, 3)
				: super.getStateForNeighborUpdate(state, world, tickView, pos, direction, neighborPos, neighborState, random);
		}
		
		protected void appendProperties(StateManager.Builder<Block, BlockState> builder)
		{
			builder.add(BREAK);
		}
	}
}

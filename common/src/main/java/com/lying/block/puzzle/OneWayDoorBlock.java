package com.lying.block.puzzle;

import org.jetbrains.annotations.Nullable;

import com.lying.block.puzzle.entity.OneWayDoorBlockEntity;
import com.mojang.serialization.MapCodec;

import net.minecraft.block.Block;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.Blocks;
import net.minecraft.block.EntityShapeContext;
import net.minecraft.block.HorizontalFacingBlock;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.state.StateManager.Builder;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.BlockMirror;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Direction.Axis;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

public class OneWayDoorBlock extends BlockWithEntity
{
	public static final MapCodec<OneWayDoorBlock> CODEC = OneWayDoorBlock.createCodec(OneWayDoorBlock::new);
	public static final EnumProperty<Direction> FACING = HorizontalFacingBlock.FACING;
	public static final EnumProperty<DoubleBlockHalf> HALF	= Properties.DOUBLE_BLOCK_HALF;
	public static final BooleanProperty OPEN	= Properties.OPEN;
	
	public static final VoxelShape 
		SHAPE_X = Block.createCuboidShape(3, 0, 0, 13, 16, 16), 
		SHAPE_Z = Block.createCuboidShape(0, 0, 3, 16, 16, 13);
	
	public OneWayDoorBlock(Settings settings)
	{
		super(settings);
		setDefaultState(getDefaultState()
				.with(FACING, Direction.NORTH)
				.with(HALF, DoubleBlockHalf.LOWER)
				.with(OPEN, false));
	}
	
	@Override
	protected void appendProperties(Builder<Block, BlockState> builder)
	{
		builder.add(OPEN, FACING, HALF);
	}
	
	protected BlockRenderType getRenderType(BlockState state)
	{
		return state.get(OPEN) ? BlockRenderType.INVISIBLE : BlockRenderType.MODEL;
	}
	
	public BlockState getPlacementState(ItemPlacementContext ctx)
	{
		return getDefaultState().with(FACING, ctx.getHorizontalPlayerFacing());
	}
	
	protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context)
	{
		return state.get(FACING).getAxis() == Axis.X ? SHAPE_X : SHAPE_Z;
	}
	
	protected VoxelShape getCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context)
	{
		if(state.get(OPEN))
			return VoxelShapes.empty();
		else if(!(context instanceof EntityShapeContext))
			return getOutlineShape(state, world, pos, context);
		
		EntityShapeContext ctx = (EntityShapeContext)context;
		if(ctx.getEntity() == null)
			return getOutlineShape(state, world, pos, context);
		
		Direction facing = state.get(FACING);
		BlockPos entPos = ctx.getEntity().getBlockPos();
		if(entPos.getManhattanDistance(pos) == 0 || entPos.getY() != pos.getY())
			return VoxelShapes.empty();
		
		BlockPos offset = pos.subtract(entPos);
		Vec3i target = facing.getVector();
		
		int valE = facing.getAxis() == Axis.X ? offset.getX() : offset.getZ();
		int valB = facing.getAxis() == Axis.X ? target.getX() : target.getZ();
		if(Math.signum(valE) == Math.signum(valB))
			return VoxelShapes.empty();
		else
			return getOutlineShape(state, world, pos, context);
	}
	
	protected void onEntityCollision(BlockState state, World world, BlockPos pos, Entity entity)
	{
		if(state.get(OPEN) || !(entity instanceof LivingEntity))
			return;
		
		Direction facing = state.get(FACING);
		entity.slowMovement(state, new Vec3d(
				facing.getAxis() == Axis.X ? 0.95D : 1D, 
				1, 
				facing.getAxis() == Axis.Z ? 0.95D : 1D
				));
	}
	
	public void onPlaced(World world, BlockPos pos, BlockState state, LivingEntity placer, ItemStack itemStack)
	{
		world.setBlockState(pos.up(), state.with(HALF, DoubleBlockHalf.UPPER), 3);
	}
	
	public BlockState onBreak(World world, BlockPos pos, BlockState state, PlayerEntity player)
	{
		if (!world.isClient && (player.isCreative() || !player.canHarvest(state)))
		{
			BlockPos neighbour = state.get(HALF) == DoubleBlockHalf.LOWER ? pos.up() : pos.down();
			if(world.getBlockState(neighbour).getBlock() == state.getBlock())
			{
				world.setBlockState(neighbour, Blocks.AIR.getDefaultState());
				world.syncWorldEvent(player, 2001, neighbour, Block.getRawIdFromState(world.getBlockState(neighbour)));
			}
		}
		
		return super.onBreak(world, pos, state, player);
	}
	
	public BlockEntity createBlockEntity(BlockPos pos, BlockState state)
	{
		return state.get(HALF) == DoubleBlockHalf.UPPER ? null : new OneWayDoorBlockEntity(pos, state);
	}
	
	@SuppressWarnings("unchecked")
	@Nullable
	public static <E extends BlockEntity, A extends BlockEntity> BlockEntityTicker<A> validateTicker(BlockEntityType<A> given, BlockEntityType<E> expected, BlockEntityTicker<? super E> ticker)
	{
		return expected == given ? (BlockEntityTicker<A>)ticker : null;
	}
	
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type)
	{
		return OneWayDoorBlockEntity.getTicker(world, state, type);
	}
	
	protected MapCodec<? extends BlockWithEntity> getCodec() { return CODEC; }
	
	protected BlockState rotate(BlockState state, BlockRotation rotation)
	{
		return state.with(FACING, rotation.rotate(state.get(FACING)));
	}
	
	protected BlockState mirror(BlockState state, BlockMirror mirror)
	{
		return state.rotate(mirror.getRotation(state.get(FACING)));
	}
	
	public void randomDisplayTick(BlockState state, World world, BlockPos pos, Random random)
	{
		final double pix = 1 / 16D;
		final double offset = 13 * pix;
		final double offset2 = random.nextDouble() * pix * 3D;
		
		double posX = 0D;
		double posY = (double)pos.getY() + random.nextDouble();
		double posZ = 0D;
		
		if(state.get(FACING).getAxis() == Axis.X)
		{
			if(random.nextBoolean())
				posX = (double)pos.getX() + offset2;
			else
				posX = (double)pos.getX() + offset + offset2;
			
			posZ = (double)pos.getZ() + random.nextDouble();
		}
		else
		{
			if(random.nextBoolean())
				posZ = (double)pos.getZ() + offset2;
			else
				posZ = (double)pos.getZ() + offset + offset2;
			
			posX = (double)pos.getX() + random.nextDouble();
		}
		
		// FIXME Replace with custom mist/fog particle
		world.addParticle(ParticleTypes.LARGE_SMOKE, posX, posY, posZ, 0, 0, 0);
	}
	
	protected void onBlockBreakStart(BlockState state, World world, BlockPos pos, PlayerEntity player)
	{
		if(!state.get(OPEN) || world.isClient)
			return;
		
		// FIXME Break instantly when OPEN, incl. second block
	}
}

package com.lying.block.entity;

import java.util.List;

import com.google.common.collect.Lists;
import com.lying.block.IWireableBlock;
import com.lying.block.SpikeTrapBlock;
import com.lying.block.SpikesBlock;
import com.lying.block.SpikesBlock.SpikePart;
import com.lying.init.CDBlockEntityTypes;
import com.lying.init.CDBlocks;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.block.piston.PistonBehavior;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MovementType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Boxes;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.World;

public class SpikeTrapBlockEntity extends TrapActorBlockEntity
{
	/** How quickly the spikes extend from the trap */
	public static final float EXTEND_RATE = 0.4F;
	/** How quickly the spikes retract back into the trap */
	public static final float RETRACT_RATE = 0.15F;
	/** Maximum block length the trap can extend, limited by block bounding box maximums */
	public static final float FULL_SIZE	= 2F;
	
	protected float extension = 0F;
	protected float prevExtension = 0F;
	
	protected VoxelShape extendedBounds = VoxelShapes.empty();
	
	public SpikeTrapBlockEntity(BlockPos pos, BlockState state)
	{
		super(CDBlockEntityTypes.SPIKE_TRAP.get(), pos, state);
	}
	
	protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup)
	{
		super.writeNbt(nbt, registryLookup);
		nbt.putFloat("Extension", extension);
	}
	
	protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup)
	{
		super.readNbt(nbt, registryLookup);
		setExtension(nbt.getFloat("Extension"));
	}
	
	public static <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type)
	{
		return type != CDBlockEntityTypes.SPIKE_TRAP.get() ? 
				null : 
				IWireableBlock.validateTicker(type, CDBlockEntityTypes.SPIKE_TRAP.get(), world.isClient() ? 
					SpikeTrapBlockEntity::tickClient : 
					SpikeTrapBlockEntity::tickServer);
	}
	
	public static <T extends BlockEntity> void tickClient(World world, BlockPos pos, BlockState state, SpikeTrapBlockEntity tile)
	{
		tile.handleExtension(pos, world, tile);
	}
	
	public static <T extends BlockEntity> void tickServer(World world, BlockPos pos, BlockState state, SpikeTrapBlockEntity tile)
	{
		TrapActorBlockEntity.tickServer(world, pos, state, tile);
		tile.handleExtension(pos, world, tile);
	}
	
	protected void updateExtension(boolean val)
	{
		setExtension(extension + (val ? +EXTEND_RATE : -RETRACT_RATE));
	}
	
	protected void setExtension(float amount)
	{
		final float prevExtension = extension;
		extension = Math.clamp(amount, 0F, 1F);
		
		if(prevExtension != extension)
			extendedBounds = calculateExtensionBoundingBox(
					this.getCachedState().get(SpikeTrapBlock.FACING),
					getPos(),
					getWorld(),
					extension * maxExtension()
					);
		
		markDirty();
	}
	
	public float extension(float partialTicks)
	{
		return MathHelper.clamp(extension + (extension - prevExtension) * partialTicks, 0F, 1F);
	}
	
	public Direction facing() { return getWorld().getBlockState(getPos()).get(SpikeTrapBlock.FACING); }
	
	public boolean isActive() { return getWorld().getBlockState(getPos()).get(SpikeTrapBlock.POWERED); }
	
	public float maxExtension() { return MathHelper.clamp(FULL_SIZE, 0.5F, 2F); }
	
	protected void handleExtension(BlockPos pos, World world, SpikeTrapBlockEntity tile)
	{
		prevExtension = extension;
		
		BlockState state = world.getBlockState(pos);
		if(state.get(SpikeTrapBlock.POWERED))
		{
			if(extension < 1F)
			{
				tile.updateExtension(true);
				SpikeTrapBlockEntity.pushEntities(pos, world, state, extension, this);
			}
		}
		else
		{
			if(extension > 0F)
				tile.updateExtension(false);
		}
		
		SpikeTrapBlockEntity.damageEntities(pos, world, state.get(SpikeTrapBlock.FACING), extension, tile);
	}
	
	public VoxelShape getExtensionShape()
	{
		return extendedBounds;
	}
	
	protected static VoxelShape calculateExtensionBoundingBox(Direction facing, BlockPos pos, World world, final float height)
	{
		if(height > 0F)
		{
			float length = height - 1F;
			final Vec3d offset = new Vec3d(facing.getOffsetX(), facing.getOffsetY(), facing.getOffsetZ());
			
			final BlockState tipState = CDBlocks.SPIKES.get().getDefaultState().with(SpikesBlock.FACING, facing);
			final VoxelShape tipShape = tipState.getCollisionShape(world, pos).offset(offset.multiply(length));
			
			List<VoxelShape> subShapes = Lists.newArrayList();
			final BlockState poleState = tipState.with(SpikesBlock.PART, SpikePart.POLE);
			final VoxelShape poleShape = poleState.getCollisionShape(world, pos);
			while(length-- > 0F)
				subShapes.add(poleShape.offset(offset.multiply(length)));
			
			return VoxelShapes.union(tipShape, subShapes.toArray(new VoxelShape[0]));
		}
		
		return VoxelShapes.empty();
	}
	
	public static void pushEntities(BlockPos pos, World world, BlockState state, float extension, SpikeTrapBlockEntity blockEntity)
	{
		if(extension == 0F)
			return;
		
		final Direction direction = state.get(SpikeTrapBlock.FACING);
		Box box = blockEntity.extendedBounds.getBoundingBox().offset(pos);
		world.getOtherEntities(null, Boxes.stretch(box, direction, extension + EXTEND_RATE).union(box)).stream()
			.filter(e -> e.getPistonBehavior() != PistonBehavior.IGNORE)
			.forEach(entity -> 
				{
					entity.move(
						MovementType.SHULKER_BOX, 
						new Vec3d(
								(box.getLengthX() + 0.01) * direction.getOffsetX(),
								(box.getLengthY() + 0.05) * direction.getOffsetY(),
								(box.getLengthZ() + 0.01) * direction.getOffsetZ()));
				});
	}
	
	public static void damageEntities(BlockPos pos, World world, Direction facing, float extension, SpikeTrapBlockEntity blockEntity)
	{
		if(extension == 0F || world.isClient())
			return;
		
		Box hurtBox = SpikesBlock.getHurtShape(facing)
			.offset(
				pos.getX(), 
				pos.getY(), 
				pos.getZ())
			.offset(
					extension * facing.getOffsetX(), 
					extension * facing.getOffsetY(), 
					extension * facing.getOffsetZ()).getBoundingBox();
		
		world.getOtherEntities(null, hurtBox).stream()
			.filter(e -> e.getPistonBehavior() != PistonBehavior.IGNORE)
			.filter(e -> e instanceof LivingEntity)
			.forEach(entity -> 
				{
					SpikesBlock.skewerEntity((LivingEntity)entity, 4F);
				});
	}
}

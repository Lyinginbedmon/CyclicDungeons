package com.lying.block.entity;

import com.lying.block.IWireableBlock;
import com.lying.block.SpikeTrapBlock;
import com.lying.init.CDBlockEntityTypes;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.block.piston.PistonBehavior;
import net.minecraft.entity.MovementType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Boxes;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Direction.AxisDirection;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class SpikeTrapBlockEntity extends TrapActorBlockEntity
{
	public static final float EXTEND_RATE = 0.4F;
	public static final float RETRACT_RATE = 0.15F;
	public static final float FULL_SIZE	= 2F;
	
	protected float extension = 0F;
	protected float prevExtension = 0F;
	
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
		extension = nbt.getFloat("Extension");
	}
	
	public static <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type)
	{
		return type != CDBlockEntityTypes.SPIKE_TRAP.get() ? 
				null : 
				IWireableBlock.validateTicker(type, CDBlockEntityTypes.SPIKE_TRAP.get(), SpikeTrapBlockEntity::tick);
	}
	
	public static <T extends BlockEntity> void tick(World world, BlockPos pos, BlockState state, SpikeTrapBlockEntity tile)
	{
		TrapActorBlockEntity.tickServer(world, pos, state, tile);
		tile.handleExtension(pos, world, tile);
	}
	
	protected void updateExtension(boolean val)
	{
		extension = Math.clamp(extension + (val ? +EXTEND_RATE : -RETRACT_RATE), 0F, 1F);
		markDirty();
	}
	
	public float extension(float partialTicks)
	{
		return extension + (extension - prevExtension) * partialTicks;
	}
	
	public Direction facing() { return getWorld().getBlockState(getPos()).get(SpikeTrapBlock.FACING); }
	
	public boolean isActive() { return getWorld().getBlockState(getPos()).get(SpikeTrapBlock.POWERED); }
	
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
	}
	
	public Box getBoundingBox(BlockState state)
	{
		Direction facing = state.get(SpikeTrapBlock.FACING);
		return calculateBoundingBox(facing, extension * FULL_SIZE);
	}
	
	public static Box calculateBoundingBox(Direction facing, float height)
	{
		boolean isPos = facing.getDirection() == AxisDirection.POSITIVE;
		double min = isPos ? 0D : 1D - height;
		double max = isPos ? height : 1D;
		switch(facing.getAxis())
		{
			case X:
				return new Box(min, 0D, 0D, max, 1D, 1D);
			default:
			case Y:
				return new Box(0D, min, 0D, 1D, max, 1D);
			case Z:
				return new Box(0D, 0D, min, 1D, 1D, max);
		}
	}
	
	public static void pushEntities(BlockPos pos, World world, BlockState state, float extension, SpikeTrapBlockEntity blockEntity)
	{
		if(extension == 0F)
			return;
		
		final Direction direction = state.get(SpikeTrapBlock.FACING);
		Box box = calculateBoundingBox(direction, extension).offset(pos);
		world.getOtherEntities(null, Boxes.stretch(box, direction, extension + EXTEND_RATE).union(box)).stream()
			.filter(e -> e.getPistonBehavior() != PistonBehavior.IGNORE)
			.forEach(entity -> 
				{
					// TODO Damage entities
					entity.move(
						MovementType.SHULKER_BOX, 
						new Vec3d(
								(box.getLengthX() + 0.01) * direction.getOffsetX(),
								(box.getLengthY() + 0.05) * direction.getOffsetY(),
								(box.getLengthZ() + 0.01) * direction.getOffsetZ()));
				});
	}
}

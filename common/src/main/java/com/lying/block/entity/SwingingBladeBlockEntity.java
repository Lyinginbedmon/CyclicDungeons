package com.lying.block.entity;

import com.lying.block.IWireableBlock.WireRecipient;
import com.lying.block.SwingingBladeBlock;
import com.lying.init.CDBlockEntityTypes;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class SwingingBladeBlockEntity extends AbstractWireableBlockEntity
{
	private float swingProgress = 0F;
	private float swingTarget = 0F;
	private boolean prevPower = false;
	
	public SwingingBladeBlockEntity(BlockPos pos, BlockState state)
	{
		super(CDBlockEntityTypes.SWINGING_BLADE.get(), pos, state);
	}
	
	protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup)
	{
		super.writeNbt(nbt, registryLookup);
		nbt.putFloat("Swing", swingProgress);
		nbt.putFloat("SwingTarget", swingTarget);
		nbt.putBoolean("PrevPower", prevPower);
	}
	
	protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup)
	{
		super.readNbt(nbt, registryLookup);
		swingProgress = nbt.getFloat("Swing");
		swingTarget = nbt.getFloat("SwingTarget");
		prevPower = nbt.getBoolean("PrevPower");
	}
	
	public float swingProgress() { return swingProgress; }
	
	public boolean processWireConnection(BlockPos pos, WireRecipient type)
	{
		if(type != WireRecipient.SENSOR)
			return false;
		
		addWire(pos, type);
		return true;
	}
	
	public static <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type)
	{
		return type != CDBlockEntityTypes.SWINGING_BLADE.get() ? 
				null : 
				SwingingBladeBlock.validateTicker(type, CDBlockEntityTypes.SWINGING_BLADE.get(), 
					world.isClient() ? 
						SwingingBladeBlockEntity::tickClient : 
						SwingingBladeBlockEntity::tickServer);
	}
	
	public static <T extends BlockEntity> void tickClient(World world, BlockPos pos, BlockState state, SwingingBladeBlockEntity tile)
	{
		
	}
	
	public static <T extends BlockEntity> void tickServer(World world, BlockPos pos, BlockState state, SwingingBladeBlockEntity tile)
	{
		
	}
}

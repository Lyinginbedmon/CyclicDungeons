package com.lying.block.entity;

import static com.lying.reference.Reference.ModInfo.prefix;

import com.lying.block.IWireableBlock;
import com.lying.block.IWireableBlock.WireRecipient;
import com.lying.init.CDBlockEntityTypes;
import com.lying.init.CDTrapLogicHandlers;
import com.lying.init.CDTrapLogicHandlers.LogicHandler;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class TrapLogicBlockEntity extends AbstractWireableBlockEntity
{
	private Identifier logicType = prefix("relay");
	private LogicHandler handler = null;
	
	public TrapLogicBlockEntity(BlockPos pos, BlockState state)
	{
		super(CDBlockEntityTypes.TRAP_LOGIC.get(), pos, state);
	}
	
	protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup)
	{
		super.writeNbt(nbt, registryLookup);
		nbt.putString("Logic", logicType.toString());
	}
	
	protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup)
	{
		super.readNbt(nbt, registryLookup);
		logicType = Identifier.of(nbt.getString("Logic"));
	}
	
	public static <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type)
	{
		return type != CDBlockEntityTypes.TRAP_LOGIC.get() ? 
				null : 
				IWireableBlock.validateTicker(type, CDBlockEntityTypes.TRAP_LOGIC.get(), 
					world.isClient() ? 
						TrapLogicBlockEntity::tickClient : 
						TrapLogicBlockEntity::tickServer);
	}
	
	public static <T extends BlockEntity> void tickClient(World world, BlockPos pos, BlockState state, TrapLogicBlockEntity tile) { }
	
	public static <T extends BlockEntity> void tickServer(World world, BlockPos pos, BlockState state, TrapLogicBlockEntity tile)
	{
		tile.cleanActors();
		if(!tile.hasActors())
			return;
		
		tile.cleanSensors();
		tile.getHandler().handleLogic(tile.getSensors(), tile.getActors(), (ServerWorld)world);
	}
	
	public boolean sensorInputState()
	{
		cleanSensors();
		return hasSensors() && getSensors().stream().anyMatch(p -> IWireableBlock.getWireable(p, world).isActive(p, world));
	}
	
	public boolean processWireConnection(BlockPos pos, WireRecipient type)
	{
		if(type == WireRecipient.LOGIC)
			return false;
		
		addWire(pos, type);
		return true;
	}
	
	public TrapLogicBlockEntity setLogic(Identifier idIn)
	{
		logicType = idIn;
		handler = null;
		return this;
	}
	
	public LogicHandler getHandler()
	{
		if(handler == null)
			handler = CDTrapLogicHandlers.get(logicType);
		
		return handler;
	}
}

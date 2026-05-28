package com.lying.block.entity;

import static com.lying.reference.Reference.ModInfo.prefix;

import com.lying.block.IWireableBlock;
import com.lying.block.entity.logic.WiringManifest.ManifestEntry.PortEntry;
import com.lying.init.CDBlockEntityTypes;
import com.lying.init.CDLogicGates;
import com.lying.init.CDTrapLogicHandlers;
import com.lying.init.CDTrapLogicHandlers.LogicHandler;
import com.lying.item.WiringGunItem.WireMode;

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
		tile.respondToPorts();
	}
	
	public void respondToPorts()
	{
		cleanActors();
		if(!hasOutputs())
			return;
		
		cleanSensors();
		getHandler().handleLogic(!hasInputs() ? -1 : sensorInputState() ? 1 : 0, getOutputListeners(), (ServerWorld)world);
	}
	
	protected void resetBlock() { }
	
	public boolean sensorInputState()
	{
		cleanSensors();
		return hasInputs() && getInput(CDLogicGates.INPUT);
	}
	
	public boolean processInputConnection(String input, PortEntry output, WireMode space)
	{
		addInputWire(input, output, space);
		return true;
	}
	
	public boolean processOutputConnection(String output, PortEntry input, WireMode space)
	{
		addOutputWire(output, input, space);
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

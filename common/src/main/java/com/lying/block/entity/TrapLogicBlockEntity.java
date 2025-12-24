package com.lying.block.entity;

import static com.lying.reference.Reference.ModInfo.prefix;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import com.google.common.base.Function;
import com.lying.block.IWireableBlock;
import com.lying.block.IWireableBlock.WireRecipient;
import com.lying.block.TrapLogicBlock;
import com.lying.init.CDBlockEntityTypes;
import com.lying.reference.Reference;

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
	private static final Map<Identifier, Supplier<LogicHandler>> REGISTRY = new HashMap<>();
	public static final Supplier<LogicHandler> LOGIC_RELAY = () -> new LogicHandler(TrapLogicBlockEntity.ID_RELAY) 
	{
		public void handleLogic(List<BlockPos> sensors, List<BlockPos> actors, ServerWorld world)
		{
			if(sensors.isEmpty())
			{
				actors.forEach(p -> IWireableBlock.getWireable(p, world).deactivate(p, world));
				return;
			}
			
			final boolean status = sensors.stream().anyMatch(p -> IWireableBlock.getWireable(p, world).isActive(p, world));
			actors.forEach(p -> 
			{
				if(status)
					IWireableBlock.getWireable(p, world).activate(p, world);
				else
					IWireableBlock.getWireable(p, world).deactivate(p, world);
			});
		}
	};
	
	public static final Identifier ID_RELAY	= prefix("relay");
	public static final Identifier ID_1S_FALLOFF	= prefix("1s_falloff");
	public static final Identifier ID_5S_FALLOFF	= prefix("5s_falloff");
	public static final Identifier ID_10S_FALLOFF	= prefix("10s_falloff");
	public static final Identifier ID_CYCLER		= prefix("cycler");
	
	static
	{
		REGISTRY.put(ID_RELAY, LOGIC_RELAY);
		
		register(ID_1S_FALLOFF, id -> new FalloffLogicHandler(1));
		register(ID_5S_FALLOFF, id -> new FalloffLogicHandler(5));
		register(ID_10S_FALLOFF, id -> new FalloffLogicHandler(10));
		register(ID_CYCLER, CycleLogicHandler::new);
	}
	
	public static Supplier<LogicHandler> register(String idIn, Function<Identifier,LogicHandler> factoryIn)
	{
		return register(prefix(idIn), factoryIn);
	}
	
	public static Supplier<LogicHandler> register(Identifier idIn, Function<Identifier,LogicHandler> factoryIn)
	{
		Supplier<LogicHandler> sup = () -> factoryIn.apply(idIn);
		REGISTRY.put(idIn, sup);
		return sup;
	}
	
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
				TrapLogicBlock.validateTicker(type, CDBlockEntityTypes.TRAP_LOGIC.get(), 
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
		
		if(!tile.hasSensors())
		{
			// Deactivate actors
			tile.getActors().forEach(p -> IWireableBlock.getWireable(pos, world).deactivate(pos, world));
		}
		else
		{
			tile.cleanActors();
			tile.cleanSensors();
			tile.getHandler().handleLogic(tile.getSensors(), tile.getActors(), (ServerWorld)world);
		}
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
			handler = REGISTRY.getOrDefault(logicType, LOGIC_RELAY).get();
		
		return handler;
	}
	
	public static abstract class LogicHandler
	{
		private final Identifier id;
		protected NbtCompound data = new NbtCompound();
		
		public LogicHandler(Identifier idIn)
		{
			id = idIn;
		}
		
		public Identifier registryName() { return id; }
		
		public abstract void handleLogic(List<BlockPos> sensors, List<BlockPos> actors, ServerWorld world);
	}
	
	public static class FalloffLogicHandler extends LogicHandler
	{
		private final int tickDelay;
		
		public FalloffLogicHandler(int seconds)
		{
			super(prefix(seconds+"s_clock"));
			tickDelay = Reference.Values.TICKS_PER_SECOND * seconds;
		}
		
		public void handleLogic(List<BlockPos> sensors, List<BlockPos> actors, ServerWorld world)
		{
			boolean status = sensors.stream().anyMatch(p -> IWireableBlock.getWireable(p, world).isActive(p, world));
			if(status)
				data.putInt("Ticks", tickDelay);
			else
			{
				int ticksRemaining = data.getInt("Ticks");
				data.putInt("Ticks", --ticksRemaining);
				status = ticksRemaining > 0;
			}
			
			if(status)
				actors.forEach(p -> IWireableBlock.getWireable(p, world).activate(p, world));
			else
				actors.forEach(p -> IWireableBlock.getWireable(p, world).deactivate(p, world));
		}
	}
	
	public static class CycleLogicHandler extends LogicHandler
	{
		public CycleLogicHandler(Identifier idIn)
		{
			super(idIn);
		}
		
		public void handleLogic(List<BlockPos> sensors, List<BlockPos> actors, ServerWorld world)
		{
			int index = Math.floorDiv((int)world.getTime(), Reference.Values.TICKS_PER_SECOND)%actors.size();
			for(int i=0; i<actors.size(); i++)
			{
				BlockPos pos = actors.get(i);
				IWireableBlock actor = IWireableBlock.getWireable(actors.get(i), world);
				if(i == index)
					actor.activate(pos, world);
				else
					actor.deactivate(pos, world);
			}
		}
	}
}

package com.lying.init;

import static com.lying.reference.Reference.ModInfo.prefix;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import org.jetbrains.annotations.NotNull;

import com.google.common.base.Function;
import com.lying.CyclicDungeons;
import com.lying.block.IWireableBlock;
import com.lying.block.entity.logic.CycleLogicHandler;
import com.lying.block.entity.logic.FalloffLogicHandler;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

public class CDTrapLogicHandlers
{
	private static final Map<Identifier, Supplier<LogicHandler>> REGISTRY = new HashMap<>();
	
	public static final Identifier ID_RELAY			= prefix("relay");
	public static final Identifier ID_1S_FALLOFF	= prefix("1s_falloff");
	public static final Identifier ID_5S_FALLOFF	= prefix("5s_falloff");
	public static final Identifier ID_10S_FALLOFF	= prefix("10s_falloff");
	public static final Identifier ID_CYCLER		= prefix("cycler");
	
	/** Default behaviour, also used whenever the specified behaviour is unrecognised */
	public static final Supplier<LogicHandler> LOGIC_RELAY = () -> new LogicHandler(CDTrapLogicHandlers.ID_RELAY) 
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
	public static final Supplier<LogicHandler> FALLOFF_1S	= register(ID_1S_FALLOFF, id -> new FalloffLogicHandler(id, 1));
	public static final Supplier<LogicHandler> FALLOFF_5S	= register(ID_5S_FALLOFF, id -> new FalloffLogicHandler(id, 5));
	public static final Supplier<LogicHandler> FALLOFF_10S	= register(ID_10S_FALLOFF, id -> new FalloffLogicHandler(id, 10));
	public static final Supplier<LogicHandler> CYCLER		= register(ID_CYCLER, CycleLogicHandler::new);
	
	@SuppressWarnings("unused")
	private static Supplier<LogicHandler> register(String idIn, Function<Identifier,LogicHandler> factoryIn)
	{
		return register(prefix(idIn), factoryIn);
	}
	
	public static Supplier<LogicHandler> register(Identifier idIn, Function<Identifier,LogicHandler> factoryIn)
	{
		Supplier<LogicHandler> sup = () -> factoryIn.apply(idIn);
		REGISTRY.put(idIn, sup);
		return sup;
	}
	
	public static void init()
	{
		CyclicDungeons.LOGGER.info(" # Initialised {} trap logic behaviours", REGISTRY.size());
	}
	
	@NotNull
	public static LogicHandler get(Identifier id)
	{
		return REGISTRY.getOrDefault(id, LOGIC_RELAY).get();
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
}

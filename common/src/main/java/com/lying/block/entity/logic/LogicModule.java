package com.lying.block.entity.logic;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

import org.jetbrains.annotations.Nullable;
import org.joml.Vector2i;

import com.google.common.base.Predicates;
import com.google.common.collect.Lists;
import com.lying.block.Port;
import com.lying.block.entity.ModularLogicBlockEntity;
import com.lying.init.CDLogicGates;
import com.lying.init.CDLogicGates.LogicGate;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.text.Text;

public class LogicModule
{
	public static final Codec<LogicModule> CODEC	= RecordCodecBuilder.create(instance -> instance.group(
			Codec.STRING.optionalFieldOf("name").forGetter(m -> m.name),
			LogicGate.CODEC.fieldOf("logic").forGetter(m -> m.handler),
			PortSet.CODEC.optionalFieldOf("input_wires").forGetter(m -> m.inputWires.isEmpty() ? Optional.empty() : Optional.of(m.inputWires)),
			PortSet.CODEC.optionalFieldOf("output_wires").forGetter(m -> m.outputWires.isEmpty() ? Optional.empty() : Optional.of(m.outputWires)),
			PortState.CODEC.optionalFieldOf("prev_inputs").forGetter(m -> m.portCache.isWorthStoring() ? Optional.of(m.portCache) : Optional.empty()),
			LogicResult.CODEC.optionalFieldOf("prev_result").forGetter(m -> m.resultCache.isWorthStoring() ? Optional.of(m.resultCache) : Optional.empty())
			).apply(instance, (name,handler,inputs,outputs,prevWires,prevOut) -> 
			{
				LogicModule module = new LogicModule(handler);
				name.ifPresent(module::name);
				inputs.ifPresent(module::addInput);
				outputs.ifPresent(module::addOutput);
				prevWires.ifPresent(module.portCache::copy);
				module.resultCache = prevOut.orElse(LogicResult.create());
				return module;
			}));
	public static final Codec<List<LogicModule>> LIST_CODEC	= CODEC.listOf();
	
	public static final PacketCodec<ByteBuf, LogicModule> PACKET_CODEC	= PacketCodec.tuple(
			PacketCodecs.optional(PacketCodecs.STRING), m -> m.name, 
			LogicGate.PACKET_CODEC, m -> m.handler, 
			PacketCodecs.optional(PortSet.PACKET_CODEC), m -> m.inputWires.isEmpty() ? Optional.empty() : Optional.of(m.inputWires), 
			PacketCodecs.optional(PortSet.PACKET_CODEC), m -> m.outputWires.isEmpty() ? Optional.empty() : Optional.of(m.outputWires), 
			PacketCodecs.optional(PortState.PACKET_CODEC), m -> m.portCache.isWorthStoring() ? Optional.of(m.portCache) : Optional.empty(), 
			PacketCodecs.optional(LogicResult.PACKET_CODEC), m -> m.resultCache.isWorthStoring() ? Optional.of(m.resultCache) : Optional.empty(), 
			(name,handler,inputs,outputs,prevWires,prevOut) ->
			{
				LogicModule module = new LogicModule(handler);
				name.ifPresent(module::name);
				inputs.ifPresent(module::addInput);
				outputs.ifPresent(module::addOutput);
				prevWires.ifPresent(module.portCache::copy);
				module.resultCache = prevOut.orElse(LogicResult.create());
				return module;
			});
	public static final PacketCodec<ByteBuf, List<LogicModule>> LIST_PACKET_CODEC = PacketCodec.of(
			(list,buf) -> 
			{
				buf.writeInt(list.size());
				if(!list.isEmpty())
					list.forEach(m -> LogicModule.PACKET_CODEC.encode(buf, m));
			}, 
			buf -> 
			{
				List<LogicModule> set = Lists.newArrayList();
				int size = buf.readInt();
				while(size-- > 0)
					set.add(LogicModule.PACKET_CODEC.decode(buf));
				return set;
			});
	
	private Optional<String> name = Optional.empty();
	private final LogicGate handler;
	
	private final PortSet inputWires = new PortSet();
	final PortSet outputWires = new PortSet();
	
	// State of input ports in previous frame
	private PortState portCache = new PortState();
	// Module output in previous frame
	private LogicResult resultCache = LogicResult.create();
	
	protected LogicModule(LogicGate handlerIn)
	{
		handler = handlerIn;
	}
	
	public static LogicModule of(LogicGate handlerIn)
	{
		return new LogicModule(handlerIn);
	}
	
	public LogicModule name(String nameIn)
	{
		name = nameIn.length() == 0 ? Optional.empty() : Optional.of(nameIn);
		return this;
	}
	
	public boolean is(LogicGate gate) { return handler.registryName().equalsIgnoreCase(gate.registryName()); }
	
	public Text displayName() { return name.isPresent() ? Text.literal(name.get()) : handler.displayName(); }
	
	public boolean hasCustomName() { return customName().orElse("").length() > 0; }
	
	@Nullable
	public Optional<String> customName() { return name; }
	
	public Vector2i texCoords() { return handler.texCoords(); }
	
	/** Returns an Optional holding the port equivalent of this module, if its name can be a valid port name */
	public Optional<Port> toPort()
	{
		if(!hasCustomName())
			return Optional.empty();
		String name = customName().get();
		try
		{
			return Optional.of(Port.of(name));
		}
		catch(Exception e) { return Optional.empty(); }
	}
	
	/** Returns TRUE if this is an input gate with a custom name */
	public boolean isInput() { return handler.registryName().equals(CDLogicGates.ENTRY.get().registryName()) && toPort().isPresent(); }
	
	/** Returns TRUE if this is an output gate with a custom name */
	public boolean isOutput() { return handler.registryName().equals(CDLogicGates.EXIT.get().registryName()) && toPort().isPresent(); }
	
	public LogicModule addOutput(PortSet wires)
	{
		for(Port port : wires.ports())
			addOutput(port, wires.get(port).toArray(new String[0]));
		return this;
	}
	
	public LogicModule addOutput(Port input, String... wires)
	{
		outputWires.put(input, wires);
		return this;
	}
	
	public LogicModule addInput(String wires)
	{
		return addInput(CDLogicGates.INPUT, wires);
	}
	
	public LogicModule addInput(PortSet wires)
	{
		for(Port port : wires.ports())
			addInput(port, wires.get(port).toArray(new String[0]));
		return this;
	}
	
	public LogicModule addInput(Port input, String... wires)
	{
		inputWires.put(input, wires);
		return this;
	}
	
	public Optional<List<Port>> inputPorts()
	{
		List<Port> ports = handler.inputPorts(this);
		return ports.isEmpty() ? Optional.empty() : Optional.of(ports);
	}
	
	public Optional<List<Port>> outputPorts()
	{
		List<Port> ports = handler.outputPorts(this);
		return ports.isEmpty() ? Optional.empty() : Optional.of(ports);
	}
	
	public Optional<List<String>> collectInputs()
	{
		List<String> set = inputWires.wires();
		return set.isEmpty() ? Optional.empty() : Optional.of(set);
	}
	
	/** Returns true if there are no wires connected to any port of this module */
	public boolean hasNoWires()
	{
		return inputWires.isEmpty() && outputWires.isEmpty();
	}
	
	public PortSet inputPortSet() { return inputWires; }
	
	public PortSet outputPortSet() { return outputWires; }
	
	public boolean hasInput(String name)
	{
		return inputWires.hasWire(name);
	}
	
	public boolean hasOutput(String name)
	{
		return outputWires.hasWire(name);
	}
	
	public void removeConnections(String name)
	{
		if(hasInput(name))
		{
			inputWires.clear(name);
			List<Port> ports = handler.inputPorts(this);
			List<Port> absent = inputWires.ports().stream().filter(Predicates.not(ports::contains)).toList();
			absent.forEach(inputWires::removePort);
		}
		
		if(hasOutput(name))
			outputWires.clear(name);
	}
	
	public void clearConnections()
	{
		inputWires.clear();
		outputWires.clear();
	}
	
	public boolean getOutputStatus(Port port) { return resultCache.get(port); }
	
	/** Adds all wires used by this module to the wire map */
	public void registerWires(Consumer<String> register)
	{
		outputWires.wires().forEach(register::accept);
		inputWires.wires().forEach(register::accept);
	}
	
	/** Returns the status of the port the given wire is connected to */
	public boolean getOutputTo(String wire)
	{
		// Find port wire is connected to, return status of that port
		Port port = null;
		for(Port portName : outputWires.ports())
			if(outputWires.get(portName).contains(wire))
			{
				port = portName;
				break;
			}
		return port == null ? false : resultCache.get(port);
	}
	
	public void update(Map<String,LogicWire> circuitWires, ModularLogicBlockEntity tile)
	{
		// Map the status of all input ports
		PortState portState = new PortState();
		
		if(!inputWires.isEmpty())
		{
			// Port is live if any wire connected to it is TRUE
			for(Port port : inputWires.ports())
				portState.put(port, inputWires.get(port).stream().anyMatch(w -> circuitWires.containsKey(w) ? circuitWires.get(w).isOn() : false));
		}
		// If we're an input module, set the input port to reflect the wired input port of the tile
		else if(isInput())
			portState.put(CDLogicGates.INPUT, tile.getInput(Port.of(name.get())));
		
		// Recalculate logic result
		resultCache = handler.getResult(portState, portCache, resultCache, tile, this);
		
		// Update port cache
		portCache.copy(portState);
	}
}
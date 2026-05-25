package com.lying.block.entity.logic;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

import com.google.common.collect.Lists;
import com.lying.block.entity.ModularLogicBlockEntity;
import com.lying.init.CDLogicGates;
import com.lying.init.CDLogicGates.LogicGate;
import com.lying.reference.Reference;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public class LogicModule
{
	public static final Codec<LogicModule> CODEC	= RecordCodecBuilder.create(instance -> instance.group(
			Codec.STRING.optionalFieldOf("name").forGetter(m -> m.name),
			LogicGate.CODEC.fieldOf("logic").forGetter(m -> m.handler),
			PortSet.CODEC.optionalFieldOf("input_wires").forGetter(m -> m.inputWires.isEmpty() ? Optional.empty() : Optional.of(m.inputWires)),
			PortSet.CODEC.optionalFieldOf("output_wires").forGetter(m -> m.outputWires.isEmpty() ? Optional.empty() : Optional.of(m.outputWires)),
			WireState.CODEC.optionalFieldOf("prev_inputs").forGetter(m -> m.portCache.isWorthStoring() ? Optional.of(m.portCache) : Optional.empty()),
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
	public static final long UPDATE_FREQUENCY = Reference.Values.TICKS_PER_SECOND / 2;
	
	private Optional<String> name = Optional.empty();
	private final LogicGate handler;
	private long lastUpdated = -1L;
	
	private final PortSet inputWires = new PortSet();
	final PortSet outputWires = new PortSet();
	
	// State of input ports in previous frame
	private WireState portCache = new WireState();
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
		name = Optional.of(nameIn);
		return this;
	}
	
	public String displayName() { return name.orElse(handler.registryName()); }
	
	/** Attaches a wire to the default result port of this module */
	public LogicModule addOutput(String wire)
	{
		return addOutput(CDLogicGates.OUTPUT, wire);
	}
	
	public LogicModule addOutput(PortSet wires)
	{
		for(String port : wires.ports())
			addOutput(port, wires.get(port).toArray(new String[0]));
		return this;
	}
	
	public LogicModule addOutput(String input, String... wires)
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
		for(String port : wires.ports())
			addInput(port, wires.get(port).toArray(new String[0]));
		return this;
	}
	
	public LogicModule addInput(String input, String... wires)
	{
		inputWires.put(input, wires);
		return this;
	}
	
	public Optional<List<String>> collectInputs()
	{
		List<String> set = inputWires.wires();
		return set.isEmpty() ? Optional.empty() : Optional.of(set);
	}
	
	public Optional<List<String>> collectOutputs()
	{
		List<String> set = outputWires.wires();
		return set.isEmpty() ? Optional.empty() : Optional.of(set);
	}
	
	public boolean hasInput(String name)
	{
		return inputWires.wires().contains(name);
	}
	
	public boolean hasOutput(String name)
	{
		return outputWires.wires().contains(name);
	}
	
	/** Adds all wires used by this module to the wire map */
	public void registerWires(Consumer<String> register)
	{
		outputWires.wires().forEach(register::accept);
		inputWires.wires().forEach(register::accept);
	}
	
	/** Returns a list of all input wires that must be calculated before this module */
	public List<String> prerequisites()
	{
		List<String> wires = Lists.newArrayList();
		inputWires.wires().forEach(wires::add);
		return wires.stream().distinct().toList();
	}
	
	/** Returns the status of the port the given wire is connected to */
	public boolean getOutputTo(String wire)
	{
		// Find port wire is connected to, return status of that port
		String port = null;
		for(String portName : outputWires.ports())
			if(outputWires.get(portName).contains(wire))
			{
				port = portName;
				break;
			}
		return port == null ? false : resultCache.get(port);
	}
	
	public void update(Map<String,LogicWire> circuitWires, long time, ModularLogicBlockEntity tile)
	{
		if(time - lastUpdated < UPDATE_FREQUENCY)
			return;
		
		// Mostly a formality, stops modules from updating unlimitedly during a singular tick
		lastUpdated = time;
		
		// Map the status of all input ports
		WireState portState = new WireState();
		
		if(!inputWires.isEmpty())
		{
			// Port is live if any wire connected to it is TRUE
			for(String port : inputWires.ports())
				portState.put(port, inputWires.get(port).stream().anyMatch(w -> circuitWires.containsKey(w) ? circuitWires.get(w).isOn() : false));
		}
		
		// Recalculate logic result
		resultCache = handler.getResult(portState, portCache, resultCache, tile);
		
		// Update port cache
		portCache.copy(portState);
	}
}
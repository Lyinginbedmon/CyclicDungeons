package com.lying.block.entity.logic;

import java.util.List;
import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public class LogicWire
{
	public static Codec<LogicWire> CODEC	= RecordCodecBuilder.create(instance -> instance.group(
			Codec.STRING.fieldOf("name").forGetter(w -> w.name),
			Codec.BOOL.optionalFieldOf("on").forGetter(w -> w.isLive ? Optional.of(true) : Optional.empty()))
			.apply(instance, (n,l) -> 
			{
				LogicWire wire = new LogicWire(n);
				wire.isLive = l.orElse(false);
				return wire;
			}));
	
	protected final String name;
	protected boolean isLive = false;
	
	public LogicWire(String nameIn)
	{
		name = nameIn;
	}
	
	public String name() { return name; }
	
	public boolean isOn() { return isLive; }
	
	public boolean isOff() { return !isLive; }
	
	public LogicWire setState(boolean var)
	{
		isLive = var;
		return this;
	}
	
	public final void update(List<LogicModule> circuit, long time)
	{
		isLive = circuit.stream().anyMatch(m -> m.hasOutput(name) && m.getOutputTo(name));
	}
}
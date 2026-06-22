package com.lying.client.screen.circuit.handlers;

import java.util.Map;
import java.util.Optional;

import org.joml.Vector2i;

import com.lying.client.screen.NodeRenderUtils;
import com.lying.client.screen.circuit.CircuitModule;
import com.lying.client.screen.circuit.CircuitPort;
import com.lying.client.screen.circuit.CircuitScreen;
import com.lying.client.screen.circuit.CircuitWire;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Vec2f;

public class ConnectPortHandler extends AbstractClickHandler
{
	final CircuitPort output;
	
	public ConnectPortHandler(CircuitPort outputIn, CircuitScreen parentIn)
	{
		super(parentIn);
		output = outputIn;
	}
	
	public String name() { return "connect_port"; }
	
	public boolean preventsWireHighlighting(CircuitWire wire) { return wire.hasTerminus(output); }
	
	public static Optional<ClickHandler> tryCreateFrom(int microX, int microY, Vector2i gridPos, Map<Vector2i, CircuitModule> circuit, CircuitScreen screen)
	{
		Optional<CircuitPort> closestPort = portAt(microX, microY, gridPos, circuit, true);
		if(closestPort.isEmpty())
			return Optional.empty();
		
		playSound(SoundEvents.BLOCK_TRIPWIRE_ATTACH);
		return Optional.of(new ConnectPortHandler(closestPort.get(), screen));
	}
	
	public boolean handleClick(boolean isHoldingShift, int microX, int microY, Vector2i gridPos, Map<Vector2i, CircuitModule> circuit)
	{
		boolean result = false;
		Optional<CircuitWire> closestWire;
		
		// Connecting port to port, creating a new wire
		Optional<CircuitPort> closestPort = portAt(microX, microY, gridPos, circuit, false);
		if(closestPort.isPresent())
		{
			final String name = parent.makeWireName();
			final CircuitPort input = closestPort.get();
			
			// Attach output to new wire
			circuit.get(output.gridPos()).addOutput(output.port(), name);
			// Attach input to new wire
			circuit.get(gridPos).addInput(input.port(), name);
			
			parent.addWire(new CircuitWire(name, output, input));
			playSound(SoundEvents.BLOCK_TRIPWIRE_ATTACH);
			
			result = true;
		}
		// Connecting port to an existing wire
		else if((closestWire = parent.getWireAt(microX, microY)).isPresent())
		{
			CircuitWire wire = closestWire.get();
			
			// Attach port to wire
			CircuitModule module = circuit.get(output.gridPos());
			module.addOutput(output.port(), wire.name());
			
			// Attach wire to port
			wire.attachPort(output);
			
			result = true;
		}
		
		if(result)
		{
			playSound(SoundEvents.BLOCK_TRIPWIRE_ATTACH);
			if(!isHoldingShift)
				parent.clearHandler();
		}
		return result;
	}
	
	public void renderBackground(DrawContext context, int microX, int microY, float delta, Map<Vector2i, CircuitModule> circuit)
	{
		Vector2i vec = output.microPosition(circuit).get();
		NodeRenderUtils.renderStraightLine(new Vec2f(vec.x(), vec.y()), new Vec2f(microX, microY), 2, context, CircuitWire.WIRE_COLOUR);
	}
}
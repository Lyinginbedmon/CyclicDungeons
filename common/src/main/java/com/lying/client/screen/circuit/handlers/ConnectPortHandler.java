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
import net.minecraft.util.math.Vec2f;

public class ConnectPortHandler extends AbstractHandler
{
	final CircuitPort output;
	
	public ConnectPortHandler(CircuitPort outputIn, CircuitScreen parentIn)
	{
		super(parentIn);
		output = outputIn;
	}
	
	public static Optional<ClickHandler> tryCreateFrom(int mouseX, int mouseY, Vector2i gridPos, Map<Vector2i, CircuitModule> circuit, CircuitScreen screen)
	{
		Optional<CircuitPort> closestPort = portAt(mouseX, mouseY, gridPos, circuit, true);
		return closestPort.isEmpty() ? Optional.empty() : Optional.of(new ConnectPortHandler(closestPort.get(), screen));
	}
	
	public boolean handleClick(boolean isHoldingShift, int mouseX, int mouseY, Vector2i gridPos, Map<Vector2i, CircuitModule> circuit)
	{
		// Connecting port to port, creating a new wire
		Optional<CircuitPort> closestPort = portAt(mouseX, mouseY, gridPos, circuit, false);
		if(closestPort.isPresent())
		{
			final String name = parent.makeWireName();
			final CircuitPort input = closestPort.get();
			
			// Attach output to new wire
			circuit.get(output.gridPos()).addOutput(output.port(), name);
			// Attach input to new wire
			circuit.get(gridPos).addInput(input.port(), name);
			
			parent.addWire(new CircuitWire(name, output, input));
			
			if(!isHoldingShift)
				parent.clearHandler();
			return true;
		}
		
		// Connecting port to an existing wire
		Optional<CircuitWire> closestWire = parent.getWireAt(mouseX, mouseY);
		if(closestWire.isPresent())
		{
			CircuitWire wire = closestWire.get();
			
			// Attach port to wire
			CircuitModule module = circuit.get(output.gridPos());
			module.addOutput(output.port(), wire.name());
			
			// Attach wire to port
			wire.attachPort(output);
			
			if(!isHoldingShift)
				parent.clearHandler();
			return true;
		}
		
		return false;
	}
	
	public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta, Map<Vector2i, CircuitModule> circuit)
	{
		Vector2i vec = output.screenPosition(circuit).get();
		NodeRenderUtils.renderStraightLine(new Vec2f(vec.x(), vec.y()), new Vec2f(mouseX, mouseY), 2, context, CircuitWire.WIRE_COLOUR);
	}
}
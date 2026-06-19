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

public class ConnectWireHandler extends AbstractHandler
{
	final CircuitWire wire;
	
	public ConnectWireHandler(CircuitWire wireIn, CircuitScreen parentIn)
	{
		super(parentIn);
		wire = wireIn;
	}
	
	public static Optional<ClickHandler> tryCreateFrom(int mouseX, int mouseY, Vector2i gridPos, Map<Vector2i, CircuitModule> circuit, CircuitScreen screen)
	{
		Optional<CircuitWire> wireAt = screen.getWireAt(mouseX, mouseY);
		return wireAt.isEmpty() ? Optional.empty() : Optional.of(new ConnectWireHandler(wireAt.get(), screen));
	}
	
	public boolean handleClick(boolean isHoldingShift, int mouseX, int mouseY, Vector2i gridPos, Map<Vector2i, CircuitModule> circuit)
	{
		Optional<CircuitPort> port = portAt(mouseX, mouseY, gridPos, circuit, false);
		if(port.isEmpty())
		{
			parent.clearHandler();
			return false;
		}
		
		// Add wire to module inputs
		CircuitPort input = port.get();
		CircuitModule module = circuit.get(input.gridPos());
		module.addInput(input.port(), wire.name());
		
		// Add module to wire outputs
		wire.attachPort(input);
		
		if(!isHoldingShift)
			parent.clearHandler();
		return false;
	}
	
	public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta, Map<Vector2i, CircuitModule> circuit)
	{
		Vector2i vec = wire.medianPoint(circuit);
		NodeRenderUtils.renderStraightLine(new Vec2f(vec.x(), vec.y()), new Vec2f(mouseX, mouseY), 2, context, CircuitWire.WIRE_COLOUR);
	}
}

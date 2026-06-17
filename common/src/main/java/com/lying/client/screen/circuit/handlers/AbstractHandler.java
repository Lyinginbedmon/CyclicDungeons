package com.lying.client.screen.circuit.handlers;

import java.util.Map;
import java.util.Optional;

import org.joml.Vector2i;

import com.lying.block.Port;
import com.lying.client.screen.circuit.CircuitModule;
import com.lying.client.screen.circuit.CircuitPort;
import com.lying.client.screen.circuit.CircuitScreen;

import net.minecraft.client.font.TextRenderer;

public abstract class AbstractHandler implements ClickHandler
{
	protected final CircuitScreen parent;
	protected final TextRenderer textRenderer;
	
	public AbstractHandler(CircuitScreen parentIn)
	{
		parent = parentIn;
		textRenderer = parentIn.getTextRenderer();
	}
	
	public static Optional<CircuitPort> portAt(int mouseX, int mouseY, Vector2i gridPos, Map<Vector2i, CircuitModule> circuit, boolean isOutput)
	{
		if(circuit.isEmpty() || !circuit.containsKey(gridPos))
			return Optional.empty();
		
		CircuitModule module = circuit.get(gridPos);
		Optional<Port> closestPort = module.getClosestPort((int)mouseX, (int)mouseY, isOutput);
		if(closestPort.isEmpty())
			return Optional.empty();
		
		return Optional.of(new CircuitPort(gridPos, closestPort.get()));
	}
}
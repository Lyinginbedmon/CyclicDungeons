package com.lying.client.screen.circuit.handlers;

import java.util.Map;

import org.joml.Vector2i;

import com.lying.client.screen.circuit.CircuitModule;
import com.lying.client.screen.circuit.CircuitScreen;
import com.lying.init.CDLogicGates.LogicGate;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

public class GateHandler extends AbstractHandler
{
	final LogicGate gate;
	
	public GateHandler(LogicGate gateIn, CircuitScreen parentIn)
	{
		super(parentIn);
		gate = gateIn;
	}
	
	public boolean handleClick(boolean isHoldingShift, int mouseX, int mouseY, Vector2i gridPos, Map<Vector2i, CircuitModule> circuit)
	{
		parent.put(gridPos, gate);
		if(!isHoldingShift)
			parent.clearHandler();
		return true;
	}
	
	public void renderForeground(DrawContext context, int mouseX, int mouseY, float delta, Map<Vector2i, CircuitModule> circuit)
	{
		Text name = gate.displayName();
		context.drawText(textRenderer, name, mouseX - (textRenderer.getWidth(name) / 2), mouseY, -1, true);
	}
}
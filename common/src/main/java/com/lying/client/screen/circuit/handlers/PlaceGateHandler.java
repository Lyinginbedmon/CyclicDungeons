package com.lying.client.screen.circuit.handlers;

import java.util.Map;

import org.joml.Vector2i;

import com.lying.client.screen.circuit.CircuitModule;
import com.lying.client.screen.circuit.CircuitScreen;
import com.lying.init.CDLogicGates.LogicGate;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.math.ColorHelper;

public class PlaceGateHandler extends AbstractClickHandler
{
	final LogicGate gate;
	
	public PlaceGateHandler(LogicGate gateIn, CircuitScreen parentIn)
	{
		super(parentIn);
		gate = gateIn;
	}
	
	public String name() { return "place_gate"; }
	
	public boolean handleClick(boolean isHoldingShift, int microX, int mouseY, Vector2i gridPos, Map<Vector2i, CircuitModule> circuit)
	{
		playSound(SoundEvents.ENTITY_ITEM_FRAME_ADD_ITEM);
		parent.put(gridPos, gate);
		if(!isHoldingShift)
			parent.clearHandler();
		return true;
	}
	
	public void renderBackground(DrawContext context, int microX, int microY, float delta, Map<Vector2i, CircuitModule> circuit)
	{
		Vector2i gridPos = CircuitScreen.gridToMicro(CircuitScreen.microToGrid(new Vector2i(microX, microY)));
		if(circuit.containsKey(gridPos))
			return;
		final Vector2i tex = gate.texCoords();
		context.drawTexture(
				RenderLayer::getGuiTextured, 
				CircuitScreen.TEXTURE, 
				gridPos.x() - 24, 
				gridPos.y() - 24, 
				(float)tex.x(), 
				(float)tex.y(), 
				48, 
				48, 
				256, 
				256, 
				ColorHelper.withAlpha(120, CircuitModule.DEFAULT_COLOUR));
	}
	
	public void renderForeground(DrawContext context, int microX, int microY, float delta, Map<Vector2i, CircuitModule> circuit)
	{
		Text name = gate.displayName();
		Vector2i gridPos = CircuitScreen.microToGrid(new Vector2i(microX, microY));
		if(circuit.containsKey(gridPos))
			return;
		gridPos = CircuitScreen.gridToMicro(gridPos);
		context.drawText(textRenderer, name, gridPos.x() - (textRenderer.getWidth(name) / 2), gridPos.y() - (textRenderer.fontHeight / 2), -1, true);
	}
}
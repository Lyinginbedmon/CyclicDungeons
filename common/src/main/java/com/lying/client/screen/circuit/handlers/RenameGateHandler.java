package com.lying.client.screen.circuit.handlers;

import java.util.Map;
import java.util.Optional;

import org.joml.Vector2i;
import org.lwjgl.glfw.GLFW;

import com.lying.client.screen.circuit.CircuitModule;
import com.lying.client.screen.circuit.CircuitScreen;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.util.Identifier;

public class RenameGateHandler extends AbstractClickHandler
{
	public static final Identifier TEXTURE = CircuitScreen.TEXTURE;
	private final Vector2i target;
	
	public RenameGateHandler(Vector2i targetIn, CircuitScreen parentIn)
	{
		super(parentIn);
		this.target = targetIn;
	}

	public static Optional<ClickHandler> tryCreateFrom(int microX, int microY, Vector2i gridPos, Map<Vector2i, CircuitModule> circuit, CircuitScreen screen)
	{
		if(!circuit.containsKey(gridPos))
			return Optional.empty();
		
		TextFieldWidget nameField = screen.nameField;
		circuit.get(gridPos).customName().ifPresentOrElse(nameField::setText, () -> nameField.setText(""));
		screen.setFocused(nameField);
		
		return Optional.of(new RenameGateHandler(gridPos, screen));
	}
	
	public String name() { return "name_gate"; }
	
	public boolean handleClick(boolean isHoldingShift, int microX, int microY, Vector2i gridPos, Map<Vector2i, CircuitModule> circuit)
	{
		if(gridPos.gridDistance(target) > 0)
		{
			parent.clearHandler();
			return true;
		}
		return false;
	}
	
	public boolean handleKeyPress(int keyCode, int modifiers)
	{
		if(keyCode == GLFW.GLFW_KEY_ESCAPE || keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER)
		{
			parent.clearHandler();
			return true;
		}
		
		return false;
	}
	
	public void onNameChanged(String name, Map<Vector2i, CircuitModule> circuit)
	{
		CircuitModule module = circuit.get(target);
		module.setName(name.replace(' ', '_').replaceAll("[^a-zA-Z0-9_]", ""));
	}
	
	public void renderBackground(DrawContext context, int microX, int microY, float delta, Map<Vector2i, CircuitModule> circuit)
	{
		CircuitModule module = circuit.getOrDefault(target, null);
		if(module == null)
			return;
		
		Vector2i tex = module.texCoords();
		Vector2i point = CircuitScreen.gridToMicro(target);
		context.drawTexture(
				RenderLayer::getGuiTextured, 
				TEXTURE, 
				point.x() - 24, 
				point.y() - 24, 
				(float)tex.x(), 
				(float)tex.y() + 48F, 
				48, 
				48, 
				256, 
				256, 
				-1);
	}
}

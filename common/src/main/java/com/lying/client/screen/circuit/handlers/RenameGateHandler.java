package com.lying.client.screen.circuit.handlers;

import java.util.Map;
import java.util.Optional;

import org.joml.Vector2i;
import org.lwjgl.glfw.GLFW;

import com.lying.client.screen.circuit.CircuitModule;
import com.lying.client.screen.circuit.CircuitScreen;

import net.minecraft.client.gui.widget.TextFieldWidget;

public class RenameGateHandler extends AbstractClickHandler
{
	private final Vector2i target;
	
	// FIXME Add highlighting to selected gate
	
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
		if(keyCode == GLFW.GLFW_KEY_ESCAPE || keyCode == GLFW.GLFW_KEY_ENTER)
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
}

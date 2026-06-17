package com.lying.client.screen.circuit.handlers;

import java.util.Map;

import org.joml.Vector2i;

import com.lying.client.screen.circuit.CircuitModule;

import net.minecraft.client.gui.DrawContext;

public interface ClickHandler
{
	public default boolean preventsWireHighlighting() { return true; }
	
	/**
	 * 
	 * @param isHoldingShift
	 * @param mouseX
	 * @param mouseY
	 * @param gridPos
	 * @param circuit
	 * @return
	 */
	public boolean handleClick(boolean isHoldingShift, int mouseX, int mouseY, Vector2i gridPos, Map<Vector2i,CircuitModule> circuit);
	
	/** Renders the background elements of this handler */
	public default void renderBackground(DrawContext context, int mouseX, int mouseY, float delta, Map<Vector2i, CircuitModule> circuit) { }
	
	/** Renders the foreground elements of this handler */
	public default void renderForeground(DrawContext context, int mouseX, int mouseY, float delta, Map<Vector2i, CircuitModule> circuit) { }
}
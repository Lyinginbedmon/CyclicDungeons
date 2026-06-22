package com.lying.client.screen.circuit.handlers;

import java.util.Map;

import org.joml.Vector2i;

import com.lying.client.screen.circuit.CircuitModule;
import com.lying.client.screen.circuit.CircuitWire;
import com.lying.reference.Reference;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

public interface ClickHandler
{
	public String name();
	
	public default Text displayName() { return Reference.ModInfo.translate("gui", "circuit_builder."+name()); }
	
	public default boolean preventsWireHighlighting(CircuitWire wire) { return true; }
	
	public boolean handleClick(boolean isHoldingShift, int microX, int microY, Vector2i gridPos, Map<Vector2i,CircuitModule> circuit);
	
	public default boolean handleScroll(int scroll) { return false; }
	
	/** Renders the background elements of this handler */
	public default void renderBackground(DrawContext context, int microX, int microY, float delta, Map<Vector2i, CircuitModule> circuit) { }
	
	/** Renders the foreground elements of this handler */
	public default void renderForeground(DrawContext context, int microX, int microY, float delta, Map<Vector2i, CircuitModule> circuit) { }
}
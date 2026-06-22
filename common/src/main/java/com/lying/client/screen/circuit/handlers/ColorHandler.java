package com.lying.client.screen.circuit.handlers;

import java.util.Map;
import java.util.Optional;

import org.joml.Vector2i;

import com.lying.client.screen.circuit.CircuitModule;
import com.lying.client.screen.circuit.CircuitScreen;
import com.lying.client.screen.circuit.CircuitWire;
import com.lying.utility.CDUtils;

import net.minecraft.sound.SoundEvents;
import net.minecraft.util.DyeColor;

public class ColorHandler extends AbstractClickHandler
{
	private int colorIndex = 0;
	
	public ColorHandler(CircuitScreen parentIn)
	{
		super(parentIn);
		this.iconPos = new Vector2i(0, 48);
	}
	
	public String name() { return "paint"; }
	
	public boolean preventsWireHighlighting(CircuitWire wire) { return false; }
	
	public boolean handleScroll(int scroll)
	{
		colorIndex += scroll;
		while(colorIndex < 0)
			colorIndex += CDUtils.ORDERED_COLORS.length;
		return true;
	}
	
	public DyeColor color() { return DyeColor.values()[colorIndex%CDUtils.ORDERED_COLORS.length]; }
	
	protected int iconColor() { return color().getEntityColor(); }
	
	public boolean handleClick(boolean isHoldingShift, int microX, int microY, Vector2i gridPos, Map<Vector2i, CircuitModule> circuit)
	{
		Optional<CircuitWire> wire;
		boolean result = false;
		
		// Paint a logic gate
		if(circuit.containsKey(gridPos))
		{
			circuit.get(gridPos).setColor(color());
			result = true;
		}
		else if((wire = parent.getWireAt(microX, microY)).isPresent())
		{
			// Paint a wire
			wire.get().setColor(color());
			result = true;
		}
		
		if(result)
		{
			playSound(SoundEvents.ITEM_BUCKET_EMPTY);
			if(!isHoldingShift)
				parent.clearHandler();
		}
		return result;
	}
}

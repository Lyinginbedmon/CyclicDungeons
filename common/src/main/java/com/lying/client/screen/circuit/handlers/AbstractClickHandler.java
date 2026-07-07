package com.lying.client.screen.circuit.handlers;

import java.util.Map;
import java.util.Optional;

import org.joml.Vector2i;

import com.lying.block.Port;
import com.lying.client.screen.circuit.CircuitModule;
import com.lying.client.screen.circuit.CircuitPort;
import com.lying.client.screen.circuit.CircuitScreen;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.sound.SoundEvent;

public abstract class AbstractClickHandler implements ClickHandler
{
	protected static final MinecraftClient mc = MinecraftClient.getInstance();
	protected final CircuitScreen parent;
	protected final TextRenderer textRenderer;
	protected Vector2i iconPos = new Vector2i(0, 0);
	
	public AbstractClickHandler(CircuitScreen parentIn)
	{
		parent = parentIn;
		textRenderer = parentIn.getTextRenderer();
	}
	
	public static Optional<CircuitPort> portAt(int microX, int microY, Vector2i gridPos, Map<Vector2i, CircuitModule> circuit, boolean isOutput)
	{
		if(circuit.isEmpty() || !circuit.containsKey(gridPos))
			return Optional.empty();
		
		CircuitModule module = circuit.get(gridPos);
		Optional<Port> closestPort = module.getClosestPort(microX, microY, isOutput);
		if(closestPort.isEmpty())
			return Optional.empty();
		
		return Optional.of(new CircuitPort(gridPos, closestPort.get()));
	}
	
	protected int iconColor() { return -1; }
	
	protected static void playSound(SoundEvent event)
	{
		mc.getSoundManager().play(PositionedSoundInstance.master(event, 1.0F));
	}
}
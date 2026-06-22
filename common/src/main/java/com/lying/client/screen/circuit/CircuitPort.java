package com.lying.client.screen.circuit;

import java.util.Map;
import java.util.Optional;

import org.joml.Vector2i;

import com.lying.block.Port;

public record CircuitPort(Vector2i gridPos, Port port)
{
	public boolean equals(Object other)
	{
		CircuitPort otherPort;
		return other instanceof CircuitPort && (otherPort = ((CircuitPort)other)).gridPos().gridDistance(gridPos) == 0 && otherPort.port().equals(port);
	}
	
	public Optional<Vector2i> microPosition(Map<Vector2i,CircuitModule> circuitMap)
	{
		if(!circuitMap.containsKey(gridPos))
			return Optional.empty();
		
		CircuitModule module = circuitMap.get(gridPos);
		Vector2i modPos = module.microPosition();
		
		Optional<Vector2i> portPos = module.getPortPosition(port);
		return portPos.isEmpty() ? Optional.empty() : Optional.of(new Vector2i(modPos.x() + portPos.get().x(), modPos.y() + portPos.get().y()));
	}
}
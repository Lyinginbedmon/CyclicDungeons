package com.lying.client.screen.circuit;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.joml.Vector2i;

import com.google.common.collect.Lists;
import com.lying.client.screen.NodeRenderUtils;
import com.lying.utility.geometry.LineSegment2f;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.math.Vec2f;

public class CircuitWire
{
	private static final char[] ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray();
	
	public static final int WIRE_COLOUR		= 0xD0D0D0;
	public static final int WIRE_HIGHLIGHT	= 0xFFFFFF;
	private static final int WIRE_WIDTH	= 2;
	private final String name;
	private List<CircuitPort> inputSet = Lists.newArrayList();
	private List<CircuitPort> outputSet = Lists.newArrayList();
	
	public CircuitWire(String nameIn, CircuitPort start, CircuitPort end)
	{
		name = nameIn;
		inputSet.add(start);
		outputSet.add(end);
	}
	
	public final String name() { return name; }
	
	public static String getWireName(int index)
	{
		String name = "";
		while(index >= 0)
		{
			name = ALPHABET[index%ALPHABET.length] + name;
			index -= ALPHABET.length;
		}
		return name;
	}
	
	public boolean removeAndDecapitate(Vector2i grid)
	{
		removeTerminus(grid);
		return decapitated();
	}
	
	public List<Vector2i> getTermini()
	{
		List<Vector2i> points = Lists.newArrayList();
		inputSet.stream().map(CircuitPort::gridPos).forEach(points::add);
		outputSet.stream().map(CircuitPort::gridPos).forEach(points::add);
		return points;
	}
	
	public boolean isTerminus(Vector2i grid)
	{
		return getTermini().contains(grid);
	}
	
	public void attachInput(CircuitPort port)
	{
		inputSet.add(port);
	}
	
	public void attachOutput(CircuitPort port)
	{
		outputSet.add(port);
	}
	
	public void removeTerminus(Vector2i grid)
	{
		outputSet.removeIf(out -> out.gridPos().gridDistance(grid) == 0);
		inputSet.removeIf(in -> in.gridPos().gridDistance(grid) == 0);
	}
	
	/** Returns true if this wire has no input or output positions */
	public boolean decapitated()
	{
		return inputSet.isEmpty() || outputSet.isEmpty();
	}
	
	public boolean isHovered(int mouseX, int mouseY, Map<Vector2i,CircuitModule> circuit)
	{
		if(decapitated())
			return false;
		
		List<CircuitPort> totalPorts = Lists.newArrayList();
		totalPorts.addAll(inputSet);
		totalPorts.addAll(outputSet);
		
		// Identify all terminus points and the median where they all meet
		final Vector2i median = medianPoint(circuit);
		
		return 
				totalPorts.stream()
				.map(p -> p.screenPosition(circuit))
				.filter(Optional::isPresent)
				.map(Optional::get)
				.anyMatch(p -> 
				{
					final LineSegment2f line = new LineSegment2f(new Vec2f(p.x(), p.y()), new Vec2f(median.x(), median.y));
					final Vec2f normal = line.normal();
					
					final Vec2f mouse = new Vec2f(mouseX, mouseY);
					final LineSegment2f lineB = new LineSegment2f(mouse.add(normal.multiply(WIRE_WIDTH)), mouse.add(normal.negate().multiply(WIRE_WIDTH)));
					
					return line.intersectsAtAll(lineB);
				});
	}
	
	public Vector2i medianPoint(Map<Vector2i,CircuitModule> circuit)
	{
		List<CircuitPort> totalPorts = Lists.newArrayList();
		totalPorts.addAll(inputSet);
		totalPorts.addAll(outputSet);
		Vector2i median = new Vector2i(0,0);
		for(Vector2i point : totalPorts.stream()
				.map(p -> p.screenPosition(circuit))
				.filter(Optional::isPresent)
				.map(Optional::get)
				.toList())
			median = median.add(point);
		return new Vector2i(median.x() / totalPorts.size(), median.y() / totalPorts.size());
	}
	
	public void render(boolean isHovered, DrawContext context, Map<Vector2i,CircuitModule> circuit)
	{
		if(decapitated())
			return;
		
		List<CircuitPort> totalPorts = Lists.newArrayList();
		totalPorts.addAll(inputSet);
		totalPorts.addAll(outputSet);
		
		// Identify all terminus points and the median where they all meet
		final Vector2i median = medianPoint(circuit);
		
		// Render each terminus as a line connecting its screen position to the median
		totalPorts.stream()
			.map(p -> p.screenPosition(circuit))
			.filter(Optional::isPresent)
			.map(Optional::get)
			.forEach(point -> 
				NodeRenderUtils.renderStraightLine(new Vec2f(point.x(), point.y()), new Vec2f(median.x(), median.y()), WIRE_WIDTH, context, isHovered ? WIRE_HIGHLIGHT : WIRE_COLOUR));
	}
}

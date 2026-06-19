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
	private List<CircuitPort> termini = Lists.newArrayList();
	
	public CircuitWire(String nameIn)
	{
		name = nameIn;
	}
	
	public CircuitWire(String nameIn, CircuitPort start, CircuitPort end)
	{
		this(nameIn);
		termini.add(start);
		termini.add(end);
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
	
	public List<Vector2i> getTermini()
	{
		return termini.stream()
				.map(CircuitPort::gridPos)
				.toList();
	}
	
	public void assertOnCircuit(Map<Vector2i, CircuitModule> circuit)
	{
		termini.forEach(p -> 
		{
			CircuitModule module = circuit.getOrDefault(p.gridPos(), null);
			if(module == null)
				return;
			
			module.addPort(p.port(), name());
		});
	}
	
	public boolean isTerminus(Vector2i grid)
	{
		return getTermini().contains(grid);
	}
	
	public void attachPort(CircuitPort port)
	{
		termini.add(port);
	}
	
	public void removeTerminus(Vector2i grid)
	{
		termini.removeIf(in -> in.gridPos().gridDistance(grid) == 0);
	}
	
	/** Returns true if this wire has insufficient positions to exist */
	public boolean decapitated()
	{
		return termini.size() < 2;
	}
	
	public boolean isHovered(int mouseX, int mouseY, Map<Vector2i,CircuitModule> circuit)
	{
		if(decapitated())
			return false;
		
		// Identify all terminus points and the median where they all meet
		final Vector2i median = medianPoint(circuit);
		
		return 
				termini.stream()
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
		Vector2i median = new Vector2i(0,0);
		for(CircuitPort p : termini)
		{
			Optional<Vector2i> point = p.screenPosition(circuit);
			if(point.isEmpty())
				continue;
			median = median.add(point.get());
		}
		
		return new Vector2i(median.x() / termini.size(), median.y() / termini.size());
	}
	
	public void render(boolean isHovered, DrawContext context, Map<Vector2i,CircuitModule> circuit)
	{
		if(decapitated())
			return;
		
		// Identify all terminus points and the median where they all meet
		final Vector2i median = medianPoint(circuit);
		
		// Render each terminus as a line connecting its screen position to the median
		final Vec2f endVec = new Vec2f(median.x(), median.y());
		final int colour = isHovered ? WIRE_HIGHLIGHT : WIRE_COLOUR;
		for(CircuitPort p : termini)
			p.screenPosition(circuit).ifPresent(point -> 
				NodeRenderUtils.renderStraightLine(new Vec2f(point.x(), point.y()), endVec, WIRE_WIDTH, context, colour));
	}
}

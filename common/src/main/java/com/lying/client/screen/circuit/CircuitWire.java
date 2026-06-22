package com.lying.client.screen.circuit;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.joml.Vector2i;

import com.google.common.collect.Lists;
import com.lying.client.screen.NodeRenderUtils;
import com.lying.utility.geometry.LineSegment2f;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.util.DyeColor;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.ColorHelper;
import net.minecraft.util.math.Vec2f;

public class CircuitWire
{
	private static final char[] ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray();
	public static final Identifier TEXTURE = CircuitScreen.TEXTURE;
	public static final int WIRE_COLOUR		= 0xD0D0D0;
	public static final int WIRE_HIGHLIGHT	= 0xFFFFFF;
	private static final int WIRE_WIDTH	= 2;
	private static final int WIRE_PADDING	= 5;
	private final String name;
	private DyeColor color = DyeColor.WHITE;
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
	
	public void setColor(DyeColor colorIn)
	{
		color = colorIn;
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
	
	public boolean hasTerminus(CircuitPort port)
	{
		return termini.contains(port);
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
		
		// Calculate the boundaries of this wire and return false if the mouse is too far outside of it
		Vector2i boundsMin = new Vector2i(Integer.MAX_VALUE,Integer.MAX_VALUE), boundsMax = new Vector2i(Integer.MIN_VALUE,Integer.MIN_VALUE);
		Vector2i sum = new Vector2i(0,0);
		List<Vector2i> terminiMicros = Lists.newArrayList();
		for(CircuitPort port : termini)
		{
			Optional<Vector2i> pos = port.microPosition(circuit);
			if(pos.isPresent())
			{
				final Vector2i v = pos.get();
				sum = sum.add(v);
				terminiMicros.add(v);
				
				if(v.x() < boundsMin.x())
					boundsMin = new Vector2i(v.x, boundsMin.y());
				if(v.y() < boundsMin.y())
					boundsMin = new Vector2i(boundsMin.x(), v.y());
				
				if(v.x() > boundsMax.x())
					boundsMax = new Vector2i(v.x, boundsMax.y());
				if(v.y() > boundsMax.y())
					boundsMax = new Vector2i(boundsMax.x(), v.y());
			}
		}
		// Pad the boundaries to ensure we can still detect hovering over straight lines, which would otherwise have bounds with negligible width/height
		if(mouseX > boundsMax.x() + WIRE_PADDING || mouseX < boundsMin.x() - WIRE_PADDING)
			return false;
		if(mouseY > boundsMax.y() + WIRE_PADDING || mouseY < boundsMin.y() - WIRE_PADDING)
			return false;
		
		// Identify the median of all terminus positions
		final Vector2i median = new Vector2i(sum.x() / termini.size(), sum.y() / termini.size());
		
		final Vec2f mouse = new Vec2f(mouseX, mouseY);
		return 
				terminiMicros.stream()
				.anyMatch(p -> 
				{
					final LineSegment2f line = new LineSegment2f(new Vec2f(p.x(), p.y()), new Vec2f(median.x(), median.y));
					final Vec2f normal = line.normal();
					final LineSegment2f lineB = new LineSegment2f(mouse.add(normal.multiply(WIRE_WIDTH)), mouse.add(normal.negate().multiply(WIRE_WIDTH)));
					return line.intersectsAtAll(lineB);
				});
	}
	
	public void render(boolean isHovered, DrawContext context, Map<Vector2i,CircuitModule> circuit)
	{
		if(decapitated())
			return;
		
		final Vector2i median = medianPoint(circuit);
		
		// Render each terminus as a line connecting its screen position to the median
		final Vec2f endVec = new Vec2f(median.x(), median.y());
		
		final int colour = getColor(isHovered);
		for(CircuitPort p : termini)
			p.microPosition(circuit).ifPresent(point -> 
				NodeRenderUtils.renderStraightLine(new Vec2f(point.x(), point.y()), endVec, WIRE_WIDTH, context, colour));
		
		context.drawTexture(
				RenderLayer::getGuiTextured, 
				TEXTURE, 
				median.x() - 5, 
				median.y() - 5,
				68F, 
				0F, 
				10, 
				10, 
				256, 
				256, 
				colour);
	}
	
	/** Returns the median point between all terminus points within the given circuit */
	public Vector2i medianPoint(Map<Vector2i,CircuitModule> circuit)
	{
		Vector2i median = new Vector2i(0,0);
		for(CircuitPort p : termini)
		{
			Optional<Vector2i> point = p.microPosition(circuit);
			if(point.isEmpty())
				continue;
			median = median.add(point.get());
		}
		
		return new Vector2i(median.x() / termini.size(), median.y() / termini.size());
	}
	
	public int getColor(boolean isHovered)
	{
		int val = color.getEntityColor();
		double 
			r = (val >> 16) & 0xFF,
			g = (val >> 8) & 0xFF,
			b = (val & 0xFF);
		
		if(!isHovered)
		{
			r *= 0.75;
			g *= 0.75;
			b *= 0.75;
		}
		
		return ColorHelper.getArgb((int)r, (int)g, (int)b);
	}
}

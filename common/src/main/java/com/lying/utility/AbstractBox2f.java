package com.lying.utility;

import java.util.List;

import com.google.common.collect.Lists;

import net.minecraft.util.math.Vec2f;

public abstract class AbstractBox2f
{
	public abstract float minX();
	public abstract float minY();
	public abstract float maxX();
	public abstract float maxY();
	
	public abstract List<Line2f> asEdges();
	
	public abstract List<Vec2f> asPoints();
	
	public abstract String toString();
	
	public boolean contains(Vec2f vec)
	{
		// A singular point cannot possibly be within the box if it is outside the simplified bounds
		if(vec.x < minX() || vec.x > maxX() || vec.y < minY() || vec.y > maxY())
			return false;
		
		// Connect point to infinity in each cardinal direction
		for(Vec2f test : new Vec2f[] { new Vec2f(0, 1), new Vec2f(1, 0) })
		{
			// If the number of intersections is odd in any direction, the point must be inside the box
			
			Line2f linePos = new Line2f(vec, vec.add(test.multiply(Float.MAX_VALUE)));
			if(intersections(linePos)%2 > 0)
				return true;
			
			Line2f lineNeg = new Line2f(vec, vec.add(test.multiply(Float.MIN_VALUE)));
			if(intersections(lineNeg)%2 > 0)
				return true;
		}
		
		return false;
	}
	
	public boolean contains(Line2f line)
	{
		return contains(line.getLeft()) && contains(line.getRight());
	}
	
	/** Returns the number of edges intersected by the given line */
	public int intersections(Line2f line)
	{
		int tally = 0;
		for(Line2f edge : asEdges())
			if(edge.intersects(line))
				tally++;
		return tally;
	}
	
	/** Returns true if the given box intersects this one */
	public boolean intersects(AbstractBox2f box)
	{
		return box.asPoints().stream().anyMatch(this::contains) || asEdges().stream().anyMatch(e1 -> box.asEdges().stream().anyMatch(e1::intersects));
	}
	
	public boolean intersects(Line2f line)
	{
		// If the bounds contain either point of a line, it must intersect
		if(contains(line.getLeft()) || contains(line.getRight()))
			return true;
		
		// If the line intersects any boundary line of the bounds, it must intersect
		return asEdges().stream().anyMatch(line::intersects);
	}
	
	/** Returns a list of all 2D positions enclosed by this box */
	public final List<Vec2f> enclosedPositions()
	{
		List<Vec2f> points = Lists.newArrayList();
		for(float x=minX(); x<maxX(); x++)
			for(float y=minY(); y<maxY(); y++)
			{
				Vec2f point = new Vec2f(x,y);
				if(contains(point))
					points.add(point);
			}
		return points;
	}
	
	/** Offsets the box by the given vector */
	public abstract AbstractBox2f move(Vec2f vec);
	
	/** Multiplies all values of the box by the given value */
	public abstract AbstractBox2f mul(float val);
	
	/** Increases the length of all sides of the box by twice the given value */
	public abstract AbstractBox2f grow(float val);
	
	/** Rotates the box around its core position */
	public AbstractBox2f spin(float radians) { return this; }
	
	/** Rotates the box around [0,0] */
	public AbstractBox2f rotate(float radians) { return this; }
	
	/** Rotates the box around the given position */
	public AbstractBox2f rotateAround(Vec2f origin, float radians) { return this; }
}

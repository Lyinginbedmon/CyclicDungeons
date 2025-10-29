package com.lying.utility;

import java.util.List;
import java.util.function.Predicate;

import com.google.common.collect.Lists;

import net.minecraft.util.math.Vec2f;

public abstract class AbstractBox2f
{
	public abstract float minX();
	public abstract float minY();
	public abstract float maxX();
	public abstract float maxY();
	
	/** Returns a set of lines representing the edges of the box's bounds */
	public abstract List<LineSegment2f> asEdges();
	
	/** Returns the list of vectors representing the corners of the box */
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
			
			LineSegment2f linePos = new LineSegment2f(vec, vec.add(test.multiply(Float.MAX_VALUE)));
			if(intersections(linePos)%2 > 0)
				return true;
			
			LineSegment2f lineNeg = new LineSegment2f(vec, vec.add(test.multiply(Float.MIN_VALUE)));
			if(intersections(lineNeg)%2 > 0)
				return true;
		}
		
		return false;
	}
	
	public boolean contains(LineSegment2f line)
	{
		return contains(line.getLeft()) && contains(line.getRight());
	}
	
	/** Returns the number of edges intersected by the given line */
	public int intersections(LineSegment2f line)
	{
		int tally = 0;
		for(LineSegment2f edge : asEdges())
			if(LineSegment2f.doSegmentsIntersect(edge, line))
				tally++;
		return tally;
	}
	
	/** Returns true if the given box intersects this one */
	public boolean intersects(AbstractBox2f box)
	{
		return box.asPoints().stream().anyMatch(this::contains) || asEdges().stream().anyMatch(e1 -> box.asEdges().stream().anyMatch(e2 -> LineSegment2f.doSegmentsIntersect(e1, e2)));
	}
	
	public boolean intersects(LineSegment2f line)
	{
		// If the bounds contain either point of a line, it must intersect
		if(contains(line.getLeft()) || contains(line.getRight()))
			return true;
		
		// If the line intersects any boundary line of the bounds, it must intersect
		final Predicate<LineSegment2f> predicate = a -> LineSegment2f.doSegmentsIntersect(line, a);
		return asEdges().stream().anyMatch(predicate);
	}
	
	public Vec2f intercept(LineSegment2f line)
	{
		for(LineSegment2f face : asEdges())
		{
			Vec2f intercept = LineSegment2f.segmentIntercept(face, line);
			if(intercept != null)
				return intercept;
		}
		return null;
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

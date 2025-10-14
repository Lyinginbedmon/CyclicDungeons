package com.lying.utility;

import org.jetbrains.annotations.Nullable;
import org.joml.Vector2i;

import net.minecraft.util.math.Vec2f;

/** Handler class for 2D line segments */
public class Line2f
{
	// Components of the slope intercept equation of this line
	// y = mx + b OR x = a
	/** Slope of a non-vertical line or X of a vertical line */
	public final float m;
	/** Y intercept */
	public final float b;
	/** Whether this line is vertical and so uses the x = a equation format */
	public final boolean isVertical;
	public final boolean isHorizontal;
	
	public Line2f(Vector2i posA, Vector2i posB)
	{
		this(new Vec2f(posA.x, posA.y), new Vec2f(posB.x, posB.y));
	}
	
	public Line2f(Vec2f posA, Vec2f posB)
	{
		// Run = change in X value
		float run = posB.x - posA.x;
		// Rise = change in Y value
		float rise = posB.y - posA.y;
		
		// If run == 0, this line must be vertical
		isVertical = run == 0;
		// If rise == 0, this line must be horizontal
		isHorizontal = rise == 0;
		
		if(isVertical)
		{
			m = posA.x;
			b = 0;
		}
		else
		{
			m = run == 0 ? 0 : rise / run;
			b = posA.y - (posA.x * m);
		}
	}
	
	/** Returns true if these two lines have identical complex properties */
	public boolean equals(Line2f line)
	{
		return 
				isVertical == line.isVertical && 
				m == line.m && 
				b == line.b;
	}
	
	public String toString()
	{
		return "Line[" + asEquation() + "]";
	}
	
	public String asEquation()
	{
		float m = (float)((int)(this.m * 100)) / 100F;
		float b = (float)((int)(this.b * 100)) / 100F;
		
		if(isVertical)
			return "x = "+m;
		
		if(m == 0)
			return "y = "+b;
		else if(b == 0)
			return "y = "+m+"x";
		else
			return "y = "+m+"x + "+b;
	}
	
	public boolean contains(@Nullable Vec2f vec)
	{
		if(vec == null)
			return false;
		
		return
				isVertical ?
					vec.x == m :
				isHorizontal ?
					vec.y == b :
					atX(vec.x).distanceSquared(vec) == 0F;
	}
	
	/** Returns a direction vector of this line */
	public Vec2f direction()
	{
		if(isVertical)
			return new Vec2f(0, 1);
		else if(isHorizontal)
			return new Vec2f(0, b);
		else
			return atX(1).add(atX(0).negate());
	}
	
	/** Returns a vector at 90 degrees from the direction of this line */
	public Vec2f normal()
	{
		Vec2f d = direction().normalize();
		return new Vec2f(-d.y, d.x);
	}
	
	/** The position on this line at the given X coordinate */
	public Vec2f atX(float x)
	{
		return
				isVertical ?
					new Vec2f(m, x):
				isHorizontal ?
					new Vec2f(x, b) :
					new Vec2f(x, ((m * x) + b));
	}
	
	/** Returns true if the given lines are parallel */
	public static boolean areParallel(Line2f a, Line2f b)
	{
		return a.isVertical == b.isVertical && a.m == b.m;
	}
	
	public boolean intersects(Line2f other)
	{
		return 
				isSame(other) || 
				intercept(other) != null;
	}
	
	public boolean isSame(Line2f line2)
	{
		if(!areParallel(this, line2))
			return false;
		
		if(isVertical && line2.isVertical)
			return m == line2.m;
		else
			return b == line2.b;
	}
	
	public boolean isParallel(Line2f line)
	{
		// Two vertical or two horizontal lines must be parallel
		if(isVertical && line.isVertical)
			return true;
		else if(isHorizontal && line.isHorizontal)
			return true;
		// Two non-vertical lines are parallel if they share the same slope
		else if(!isVertical && !line.isVertical)
			return this.m == line.m;
		// One vertical and one non-vertical line cannot, by definition, be parallel
		else
			return false;
	}
	
	@Nullable
	public Vec2f intercept(Line2f other)
	{
		// Parallel lines by definition cannot intercept without overlapping
		if(isParallel(other))
			return null;
		
		// Handle one vertical and one non-vertical line
		if(isVertical != other.isVertical)
		{
			// X of vertical line determines y = mx + B of non-vertical line
			Line2f vert = isVertical ? this : other;
			Line2f hori = isVertical ? other : this;
			
			// Intercept occurs when the X of the horizontal line equals the X value of the vertical line
			return hori.atX(vert.m);
		}
		// Handle two non-vertical lines
		else
		{
			// y = ax+c and y = bx+d
			float 
				a = this.m, c = this.b,
				b = other.m, d = other.b;
			
			// Point at which these lines would intersect, according to this line's equation
			return atX((d - c) / (a - b));
		}
	}
}

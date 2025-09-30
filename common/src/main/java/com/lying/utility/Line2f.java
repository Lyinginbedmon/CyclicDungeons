package com.lying.utility;

import org.jetbrains.annotations.Nullable;

import net.minecraft.util.Pair;
import net.minecraft.util.math.Vec2f;

public class Line2f extends Pair<Vec2f, Vec2f>
{
	// Components of the slope intercept equation of this line
	// y = mx + b OR x = a
	public final float m, b;
	private final Pair<Float, Float> xRange, yRange;
	public final boolean isVertical;
	
	public Line2f(Vec2f posA, Vec2f posB)
	{
		super(posA, posB);
		xRange = new Pair<Float, Float>(Math.min(posA.x, posB.x), Math.max(posA.x, posB.x));
		yRange = new Pair<Float, Float>(Math.min(posA.y, posB.y), Math.max(posA.y, posB.y));
		
		float run = getRight().x - getLeft().x;
		float rise = getRight().y - getLeft().y;
		
		isVertical = run == 0;
		if(isVertical)
		{
			m = getLeft().x;
			b = 0;
		}
		else
		{
			m = run == 0 ? 0 : rise / run;
			b = getLeft().y - (getLeft().x * m);
		}
	}
	
	/** Returns true if these two lines have identical complex properties */
	public boolean equals(Line2f line)
	{
		return isVertical == line.isVertical && m == line.m && b == line.b && xRange.equals(line.xRange) && yRange.equals(line.yRange);
	}
	
	public double length() { return getLeft().add(getRight().negate()).length(); }
	
	public Vec2f atX(float x)
	{
		return new Vec2f(x, ((m * x) + b));
	}
	
	public boolean linksTo(Line2f line)
	{
		return getLeft().equals(line.getRight()) || getRight().equals(line.getRight()) || getLeft().equals(line.getLeft()) || getRight().equals(line.getLeft());
	}
	
	/** Returns true if this is the same line, without checking complex properties */
	public boolean simpleEquals(Line2f line)
	{
		return (getLeft().equals(line.getLeft()) && getRight().equals(line.getRight())) || (getLeft().equals(line.getRight()) && getRight().equals(getLeft()));
	}
	
	public Line2f offset(Vec2f offset)
	{
		return new Line2f(getLeft().add(offset), getRight().add(offset));
	}
	
	public Line2f scale(float scalar)
	{
		return new Line2f(getLeft().multiply(scalar), getRight().multiply(scalar));
	}
	
	/** Returns true if the given lines are parallel */
	public static boolean areParallel(Line2f a, Line2f b)
	{
		return a.isVertical == b.isVertical && a.m == b.m;
	}
	
	public AbstractBox2f bounds()
	{
		return new Box2f(xRange.getLeft(), yRange.getLeft(), xRange.getRight(), yRange.getRight());
	}
	
	public boolean intersects(Line2f line2)
	{
		return intercept(line2) != null;
	}
	
	public boolean isSame(Line2f line2)
	{
		return areParallel(this, line2) && inRange(line2.getLeft()) || inRange(line2.getRight());
	}
	
	/** Returns the point that this line intersects with the given line, if at all */
	@Nullable
	public Vec2f intercept(Line2f line2) { return intercept(line2, false); }
	
	@Nullable
	public Vec2f intercept(Line2f line2, boolean ignoreRange)
	{
		Vec2f intercept = null;
		// Two vertical lines = Parallel: No intercept
		if(isVertical && line2.isVertical)
		{
			// In the unlikely scenario that two vertical lines directly overlap, we treat them as side-by-side
			return null;
		}
		// Handle one vertical and one non-vertical line
		else if(isVertical != line2.isVertical)
		{
			// X of vertical line determines y = mx + B of non-vertical line
			// Intercept if within range of both
			
			Line2f vert = isVertical ? this : line2;
			Line2f hori = isVertical ? line2 : this;
			
			// Non-vertical line's position at the X coordinate of the vertical line
			intercept = hori.atX(vert.m);
		}
		// Handle two non-vertical lines
		else
		{
			// Return null if both lines have the same slope and are therefore parallel
			if(this.m == line2.m)
				return null;
			
			float a = this.m, b = line2.m;
			float c = this.b, d = line2.b;
			
			float x = (d - c) / (a - b);
			float y = (a * x) + c;
			
			// Point at which these lines would intersect, if they were of infinite length
			intercept = new Vec2f(x, y);
		}
		
		// Optional range check
		return (ignoreRange || (inRange(intercept) && line2.inRange(intercept))) ? intercept : null;
	}
	
	public boolean inRange(Vec2f point)
	{
		if(point.equals(getLeft()) || point.equals(getRight()))
			return false;
		
		if(xRange.getRight() != xRange.getLeft())
			if(point.x < xRange.getLeft() || point.x > xRange.getRight())
				return false;
		
		if(yRange.getRight() != yRange.getLeft())
			if(point.y < yRange.getLeft() || point.y > yRange.getRight())
				return false;
		
		return true;
	}
}

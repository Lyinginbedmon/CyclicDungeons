package com.lying.utility;

import org.jetbrains.annotations.Nullable;
import org.joml.Vector2i;

import net.minecraft.util.Pair;

public class Line2 extends Pair<Vector2i, Vector2i>
{
	// Components of the slope intercept equation of this line
	// y = mx + b OR x = a
	private final float m, b;
	private final Pair<Integer, Integer> xRange, yRange;
	private final boolean isVertical;
	
	public Line2(Vector2i posA, Vector2i posB)
	{
		super(posA, posB);
		xRange = new Pair<Integer, Integer>(Math.min(posA.x, posB.x), Math.max(posA.x, posB.x));
		yRange = new Pair<Integer, Integer>(Math.min(posA.y, posB.y), Math.max(posA.y, posB.y));
		
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
	public boolean equals(Line2 line)
	{
		return isVertical == line.isVertical && m == line.m && b == line.b && xRange.equals(line.xRange) && yRange.equals(line.yRange);
	}
	
	public boolean linksTo(Line2 line)
	{
		return getLeft().equals(line.getRight()) || getRight().equals(line.getRight()) || getLeft().equals(line.getLeft()) || getRight().equals(line.getLeft());
	}
	
	/** Returns true if this is the same line, without checking complex properties */
	public boolean simpleEquals(Line2 line)
	{
		return (getLeft().equals(line.getLeft()) && getRight().equals(line.getRight())) || (getLeft().equals(line.getRight()) && getRight().equals(getLeft()));
	}
	
	public Line2 offset(Vector2i offset)
	{
		return new Line2(Vector2iUtils.add(getLeft(), offset), Vector2iUtils.add(getRight(), offset));
	}
	
	public Line2 scale(int scalar)
	{
		return new Line2(Vector2iUtils.mul(getLeft(), scalar), Vector2iUtils.mul(getRight(), scalar));
	}
	
	/** Returns true if the given lines are parallel */
	public static boolean areParallel(Line2 a, Line2 b)
	{
		if(a.isVertical && b.isVertical)
			return true;
		else if(!a.isVertical && !b.isVertical)
			return a.m == b.m;
		return false;
	}
	
	public Box2 bounds()
	{
		return new Box2(xRange.getLeft(), yRange.getLeft(), xRange.getRight(), yRange.getRight());
	}
	
	public boolean intersects(Line2 line2)
	{
		return intercept(line2) != null;
	}
	
	/** Returns the point that this line intersects with the given line, if at all */
	@Nullable
	public Vector2i intercept(Line2 line2) { return intercept(line2, false); }
	
	@Nullable
	public Vector2i intercept(Line2 line2, boolean ignoreRange)
	{
		Vector2i intercept = null;
		if(isVertical && line2.isVertical)
		{
			// Two vertical lines = Parallel: No intercept
			// In the unlikely scenario that two vertical lines directly overlap, we treat them as side-by-side
			return null;
		}
		else if(isVertical != line2.isVertical)
		{
			// Handle one vertical and one non-vertical line
			// X of vertical line determines y = mx + B of non-vertical line
			// Intercept if within range of both
			
			Line2 vert = isVertical ? this : line2;
			Line2 hori = isVertical ? line2 : this;
			
			// Horizontal line's position at the X coordinate of the vertical line
			float x = vert.m;
			intercept = new Vector2i((int)x, (int)((hori.m * x) + hori.b));
		}
		else
		{
			// Return null if both lines have the same slope and are therefore parallel
			if(this.m == line2.m)
				return null;
			
			// Handle two non-vertical lines
			float a = this.m, b = line2.m;
			float c = this.b, d = line2.b;
			
			float x = (d - c) / (a - b);
			float y = (a * x) + c;
			
			// Point at which these lines would intersect, if they were of infinite length
			intercept = new Vector2i((int)x, (int)y);
		}
		
		// Optional range check
		return (ignoreRange || (inRange(intercept) && line2.inRange(intercept))) ? intercept : null;
	}
	
	public boolean inRange(Vector2i point)
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

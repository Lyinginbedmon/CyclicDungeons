package com.lying.utility;

import org.jetbrains.annotations.Nullable;
import org.joml.Vector2i;

import net.minecraft.util.math.Vec2f;

public class LineSegment2f extends Line2f
{
	protected final Vec2f left, right;
	protected final float minX, maxX, minY, maxY;
	
	public LineSegment2f(GridTile tileA, GridTile tileB)
	{
		this(new Vec2f(tileA.x, tileA.y).add(0.5F), new Vec2f(tileB.x, tileB.y).add(0.5F));
	}
	
	public LineSegment2f(Vector2i posA, Vector2i posB)
	{
		this(new Vec2f(posA.x, posA.y), new Vec2f(posB.x, posB.y));
	}
	
	public LineSegment2f(Vec2f leftIn, Vec2f rightIn)
	{
		super(leftIn, rightIn);
		this.left = leftIn;
		this.right = rightIn;
		
		minX = Math.min(leftIn.x, rightIn.x);
		maxX = Math.max(leftIn.x, rightIn.x);
		
		minY = Math.min(leftIn.y, rightIn.y);
		maxY = Math.max(leftIn.y, rightIn.y);
	}
	
	public String toString()
	{
		float aX = (float)(int)(left.x * 100) / 100F;
		float aY = (float)(int)(left.y * 100) / 100F;
		float bX = (float)(int)(right.x * 100) / 100F;
		float bY = (float)(int)(right.y * 100) / 100F;
		return "LineSegment[" +  aX + ", " + aY + " to "+ bX + ", " + bY + "]";
	}
	
	/** Returns true if these two lines have identical complex properties */
	public boolean equals(LineSegment2f line)
	{
		return 
				super.equals(line) && 
				minX == line.minX &&
				maxX == line.maxX &&
				minY == line.minY &&
				maxY == line.maxY;
	}
	
	/** Returns true if this is the same line, without checking complex properties */
	public boolean simpleEquals(LineSegment2f line)
	{
		return (left.equals(line.left) && right.equals(line.right)) || (left.equals(line.right) && right.equals(left));
	}
	
	public boolean contains(@Nullable Vec2f vec)
	{
		return vec != null && super.contains(vec) && inRange(vec);
	}
	
	public Vec2f direction()
	{
		return right.add(left.negate());
	}
	
	public Vec2f midPoint() { return left.add(direction().multiply(0.5F)); }
	
	public LineSegment2f localTo(Vec2f point)
	{
		return new LineSegment2f(getLeft().add(point.negate()), getRight().add(point.negate()));
	}
	
	public boolean inRange(Vec2f point)
	{
		float 
			x = point.x, 
			y = point.y;
		
		return 
			x >= minX && x <= maxX &&
			y >= minY && y <= maxY;
	}
	
	public Vec2f getLeft() { return left; }
	
	public Vec2f getRight() { return right; }
	
	public Vec2f[] toPoints() { return new Vec2f[] { left, right }; }
	
	public boolean linksTo(LineSegment2f line)
	{
		Vec2f[] otherPoints = line.toPoints();
		for(Vec2f point : toPoints())
			for(Vec2f otherPoint : otherPoints)
				if(point.distanceSquared(otherPoint) == 0F)
					return true;
		
		return false;
	}
	
	public LineSegment2f offset(Vec2f offset)
	{
		return new LineSegment2f(left.add(offset), right.add(offset));
	}
	
	public LineSegment2f scale(float scalar)
	{
		return new LineSegment2f(left.multiply(scalar), right.multiply(scalar));
	}
	
	public float length()
	{
		Vec2f dir = direction();
		float a = Math.abs(dir.x);
		float b = Math.abs(dir.y);
		return (float)Math.sqrt(a * a + b * b);
	}
	
	public float manhattanLength()
	{
		Vec2f dir = direction();
		return Math.abs(dir.x) + Math.abs(dir.y);
	}
	
	/** Returns the grid-aligned bounds occupied by this line segment */
	public AbstractBox2f bounds()
	{
		return new Box2f(minX, maxX, minY, maxY);
	}
	
	public boolean intersectsAtAll(LineSegment2f other)
	{
		return contains(other.left) || contains(other.right) || doSegmentsIntersect(this, other);
	}
	
	public boolean isSame(LineSegment2f line2)
	{
		return super.isSame(line2) && inRange(line2.left) && inRange(line2.right);
	}
	
	@Nullable
	public LineSegment2f clip(AbstractBox2f box)
	{
		if(!box.intersects(this))
			return this;
		
		boolean 
			leftInside = box.contains(left),
			rightInside = box.contains(right);
		
		// Eliminate entirely if line is wholly within the box
		if(leftInside && rightInside)
			return null;
		
		// Exchange internal point with intercept
		if(leftInside || rightInside)
		{
			Vec2f intercept = null;
			for(LineSegment2f edge : box.asEdges())
			{
				intercept = segmentIntercept(edge, this);
				if(intercept != null)
					break;
			}
			
			if(intercept != null)
			{
				LineSegment2f clipped = new LineSegment2f(leftInside ? intercept : left, rightInside ? intercept : right);
				return clipped.length() > 0 ? clipped : null;
			}
		}
		
		return this;
	}
	
	/** Returns the point that this line intersects with the given line, if at all */
	@Nullable
	public Vec2f intercept(Line2f line2)
	{
		return intercept(line2, false);
	}
	
	@Nullable
	public Vec2f intercept(Line2f other, boolean ignoreRange)
	{
		Vec2f intercept = super.intercept(other);
		return ignoreRange || contains(intercept) ? intercept : null;
	}
	
	/** Returns an intercept point that is mutually-inclusive of both line segments */
	@Nullable
	public static Vec2f segmentIntercept(LineSegment2f a, LineSegment2f b)
	{
		Vec2f interceptA = a.intercept(b, false);
		Vec2f interceptB = b.intercept(a, false);
		if(interceptA == null || interceptB == null)
			return null;
		
		// Floating point errors mess with precision, so we average whatever point both equations produces
		return interceptA.add(interceptB).multiply(0.5F);
	}
	
	public static boolean doSegmentsIntersect(LineSegment2f a, LineSegment2f b)
	{
		return segmentIntercept(a, b) != null;
	}
	
	public LineSegment2f turnClockwise()
	{
		Vec2f a = new Vec2f(-left.y, left.x);
		Vec2f b = new Vec2f(-right.y, right.x);
		return new LineSegment2f(a, b);
	}
	
	public LineSegment2f turnCClockwise()
	{
		Vec2f a = new Vec2f(left.y, -left.x);
		Vec2f b = new Vec2f(right.y, -right.x);
		return new LineSegment2f(a, b);
	}
}

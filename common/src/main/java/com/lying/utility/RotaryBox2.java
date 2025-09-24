package com.lying.utility;

import org.joml.Vector2i;

import net.minecraft.util.math.Vec2f;

public class RotaryBox2
{
	private Vector2i[] points = new Vector2i[4];
	private Line2[] edges = new Line2[4];
	
	private int minX, minY, maxX, maxY;
	
	public RotaryBox2(Vector2i aIn, Vector2i bIn, Vector2i cIn, Vector2i dIn)
	{
		points[0] = aIn;
		points[1] = bIn;
		points[2] = cIn;
		points[3] = dIn;
		updateValues();
	}
	
	public RotaryBox2 clone()
	{
		return new RotaryBox2(points[0], points[1], points[2], points[3]);
	}
	
	public void updateValues()
	{
		calculateSimpleBounds();
		buildEdges();
	}
	
	protected void calculateSimpleBounds()
	{
		int mx = Integer.MAX_VALUE, my = Integer.MAX_VALUE, mX = Integer.MIN_VALUE, mY = Integer.MIN_VALUE;
		for(Vector2i vec : points)
		{
			int x = vec.x();
			if(x > mX)
				mX = x;
			if(x < mx)
				mx = x;
			
			int y = vec.y();
			if(y > mY)
				mY = y;
			if(y < my)
				my = y;
		}
		minX = mx;
		minY = my;
		maxX = mX;
		maxY = mY;
	}
	
	protected void buildEdges()
	{
		edges = new Line2[] 
				{
					new Line2(points[0], points[1]), 
					new Line2(points[1], points[2]), 
					new Line2(points[2], points[3]), 
					new Line2(points[3], points[0])
				};
	}
	
	public static RotaryBox2 enclosing(Vector2i a, Vector2i b)
	{
		int minX = Math.min(a.x(), b.x());
		int minY = Math.min(a.y(), b.y());
		int maxX = Math.max(a.x(), b.x());
		int maxY = Math.max(a.y(), b.y());
		return new RotaryBox2(new Vector2i(minX, minY), new Vector2i(maxX, minY), new Vector2i(maxX, maxY), new Vector2i(minX, maxY));
	}
	
	public static RotaryBox2 ofSize(int width, int height)
	{
		return new RotaryBox2(new Vector2i(0, 0), new Vector2i(width, 0), new Vector2i(width, height), new Vector2i(0, height));
	}
	
	public static RotaryBox2 fromLine(Line2 line, int height)
	{
		Vec2f p1 = new Vec2f(line.getLeft().x, line.getLeft().y);
		Vec2f p2 = new Vec2f(line.getRight().x, line.getRight().y);
		
		Vec2f delta = p2.add(p1.negate()).normalize();
		delta = new Vec2f(-delta.y, delta.x);
		int minH = -height / 2;
		int maxH = minH + height;
		
		Vec2f a = p1.add(delta.multiply(minH));
		Vec2f b = p2.add(delta.multiply(minH));
		Vec2f c = p2.add(delta.multiply(maxH));
		Vec2f d = p1.add(delta.multiply(maxH));
		
		return new RotaryBox2(
				new Vector2i((int)a.x, (int)a.y),
				new Vector2i((int)b.x, (int)b.y),
				new Vector2i((int)c.x, (int)c.y),
				new Vector2i((int)d.x, (int)d.y)
				);
	}
	
	/** Reduces this box to its grid-aligned equivalent */
	public Box2 simplify()
	{
		return new Box2(minX, maxX, minY, maxY);
	}
	
	public Line2[] edges()
	{
		Line2[] set = new Line2[4];
		for(int i=0; i<4; i++)
			set[i] = edges[i];
		return set;
	}
	
	/**
	 * Returns true if the given point is within the simple bounds of this box.<br>
	 * This does not implicitly mean it is within the box itself.
	 */
	public boolean inBounds(Vector2i vec)
	{
		return simplify().contains(vec);
	}
	
	public boolean contains(Vector2i vec)
	{
		// A singular point cannot possibly be within the box if it is outside the simplified bounds
		if(vec.x < minX || vec.x > maxX || vec.y < minY || vec.y > maxY)
			return false;
		
		// Connect point to infinity in each cardinal direction
		for(Vector2i test : new Vector2i[] { new Vector2i(0, 1), new Vector2i(1, 0) })
		{
			// If the number of intersections is odd in any direction, the point must be inside the box
			
			Line2 linePos = new Line2(vec, Vector2iUtils.add(vec, Vector2iUtils.mul(test, Integer.MAX_VALUE)));
			if(intersections(linePos)%2 > 0)
				return true;
			
			Line2 lineNeg = new Line2(vec, Vector2iUtils.add(vec, Vector2iUtils.mul(test, Integer.MIN_VALUE)));
			if(intersections(lineNeg)%2 > 0)
				return true;
		}
		
		return false;
	}
	
	public int intersections(Line2 line)
	{
		int tally = 0;
		for(Line2 edge : edges)
			if(edge.intersects(line))
				tally++;
		return tally;
	}
	
	public boolean intersects(Line2 line)
	{
		return intersections(line) > 0;
	}
	
	public boolean intersects(Box2 box)
	{
		if(simplify().intersects(box))
		{
			for(Vector2i point : points)
				if(box.contains(point))
					return true;
			
			for(Line2 edge : edges)
				if(box.intersects(edge))
					return true;
		}
		return false;
	}
	
	/** Offsets the box by the given vector */
	public RotaryBox2 move(Vector2i vec)
	{
		for(int i=0; i<4; i++)
			points[i] = Vector2iUtils.add(points[i], vec);
		updateValues();
		return this;
	}
	
	/** Rotates the box around its core position */
	public RotaryBox2 spin(double radians)
	{
		return rotateAround(new Vector2i(minX + (maxX - minX) / 2, minY + (maxY - minY) / 2), radians);
	}
	
	/** Rotates the box around [0,0] */
	public RotaryBox2 rotate(double radians)
	{
		for(int i=0; i<4; i++)
			points[i] = Vector2iUtils.rotate(points[i], radians);
		updateValues();
		return this;
	}
	
	/** Rotates the box around the given position */
	public RotaryBox2 rotateAround(Vector2i origin, double radians)
	{
		return move(Vector2iUtils.negate(origin)).rotate(radians).move(origin);
	}
	
	/** Multiplies all values of the box by the given value */
	public RotaryBox2 mul(double val)
	{
		for(int i=0; i<4; i++)
			points[i] = Vector2iUtils.mul(points[i], val);
		updateValues();
		return this;
	}
	
	/** Scales the box around its core position */
	public RotaryBox2 scale(double val)
	{
		Vector2i origin = new Vector2i(minX + (maxX - minX) / 2, minY + (maxY - minY) / 2);
		return move(Vector2iUtils.negate(origin)).mul(val).move(origin);
	}
}

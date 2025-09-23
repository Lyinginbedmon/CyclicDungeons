package com.lying.utility;

import org.joml.Vector2i;

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
		int mx = Integer.MIN_VALUE, my = Integer.MIN_VALUE, mX = Integer.MAX_VALUE, mY = Integer.MAX_VALUE;
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
		Vector2i p1 = line.getLeft();
		Vector2i p2 = line.getRight();
		
		Vector2i delta = Vector2iUtils.rotate(Vector2iUtils.normalize(Vector2iUtils.subtract(p1, p2)), Math.toRadians(90D));
		int minH = -height / 2;
		int maxH = minH + height;
		
		return new RotaryBox2(
				Vector2iUtils.add(p1, Vector2iUtils.mul(delta, minH)),
				Vector2iUtils.add(p2, Vector2iUtils.mul(delta, minH)),
				Vector2iUtils.add(p2, Vector2iUtils.mul(delta, maxH)),
				Vector2iUtils.add(p1, Vector2iUtils.mul(delta, maxH))
				);
	}
	
	/** Reduces this box to its grid-aligned equivalent */
	public Box2 simplify()
	{
		return new Box2(minX, maxX, minY, maxY);
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
		// A singular point cannot possibly be within the box if it is outside the simple bounds
		if(!inBounds(vec))
			return false;
		
		Vector2i[] testSet = new Vector2i[] 
				{
					new Vector2i(0, Integer.MAX_VALUE),
					new Vector2i(0, Integer.MIN_VALUE),
					new Vector2i(Integer.MAX_VALUE, 0),
					new Vector2i(Integer.MIN_VALUE)
				};
		
		// Connect point to infinity in each cardinal direction
		for(Vector2i test : testSet)
		{
			Line2 line = new Line2(vec, test.x() == 0 ? new Vector2i(vec.x(), test.y()) : new Vector2i(test.y(), vec.y()));
			int tally = 0;
			for(Line2 edge : edges)
				if(edge.intersects(line))
					tally++;
			
			// If the number of intersections is odd, the point must be inside the box
			if(tally%2 > 0)
				return true;
		}
		
		return false;
	}
	
	public boolean intersects(Line2 line)
	{
		for(Line2 edge : edges)
			if(edge.intersects(line))
				return true;
		
		return false;
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

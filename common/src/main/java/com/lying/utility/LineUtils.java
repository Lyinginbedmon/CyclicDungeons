package com.lying.utility;

import java.util.List;

import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector2i;

import com.google.common.collect.Lists;
import com.lying.blueprint.BlueprintRoom;
import com.lying.init.CDLoggers;
import com.lying.worldgen.Tile;

import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec2f;

public class LineUtils
{
	public static final DebugLogger LOGGER = CDLoggers.PLANAR;
	private static final int TILE_SIZE = Tile.TILE_SIZE;
	
	public static List<LineSegment2f> simplifyLines(List<LineSegment2f> linesIn)
	{
		if(linesIn.size() <= 1)
			return linesIn;
		
		List<LineSegment2f> output = Lists.newArrayList();
		boolean anyMerged = false;
		for(int i=1; i<linesIn.size(); i++)
		{
			LineSegment2f a = linesIn.get(i - 1);
			LineSegment2f b = linesIn.get(i);
			
			LineSegment2f merged = mergeLines(a,b);
			if(merged == null)
				output.add(a);
			else
			{
				output.add(merged);
				anyMerged = true;
			}
		}
		return anyMerged == true ? simplifyLines(output) : linesIn;
	}
	
	@Nullable
	public static LineSegment2f mergeLines(LineSegment2f a, LineSegment2f b)
	{
		if(!a.linksTo(b) || !a.alignsWith(b))
			return null;
		
		List<Vec2f> points = Lists.newArrayList();
		for(Vec2f point : new Vec2f[] {a.getLeft(), a.getRight(), b.getLeft(), b.getRight()})
		{
			if(points.contains(point))
				points.remove(point);
			else
				points.add(point);
		}
		
		return new LineSegment2f(points.getFirst(), points.getLast());
	}
	
	/** Attempts to generate a viable deterministic line, from the most elegant to the least */
	public static List<LineSegment2f> trialLines(BlueprintRoom start, BlueprintRoom end)
	{
		// The bounding boxes of both rooms
		Box2f 
			startBox = (Box2f)start.worldBounds(), 
			endBox = (Box2f)end.worldBounds();
		
		final Pair<Direction,Vec2f> startPoints = findClosestTile(startBox, end.position());
		final Pair<Direction,Vec2f> endPoints = findClosestTile(endBox, start.position());
		
		// The points along the boundary edges closest together
		Vec2f startDoor = startPoints.getRight();
		Vec2f endDoor = endPoints.getRight();
		
		// If the line between doors is straight, we only need one line to describe this passage
		LineSegment2f startToEnd = new LineSegment2f(startDoor, endDoor);
		if(startToEnd.isStraightLine())
			return List.of(startToEnd);
		
		// The boundary face direction of the above points
		Direction startExit = startPoints.getLeft();
		Direction endExit = endPoints.getLeft();
		
		Vec2f outOfStart = startDoor.add(new Vec2f(startExit.getOffsetX(), startExit.getOffsetZ()).multiply(TILE_SIZE * 1.5F));
		Vec2f outOfEnd = endDoor.add(new Vec2f(endExit.getOffsetX(), endExit.getOffsetZ()).multiply(TILE_SIZE * 1.5F));
		if(outOfStart.distanceSquared(endDoor) == startDoor.distanceSquared(outOfStart))
			return List.of(new LineSegment2f(startDoor, outOfStart), new LineSegment2f(outOfStart, endDoor));
		
		// Comprehensive calculation
		return List.of(new LineSegment2f(startDoor, outOfStart), new LineSegment2f(outOfStart, outOfEnd), new LineSegment2f(outOfEnd, endDoor));
	}
	
	private static Pair<Direction,Vec2f> findClosestTile(Box2f bounds, Vector2i target)
	{
		Vec2f destination = new Vec2f(target.x, target.y);
		float minDist = Float.MAX_VALUE;
		Pair<Direction,Vec2f> best = null;
		for(Direction face : Direction.Type.HORIZONTAL)
		{
			LineSegment2f edge = bounds.getEdge(face);
			int tileLength = (int)(edge.length() / TILE_SIZE);
			
			// Offset half a tile back into the bounds of the room, since we're trying to approximate a tile position
			Vec2f offset = new Vec2f(face.getOffsetX(), face.getOffsetZ()).multiply(TILE_SIZE * -0.5F);
			Vec2f dir = edge.direction().normalize();
			switch(tileLength)
			{
				// Rooms shouldn't ever have a side smaller than 3 tiles, but we include them here for error handling
				case 1:
				case 2:
					Vec2f p2 = edge.left.add(dir.multiply(TILE_SIZE * 0.5F));
					float d2 = p2.add(offset).distanceSquared(destination);
					if(d2 < minDist || best == null)
					{
						best = Pair.of(face, p2);
						minDist = d2;
					}
					break;
				case 3:
					Vec2f p3 = edge.left.add(edge.direction().multiply(0.5F));
					float d3 = p3.add(offset).distanceSquared(destination);
					if(d3 < minDist || best == null)
					{
						best = Pair.of(face, p3);
						minDist = d3;
					}
					break;
				default:
					// Start at index=1 and stop at len-1 to exclude corner tiles
					for(int i=1; i<tileLength - 1; i++)
					{
						Vec2f pN = edge.left.add(dir.multiply((i + 0.5F) * TILE_SIZE));
						float dN = pN.add(offset).distanceSquared(destination);
						if(dN < minDist || best == null)
						{
							best = Pair.of(face, pN);
							minDist = dN;
						}
					}
					break;
			}
		}
		return best;
	}
}
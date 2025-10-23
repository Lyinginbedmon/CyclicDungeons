package com.lying.utility;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Predicate;

import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.Nullable;

import com.google.common.collect.Lists;
import com.lying.blueprint.BlueprintPassage;
import com.lying.blueprint.BlueprintRoom;
import com.lying.init.CDLoggers;
import com.lying.worldgen.Tile;

import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec2f;

public class LineUtils
{
	public static final DebugLogger LOGGER = CDLoggers.PLANAR;
	private static final List<BiFunction<Vec2f,Vec2f,ArrayList<LineSegment2f>>> providers = List.of(
			LineUtils::xFirstCurved,
			LineUtils::yFirstCurved,
			LineUtils::xFirst,
			LineUtils::yFirst,
			LineUtils::diagonal
			);
	private static final float TILE_SIZE = Tile.TILE_SIZE;
	
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
	public static List<LineSegment2f> trialLines(BlueprintRoom start, BlueprintRoom end, Predicate<List<LineSegment2f>> qualifier)
	{
		/**
		 * Draw a straight line from start to end
		 * Identify which bounding box faces the line passes through
		 * Find the closest tile "slot" along those faces to the intersection point
		 * Connect identified slots
		 */
		
		// The bounding boxes of both rooms, which are always grid-aligned
		Box2f 
			startBox = (Box2f)start.bounds(), 
			endBox = (Box2f)end.bounds();
		
		// The direct line between the origin points of both rooms
		LineSegment2f direct = new LineSegment2f(start.tilePosition().mul(Tile.TILE_SIZE), end.tilePosition().mul(Tile.TILE_SIZE));
		
		Optional<Pair<LineSegment2f,Vec2f>> 
			startIntercepts = findFaceIntersection(direct, startBox),
			endIntercepts = findFaceIntersection(direct, endBox);
		
		if(startIntercepts.isEmpty() || endIntercepts.isEmpty())
			return List.of(direct);
		
		// The face of each room's bounding box that the direct line intersects
		LineSegment2f 
			startFace = startIntercepts.get().getLeft(), 
			endFace = endIntercepts.get().getLeft();
		
		// The point on those faces the direct line intersects
		Vec2f 
			startIntercept = startIntercepts.get().getRight(),
			endIntercept = endIntercepts.get().getRight();
		
		// Door tiles = nearest multiple of tile size from either end of the face
		Vec2f 
			startDir = startIntercept.add(startFace.getLeft().negate()),
			endDir = endIntercept.add(endFace.getLeft().negate());
		
		// Adjust direction of door from face start when it is too close
		if(startDir.length() == 0F)
			startDir = startFace.direction().normalize();
		if(endDir.length() == 0F)
			endDir = endFace.direction().normalize();
		
		float startLen = fitLengthToTile(startDir, startFace);
		float endLen = fitLengthToTile(endDir, endFace);
		
		// Middle of the tile grid position where each end of the path enters/exits the room bounds
		Vec2f 
			startDoor = startFace.getLeft().add(startDir.normalize().multiply(startLen)),
			endDoor = endFace.getLeft().add(endDir.normalize().multiply(endLen));
		LineSegment2f startToEnd = new LineSegment2f(startDoor, endDoor);
		
		// If the intercepts are as close together as rooms can be, reduce the passage to a direct connection
		float manhattan = Math.abs(endDoor.x - startDoor.x) + Math.abs(endDoor.y - startDoor.y); 
		if(manhattan <= TILE_SIZE && qualifier.test(List.of(startToEnd)))
			return List.of(startToEnd);
		
		/**
		 * Step outside of each box based on perpendicular of intercepted face
		 * Then connect each external point
		 */
		
		float outLen = TILE_SIZE * 2;
		Vec2f 
			outOfStart = startDoor.add(findPerpendicularExteriorDir(startFace, startBox).multiply(outLen)), 
			outOfEnd = endDoor.add(findPerpendicularExteriorDir(endFace, endBox).multiply(outLen));
		
		if(outOfStart.distanceSquared(endDoor) < outOfEnd.distanceSquared(endDoor) && qualifier.test(List.of(startToEnd)))
			return List.of(startToEnd);
		
		LineSegment2f startLine = new LineSegment2f(startDoor, outOfStart);
		LineSegment2f endLine = new LineSegment2f(outOfEnd, endDoor);
		if(endBox.intersects(startLine) && qualifier.test(List.of(startLine)))
			return List.of(startLine);
		else if(outOfStart.distanceSquared(outOfEnd) < 1 && startLine.isParallel(endLine) && qualifier.test(List.of(startToEnd)))
			return List.of(startToEnd);
		
		return List.of(
				startLine, 
				new LineSegment2f(outOfStart, outOfEnd), 
				endLine);
	}
	
	/** Attempts to generate a viable deterministic line, from the most elegant to the least */
	public static List<GridTile> trialTiles(BlueprintRoom start, BlueprintRoom end)
	{
		/**
		 * Draw a straight line from start to end
		 * Identify which bounding box faces the line passes through
		 * Find the closest tile "slot" along those faces to the intersection point
		 * Connect identified slots
		 */
		
		// The bounding boxes of both rooms
		Box2f 
			startBox = (Box2f)start.bounds(), 
			endBox = (Box2f)end.bounds();
		
		// The direct line between the origin points of both rooms
		LineSegment2f direct = new LineSegment2f(start.tilePosition().mul(Tile.TILE_SIZE).toVec2f(), end.tilePosition().mul(Tile.TILE_SIZE).toVec2f());
		
		Optional<Pair<LineSegment2f,Vec2f>> 
			startIntercepts = findFaceIntersection(direct, startBox),
			endIntercepts = findFaceIntersection(direct, endBox);
		
		if(startIntercepts.isEmpty() || endIntercepts.isEmpty())
			return List.of();
		
		// The face of each room's bounding box that the direct line intersects
		LineSegment2f 
			startEdge = startIntercepts.get().getLeft(), 
			endEdge = endIntercepts.get().getLeft();
		
		// The point on those faces the direct line intersects
		Vec2f 
			startIntercept = startIntercepts.get().getRight(),
			endIntercept = endIntercepts.get().getRight();
		
		// Door tiles = nearest multiple of tile size from either end of the face
		Vec2f 
			startDir = startIntercept.add(startEdge.getLeft().negate()),
			endDir = endIntercept.add(endEdge.getLeft().negate());
		
		// Adjust direction of door from face start when it is too close
		if(startDir.length() == 0F)
			startDir = startEdge.direction().normalize();
		if(endDir.length() == 0F)
			endDir = endEdge.direction().normalize();
		
		float startLen = fitLengthToTile(startDir, startEdge);
		float endLen = fitLengthToTile(endDir, endEdge);
		
		// Middle of the tile grid position where each end of the path enters/exits the room bounds
		Vec2f 
			startDoor = startEdge.getLeft().add(startDir.normalize().multiply(startLen)),
			endDoor = endEdge.getLeft().add(endDir.normalize().multiply(endLen));
		
		GridTile startTile = GridTile.fromVec(startDoor);
		GridTile endTile = GridTile.fromVec(endDoor);
		
		// If the start and end tiles are the same, return the singular tile
		if(startTile.equals(endTile))
			return List.of(startTile);
		// If distance between start and end tiles is 1, they are immediately adjacent
		else if(startTile.manhattanDistance(endTile) == 1)
			return List.of(startTile, endTile);
		
		GridTile outOfStart = startTile.offset(identifyEdgeOfFace(startEdge, startBox));
		GridTile outOfEnd = endTile.offset(identifyEdgeOfFace(endEdge, endBox));
		if(outOfStart.distance(endTile) == 1)
			return List.of(startTile, outOfStart, endTile);
		else if(outOfEnd.distance(startTile) == 1)
			return List.of(startTile, outOfEnd, endTile);
		
		// Comprehensive calculation
		List<GridTile> tilesOccupied = Lists.newArrayList();
		tilesOccupied.add(startTile);
		
		LineSegment2f startToEnd = new LineSegment2f(outOfStart, outOfEnd);
		tilesOccupied.addAll(BlueprintPassage.lineToTiles(startToEnd));
		
		tilesOccupied.add(endTile);
		return tilesOccupied;
	}
	
	private static Vec2f findPerpendicularExteriorDir(LineSegment2f face, Box2f bounds)
	{
		Vec2f faceMid = face.midPoint();
		Vec2f normal = face.normal();
		
		Vec2f faceLeft = faceMid.add(normal);
		Vec2f faceRight = faceMid.add(normal.negate());
		
		if(bounds.contains(faceLeft) && bounds.contains(faceRight))
			return new Vec2f(1,1);
		else if(bounds.contains(faceLeft))
			return normal.negate();
		else
			return normal;
	}
	
	private static Direction identifyEdgeOfFace(LineSegment2f face, Box2f bounds)
	{
		for(Direction edge : Direction.Type.HORIZONTAL)
			if(face.equals(bounds.getEdge(edge)))
				return edge;
		
		return Direction.NORTH;
	}
	
	@Nullable
	private static Vec2f getExteriorNormal(LineSegment2f face, AbstractBox2f bounds)
	{
		Vec2f faceMid = face.midPoint();
		LineSegment2f perpendicular = face.localTo(faceMid).turnClockwise();
		
		Vec2f left = perpendicular.getLeft().normalize();
		Vec2f right = perpendicular.getRight().normalize();
		
		Vec2f faceLeft = faceMid.add(left);
		Vec2f faceRight = faceMid.add(right);
		if(bounds.contains(faceLeft) && bounds.contains(faceRight))
			return null;
		else if(bounds.contains(faceLeft))
			return right;
		else
			return left;
	}
	
	private static Optional<Pair<LineSegment2f,Vec2f>> findFaceIntersection(LineSegment2f direct, AbstractBox2f bounds)
	{
		for(LineSegment2f face : bounds.asEdges())
		{
			Vec2f intercept = LineSegment2f.segmentIntercept(face, direct);
			if(intercept != null)
				return Optional.of(Pair.of(face, intercept));
		}
		
		LOGGER.error("Line created from within bounding box does not intersect it? This isn't possible");
		return Optional.empty();
	}
	
	private static float fitLengthToTile(Vec2f intercept, LineSegment2f face)
	{
		/**
		 * Calculate distance to face segment start from intercept
		 * Convert distance to multiple of TILE_SIZE
		 * Offset by half a tile to centre line in tile grid
		 */
		float len = intercept.length();
		
		// Offset position half a tile towards the midpoint of the segment for alignment
		float offset = TILE_SIZE * 0.5F * (intercept.length() > (face.length() * 0.5F) ? -1 : 1);
		
		return (float)(MathHelper.clamp(Math.floor(len / TILE_SIZE), 1, Math.floor(face.length() / TILE_SIZE) - 1) * TILE_SIZE) + offset;
	}
	
	public static List<LineSegment2f> trialLines(Vec2f start, Vec2f end, Predicate<List<LineSegment2f>> qualifier)
	{
		ArrayList<LineSegment2f> line = Lists.newArrayList();
		
		for(BiFunction<Vec2f,Vec2f,ArrayList<LineSegment2f>> provider : providers)
		{
			line = provider.apply(start, end);
			line.removeIf(l -> l.length() == 0F);
			line.removeIf(Objects::isNull);
			
			if(line.isEmpty())
				continue;
			
			if(line.size() == 1 || qualifier.test(line))
				return line;
		}
		
		return line;
	}
	
	public static ArrayList<LineSegment2f> diagonal(Vec2f start, Vec2f end)
	{
		// TODO Restrict angular sharpness to avoid 45-degree lines
		return Lists.newArrayList(new LineSegment2f(start, end));
	}
	
	public static ArrayList<LineSegment2f> xFirst(Vec2f start, Vec2f end)
	{
		Vec2f offset = end.add(start.negate());
		Vec2f mid = start.add(new Vec2f(offset.x, 0F));
		return Lists.newArrayList(
				new LineSegment2f(start, mid),
				new LineSegment2f(mid, end)
				);
	}
	
	public static ArrayList<LineSegment2f> yFirst(Vec2f start, Vec2f end)
	{
		Vec2f offset = end.add(start.negate());
		Vec2f mid = start.add(new Vec2f(0F, offset.y));
		return Lists.newArrayList(
				new LineSegment2f(start, mid),
				new LineSegment2f(mid, end)
				);
	}
	
	public static ArrayList<LineSegment2f> xFirstCurved(Vec2f start, Vec2f end)
	{
		LineSegment2f direct = new LineSegment2f(start, end);
		if(Math.abs(direct.m) != 1)
			return Lists.newArrayList();
		
		ArrayList<LineSegment2f> lines = Lists.newArrayList();
		
		Vec2f toX = new Vec2f(end.x - start.x, 0F).multiply(0.5F);
		Vec2f toY = new Vec2f(0F, end.y - start.y).multiply(0.5F);
		
		Vec2f a = start.add(toX);
		Vec2f b = end.add(toY.negate());
		lines.add(new LineSegment2f(start, a));
		lines.add(new LineSegment2f(a, b));
		lines.add(new LineSegment2f(b, end));
		return lines;
	}
	
	public static ArrayList<LineSegment2f> yFirstCurved(Vec2f start, Vec2f end)
	{
		LineSegment2f direct = new LineSegment2f(start, end);
		if(Math.abs(direct.m) != 1)
			return Lists.newArrayList();
		
		ArrayList<LineSegment2f> lines = Lists.newArrayList();
		
		Vec2f toX = new Vec2f(end.x - start.x, 0F).multiply(0.5F);
		Vec2f toY = new Vec2f(0F, end.y - start.y).multiply(0.5F);
		
		Vec2f a = start.add(toY);
		Vec2f b = end.add(toX.negate());
		lines.add(new LineSegment2f(start, a));
		lines.add(new LineSegment2f(a, b));
		lines.add(new LineSegment2f(b, end));
		return lines;
	}
}
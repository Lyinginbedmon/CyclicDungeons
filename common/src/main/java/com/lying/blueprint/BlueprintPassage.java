package com.lying.blueprint;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Predicate;

import org.joml.Vector2i;

import com.google.common.collect.Lists;
import com.lying.init.CDTiles;
import com.lying.utility.AbstractBox2f;
import com.lying.utility.CompoundBox2f;
import com.lying.utility.Line2f;
import com.lying.utility.RotaryBox2f;
import com.lying.worldgen.Tile;
import com.lying.worldgen.TileSet;

import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec2f;

public class BlueprintPassage
{
	public static final Map<Tile,Float> PASSAGE_TILE_SET = Map.of(
			CDTiles.FLOOR.get(), 10000F,
			CDTiles.AIR.get(), 10F
			);
	
	public static final int PASSAGE_WIDTH = 3;
	private final BlueprintRoom parent, child;
	private final Line2f line;
	private final CompoundBox2f box;
	
	public BlueprintPassage(BlueprintRoom a, BlueprintRoom b)
	{
		this(a, b, PASSAGE_WIDTH);
	}

	public BlueprintPassage(BlueprintRoom a, BlueprintRoom b, float width)
	{
		this(a, b, new Line2f(new Vec2f(a.position().x, a.position().y), new Vec2f(b.position().x, b.position().y)), width);
	}
	
	public BlueprintPassage(BlueprintRoom a, BlueprintRoom b, Line2f lineIn, float width)
	{
		parent = a;
		child = b;
		line = lineIn;
		box = new CompoundBox2f();
		for(Line2f l : asLines())
			box.add(RotaryBox2f.fromLine(l, width));
	}
	
	public BlueprintRoom parent() { return parent; }
	
	public BlueprintRoom child() { return child; }
	
	/** Returns this passage as a singular line */
	public Line2f asLine() { return line; }
	
	/** Returns this passage as a set of one or more lines */
	public List<Line2f> asLines()
	{		
		return LineUtils.trialLines(line.getLeft(), line.getRight(), this::isLineViable);
	}
	
	private boolean isLineViable(List<Line2f> line)
	{
		if(line.isEmpty() || line.stream().anyMatch(Objects::isNull))
			return false;
		// In the event of a single-length line, guarantee viability
		else if(line.size() == 1)
			return true;
		
		AbstractBox2f firstB = parent.bounds().grow(1F);
		if(line.subList(1, line.size()).stream().anyMatch(firstB::intersects))
			return false;
		
		AbstractBox2f lastB = child.bounds().grow(1F);
		if(line.subList(0, line.size() - 1).stream().anyMatch(lastB::intersects))
			return false;
		
		return true;
	}
	
	public AbstractBox2f asBox() { return box; }
	
	public TileSet asTileSet(BlockPos origin, int height, List<AbstractBox2f> boundaries)
	{
		BlockPos start = new BlockPos((int)line.getLeft().x, 0, (int)line.getLeft().y);
		BlockPos end = new BlockPos((int)line.getRight().x, height * Tile.TILE_SIZE, (int)line.getRight().y);
		
		// TODO Lock off tiles inside or colliding with other rooms
		List<BlockPos> points = Lists.newArrayList();
		final Predicate<BlockPos> intersect = p -> boundaries.stream().anyMatch(bounds -> 
			p.getX() >= bounds.minX() && p.getX() <= bounds.maxX() &&
			p.getY() >= bounds.minY() && p.getY() <= bounds.maxY());
		
		BlockPos.Mutable.iterate(start, end).forEach(p -> 
		{
			if(intersect.test(p))
				return;
			
			points.add(p.toImmutable());
		});
		
		TileSet map = new TileSet();
		points.stream().map(p -> 
		{
			BlockPos point = p.subtract(start);
			return new BlockPos(point.getX() / Tile.TILE_SIZE, point.getY() / Tile.TILE_SIZE, point.getZ() / Tile.TILE_SIZE);
		}).forEach(map::addToVolume);
		
		return map;
	}
	
	public void build(BlockPos origin, ServerWorld world, List<AbstractBox2f> boundaries)
	{
//		TileSet map = asTileSet(PASSAGE_WIDTH, boundaries);
//		if(map.isEmpty() || !map.hasBlanks())
//			return;
//		
//		TileGenerator.generate(map, PASSAGE_TILE_SET, world.getRandom());
		
//		// Add enclosed positions to volume where they exceed the boundaries of rooms
//		final Predicate<Vec2f> isExterior = p -> boundaries.stream().noneMatch(b -> b.contains(p));
//		final List<Vec2f> positions = box.enclosedPositions();
//		if(positions.stream().noneMatch(isExterior))
//			return;
//		
//		positions.stream()
//			.filter(isExterior)
//			.forEach(p -> map.addToVolume(origin.add((int)p.x, 0, (int)p.y)));
//		
		Vec2f parentPos = line.getLeft();
		Vec2f childPos = line.getRight();
		
		// Populate map
		Vec2f current = parentPos;
		while(current.distanceSquared(childPos) > 0)
		{
			double minDist = Double.MAX_VALUE;
			Direction face = Direction.NORTH;
			for(Direction facing : Direction.Type.HORIZONTAL)
			{
				double dist = current.add(new Vec2f(facing.getOffsetX(), facing.getOffsetZ())).distanceSquared(childPos);
				if(minDist > dist)
				{
					face = facing;
					minDist = dist;
				}
			}
			
			current = current.add(new Vec2f(face.getOffsetX(), face.getOffsetZ()));
			BlockPos pos = origin.add((int)current.x, 0, (int)current.y);
//			if(map.contains(pos))
//				map.put(pos, CDTiles.FLOOR.get());
//			else
				Tile.tryPlace(Blocks.GRAY_CONCRETE_POWDER.getDefaultState(), pos, world);
		}
		
		// Generate
//		BlockPos start = new BlockPos((int)line.getLeft().x, origin.getY(), (int)line.getLeft().y);
//		map.generate(start, world);
	}
	
	/** Returns true if the given point is either end of this passage */
	public boolean isTerminus(Vec2f point)
	{
		return line.getLeft().equals(point) || line.getRight().equals(point);
	}
	
	/** Returns true if the given room is either intended end of this passage */
	public boolean isTerminus(BlueprintRoom room)
	{
		Vector2i pos = room.position();
		return isTerminus(new Vec2f(pos.x, pos.y));
	}
	
	public boolean isTerminus(BlueprintPassage b) { return isTerminus(b.line.getLeft()) || isTerminus(b.line.getRight()); }
	
	public boolean intersects(BlueprintPassage b)
	{
		if(canShareSpaceWith(b))
		{
			// Allow intersection if both passages start from the same parent
			// This promotes the generation of junctions and reduces overall doorway counts
			return false;
		}
		
		List<Line2f> linesA = asLines();
		List<Line2f> linesB = b.asLines();
		return linesA.stream().anyMatch(l -> linesB.stream().anyMatch(l::intersects));
	}
	
	public boolean canShareSpaceWith(BlueprintPassage b)
	{
		return b.parent.equals(parent) && b.child.metadata().depth() == child.metadata().depth();
	}
	
	/** Returns true if this passage intersects with any other passages in the given chart */
	public boolean hasIntersections(List<BlueprintRoom> chart)
	{
		List<BlueprintPassage> paths = Blueprint.getPassages(chart);
		return paths.stream()
				.filter(p -> !isTerminus(p))
				.anyMatch(this::intersects);
	}
	
	/** Returns true if this passage intersects any unrelated rooms in the given chart */
	public boolean hasTunnels(List<BlueprintRoom> chart)
	{
		return chart.stream()
				.filter(r -> !isTerminus(r))
				.map(BlueprintRoom::bounds)
				.anyMatch(this.box::intersects);
	}
	
	private static class LineUtils
	{
		private static final List<BiFunction<Vec2f,Vec2f,ArrayList<Line2f>>> providers = List.of(
				LineUtils::xFirstCurved,
				LineUtils::yFirstCurved,
				LineUtils::xFirst,
				LineUtils::yFirst,
				LineUtils::diagonal
				);
		
		/** Attempts to generate a viable deterministic line, from the most elegant to the least */
		public static List<Line2f> trialLines(Vec2f start, Vec2f end, Predicate<List<Line2f>> qualifier)
		{
			ArrayList<Line2f> line = Lists.newArrayList();
			for(BiFunction<Vec2f,Vec2f,ArrayList<Line2f>> provider : providers)
			{
				line = provider.apply(start, end);
				line.removeIf(l -> l.length() == 0F);
				
				if(qualifier.test(line))
					return line;
			}
			
			return line;
		}
		
		private static ArrayList<Line2f> diagonal(Vec2f start, Vec2f end)
		{
			return Lists.newArrayList(new Line2f(start, end));
		}
		
		private static ArrayList<Line2f> xFirst(Vec2f start, Vec2f end)
		{
			Vec2f offset = end.add(start.negate());
			Vec2f mid = start.add(new Vec2f(offset.x, 0F));
			return Lists.newArrayList(
					new Line2f(start, mid),
					new Line2f(mid, end)
					);
		}
		
		private static ArrayList<Line2f> yFirst(Vec2f start, Vec2f end)
		{
			Vec2f offset = end.add(start.negate());
			Vec2f mid = start.add(new Vec2f(0F, offset.y));
			return Lists.newArrayList(
					new Line2f(start, mid),
					new Line2f(mid, end)
					);
		}
		
		private static ArrayList<Line2f> xFirstCurved(Vec2f start, Vec2f end)
		{
			Line2f direct = new Line2f(start, end);
			if(Math.abs(direct.m) != 1)
				return Lists.newArrayList();
			
			ArrayList<Line2f> lines = Lists.newArrayList();
			
			Vec2f toX = new Vec2f(end.x - start.x, 0F).multiply(0.5F);
			Vec2f toY = new Vec2f(0F, end.y - start.y).multiply(0.5F);
			
			Vec2f a = start.add(toX);
			Vec2f b = end.add(toY.negate());
			lines.add(new Line2f(start, a));
			lines.add(new Line2f(a, b));
			lines.add(new Line2f(b, end));
			return lines;
		}
		
		private static ArrayList<Line2f> yFirstCurved(Vec2f start, Vec2f end)
		{
			Line2f direct = new Line2f(start, end);
			if(Math.abs(direct.m) != 1)
				return Lists.newArrayList();
			
			ArrayList<Line2f> lines = Lists.newArrayList();
			
			Vec2f toX = new Vec2f(end.x - start.x, 0F).multiply(0.5F);
			Vec2f toY = new Vec2f(0F, end.y - start.y).multiply(0.5F);
			
			Vec2f a = start.add(toY);
			Vec2f b = end.add(toX.negate());
			lines.add(new Line2f(start, a));
			lines.add(new Line2f(a, b));
			lines.add(new Line2f(b, end));
			return lines;
		}
	}
}

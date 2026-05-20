package com.lying.graph;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import org.jetbrains.annotations.Nullable;
import org.joml.Vector2i;

import com.google.common.collect.Lists;
import com.lying.CyclicDungeons;
import com.lying.blueprint.Blueprint;
import com.lying.blueprint.BlueprintPassage;
import com.lying.blueprint.BlueprintRoom;
import com.lying.grid.GridTile;
import com.lying.init.CDLoggers;
import com.lying.utility.geometry.AbstractBox2f;
import com.lying.utility.logging.DebugLogger;
import com.mojang.datafixers.util.Pair;

import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;

/** Utilities for organising a blueprint, prior to scrunching */
public abstract class GraphOrganiser
{
	public static final DebugLogger LOGGER = CDLoggers.PLANAR;
	private static final int GRID_SIZE = 30;
	
	/** Returns a comparator of rooms by their total number of descendant rooms */
	public static final Comparator<BlueprintRoom> descendantSort(List<BlueprintRoom> chart)
	{
		return (a,b) -> 
		{
			int desA = a.descendantCount(chart);
			int desB = b.descendantCount(chart);
			return desA > desB ? -1 : desA < desB ? 1 : 0;
		};
	}
	
	public static final Comparator<BlueprintRoom> parentSort(List<BlueprintRoom> chart)
	{
		return (a,b) -> 
		{
			Optional<UUID> aParents = a.parentID();
			Optional<UUID> bParents = b.parentID();
			
			// If parents identical, don't move
			if(aParents.isPresent() && bParents.isPresent() && aParents.get().equals(bParents.get()))
				return 0;
			else
			{
				int desA = a.descendantCount(chart);
				int desB = b.descendantCount(chart);
				return desA > desB ? -1 : desA < desB ? 1 : 0;
			}
		};
	}
	
	public final void organise(Blueprint chart, Random rand)
	{
		LOGGER.info(" # Applying organiser to {}:{} planar graph", chart.size(), chart.maxDepth());
		chart.setOrganised(false);
		
		// Clears all preceding structural information
		chart.forEach(n -> n.setPosition(0, 0));
		
		applyLayout(chart, rand);
		chart.setOrganised(true);
		chart.clearPassageCache();
	}
	
	public abstract void applyLayout(Blueprint chart, Random rand);
	
	public static List<AbstractBox2f> getBounds(Collection<BlueprintRoom> chart)
	{
		return chart.stream().map(BlueprintRoom::tileBounds).toList();
	}
	
	/** Returns a list of all paths between the given nodes, with post-processing applied */
	public static List<BlueprintPassage> getFinalisedPassages(Blueprint chart)
	{
		return mergePassages(chart.passages(), getBounds(chart));
	}
	
	/** Returns a list of all paths between the given nodes, without post-processing */
	public static List<BlueprintPassage> getPassages(Collection<BlueprintRoom> chart)
	{
		List<BlueprintPassage> paths = Lists.newArrayList();
		List<BlueprintRoom> rooms = Lists.newArrayList(chart);
		rooms.sort((a,b) -> a.metadata().depth() < b.metadata().depth() ? -1 : a.metadata().depth() > b.metadata().depth() ? 1 : 0);
		
		for(BlueprintRoom n : rooms)
			n.getChildren(chart).stream().map(c -> new BlueprintPassage(n, c)).forEach(paths::add);
		return paths;
	}
	
	public static List<BlueprintPassage> mergePassages(Collection<BlueprintPassage> pathsIn, Collection<AbstractBox2f> bounds)
	{
		LOGGER.info(" # Merging entwining paths");
		
		List<BlueprintPassage> pathsToMerge = Lists.newArrayList();
		
		// Subtract the bounds from all paths to limit to outside of rooms
		pathsToMerge.addAll(pathsIn.stream().filter(Objects::nonNull).toList());
		
		// Merge all paths together with applicable paths
		List<BlueprintPassage> mergedPaths = Lists.newArrayList();
		while(!pathsToMerge.isEmpty())
		{
			BlueprintPassage path = pathsToMerge.removeFirst();
			List<BlueprintPassage> merge = pathsToMerge.stream().filter(path::canMergeWith).toList();
			if(!merge.isEmpty())
				merge.forEach(path::mergeWith);
			mergedPaths.add(path);
			pathsToMerge.removeAll(merge);
		}
		
		LOGGER.info(" ## Merging complete: reduced from {} to {}", pathsIn.size(), mergedPaths.size());
		return mergedPaths;
	}
	
	public static class Poisson extends GraphOrganiser
	{
		public static Poisson create() { return new Poisson(); }
		
		public void applyLayout(Blueprint chart, Random rand)
		{
			// Find largest radius within dungeon
			int diameter = 1;
			for(BlueprintRoom room : chart)
			{
				Vector2i size = room.metadata().size();
				int longestSide = Math.max(size.x, size.y);
				if(longestSide > diameter)
					diameter = longestSide;
			}
			final int radius = Math.ceilDiv(diameter, 2) + 1;
			
			PoissonGrid fish = new PoissonGrid(radius, 16);
			fish.generateTo(chart.size() * chart.size(), rand);
			
			List<GridTile> points = Lists.newArrayList(fish.values());
			for(int d=0; d<=chart.maxDepth(); d++)
				for(BlueprintRoom room : chart.byDepth(d))
				{
					if(points.isEmpty())
					{
						CyclicDungeons.LOGGER.warn(" ? Poisson organiser ran out of points to organise dungeon");
						return;
					}
					
					GridTile pos;
					if(d > 0)
					{
						// Select point closest to parent position
						GridTile parentPos = room.getParent(chart).get().tilePosition();
						pos = points.getFirst();
						int dist = pos.manhattanDistance(parentPos);
						for(GridTile tile : points)
							if(tile.manhattanDistance(parentPos) < dist)
							{
								dist = tile.manhattanDistance(parentPos);
								pos = tile;
							}
					}
					else
					{
						// Set point at 0,0
						pos = GridTile.ZERO;
					}
					room.setTilePosition(pos);
					points.remove(pos);
				}
		}
		
		public static class PoissonGrid
		{
			// Precalculated offset positions, for immediate-neighbourhood
			private static List<GridTile> HARD_OFFSETS = Lists.newArrayList
					(
						new GridTile(0, 0),
						new GridTile(-1, -1),
						new GridTile(0, -1),
						new GridTile(1, -1),
						new GridTile(-1, 0),
						new GridTile(1, 0),
						new GridTile(-1, 1),
						new GridTile(0, 1),
						new GridTile(1, 1)
					);
			// Precalculated offset positions, for outer-neighbourhood
			private static List<GridTile> SOFT_OFFSETS = Lists.newArrayList
					(
						new GridTile(-2, -2),
						new GridTile(-1, -2),
						new GridTile(0, -2),
						new GridTile(1, -2),
						new GridTile(2, -2),
						new GridTile(2, -1),
						new GridTile(2, 0),
						new GridTile(2, 1),
						new GridTile(2, 2),
						new GridTile(1, 2),
						new GridTile(0, 2),
						new GridTile(-1, 2),
						new GridTile(-2, 2),
						new GridTile(-2, 1),
						new GridTile(-2, 0),
						new GridTile(-2, -1)
					);
			private static final Point INITIAL = Point.of(GridTile.ZERO, GridTile.ZERO);
			/** List of points that are still evaluable */
			private final List<Point> activePoints = Lists.newArrayList(INITIAL);
			/** Map of background grid tiles to foreground grid tiles */
			private final Map<GridTile, GridTile> background = new HashMap<>();
			
			final int radius;
			final double cellLength, diameter;
			final int samples;
			
			public PoissonGrid(int radiusIn, int samplesIn)
			{
				radius = radiusIn;
				diameter = radius * 2;
				cellLength = Math.sqrt(radiusIn);
				samples = samplesIn;
				
				log(INITIAL);
			}
			
			/** Returns true if the given position in the background grid is occupied */
			public boolean contains(GridTile vector) { return background.keySet().stream().anyMatch(vector::equals); }
			
			@Nullable
			public GridTile get(GridTile key)
			{
				for(Entry<GridTile, GridTile> entry : background.entrySet())
					if(entry.getKey().manhattanDistance(key) == 0)
						return entry.getValue();
				return null;
			}
			
			/** Returns all points within this foreground grid */
			public List<GridTile> values() { return Lists.newArrayList(background.values()); }
			
			/** Converts the given vector to a corresponding position in the background grid */
			public GridTile toBackground(GridTile vector)
			{
				int x = (int)Math.floor((double)vector.x / cellLength);
				int y = (int)Math.floor((double)vector.y / cellLength);
				return new GridTile(x, y);
			}
			
			protected void log(Point pair) { background.put(pair.getFirst(), pair.getSecond()); }
			
			public void generateTo(int size, Random rand)
			{
				while(background.size() < size && !activePoints.isEmpty())
					iterate(rand);
			}
			
			public void iterate(Random rand)
			{
				if(activePoints.isEmpty())
					return;
				
				Point point = activePoints.size() == 1 ? activePoints.getFirst() : activePoints.get(rand.nextInt(activePoints.size()));
				
				boolean shouldRemove = true;
				GridTile xForeground = point.getSecond();
				for(int i=0; i<samples; i++)
				{
					double dirX = (rand.nextDouble() - 0.5F) * 2;
					double dirY = (rand.nextDouble() - 0.5F) * 2;
					Vec3d dir = new Vec3d(dirX, 0, dirY).normalize().multiply(radius * (1 + rand.nextDouble()));
					
					final GridTile sampleForeground = new GridTile(xForeground.x + (int)dir.x, xForeground.y + (int)dir.z);
					if(sampleForeground.y < 0)
						continue;
					
					// Check occupancies around background tile
					final GridTile sampleBackground = toBackground(sampleForeground);
					
					// Check if the immediate neighbourhood is occupied
					if(HARD_OFFSETS.stream().anyMatch(o -> contains(sampleBackground.add(o))))
						continue;
					// Check if the wider neighbourhood has any points too close
					else if(SOFT_OFFSETS.stream()
							.anyMatch(o -> 
							{
								GridTile tile = sampleBackground.add(o);
								return contains(tile) ? get(tile).distance(sampleForeground) < diameter : false;
							}))
							continue;
					
					Point pair = Point.of(sampleBackground, sampleForeground);
					activePoints.add(pair);
					log(pair);
					
					shouldRemove = false;
				}
				
				if(shouldRemove)
					activePoints.remove(point);
			}
			
			private static class Point extends Pair<GridTile,GridTile>
			{
				protected Point(GridTile first, GridTile second)
				{
					super(first, second);
				}
				
				public static Point of(GridTile background, GridTile foreground)
				{
					return new Point(background, foreground);
				}
			}
		}
	}
	
	public static class Tree extends GraphOrganiser
	{
		public static Tree create() { return new Tree(); }
		
		public void applyLayout(Blueprint chart, Random rand)
		{
			final Comparator<BlueprintRoom> descSort = parentSort(chart);
			final Comparator<BlueprintRoom> childSort = (a,b) -> 
			{
				int aChild = a.childrenCount();
				int bChild = b.childrenCount();
				return aChild > bChild ? -1 : aChild < bChild ? 1 : 0;
			};
			
			for(int depth=0; depth<=chart.maxDepth(); depth++)
			{
				List<BlueprintRoom> nodes = Lists.newArrayList();
				nodes.addAll(chart.byDepth(depth));
				nodes.sort(descSort);
				
				// Identify parents in previous tier and sort by number of children in this tier
				List<BlueprintRoom> prevTierParents = Lists.newArrayList();
				nodes.stream().map(r -> r.getParent(chart)).filter(Optional::isPresent).map(Optional::get).forEach(prevTierParents::add);
				prevTierParents = prevTierParents.stream().distinct().sorted(childSort).toList();
				
				// Precalculate slot positions
				int y = depth * GRID_SIZE;
				int rowWidth = (nodes.size() - 1) * GRID_SIZE;
				int x = -rowWidth / 2;
				List<Vector2i> slots = Lists.newArrayList();
				for(int i=0; i<nodes.size(); i++)
				{
					slots.add(new Vector2i(x, y));
					x += GRID_SIZE;
				}
				
				// Place child rooms close to their parent in the previous tier
				for(BlueprintRoom parent : prevTierParents)
				{
					Vector2i parentPos = parent.position();
					for(BlueprintRoom child : nodes.stream().filter(parent::isChild).toList())
					{
						Vector2i pos = null;
						double minDist = Double.MAX_VALUE;
						for(Vector2i slot : slots)
						{
							double d = slot.distance(parentPos);
							if(d < minDist)
							{
								minDist = d;
								pos = slot;
							}
						}
						
						child.setPosition(pos);
						slots.remove(pos);
						nodes.remove(child);
					}
				}
				
				// If any nodes have no parent, assign remaining slot
				if(!nodes.isEmpty())
					for(BlueprintRoom node : nodes)
						node.setPosition(slots.remove(0));
			}
		}
	}
	
	public static class Circular extends GraphOrganiser
	{
		public static Circular create() { return new Circular(); }
		
		public void applyLayout(Blueprint chart, Random rand)
		{
			final Comparator<BlueprintRoom> descSort = descendantSort(chart);
			for(int step = 1; step <= chart.maxDepth(); step++)
			{
				// Rooms at this radius
				List<BlueprintRoom> set = Lists.newArrayList();
				set.addAll(chart.byDepth(step));
				set.sort(descSort);
				
				// Calculate all slots available at this radius
				int radius = step * GRID_SIZE;
				double circ = 2 * (Math.PI * radius);
				
				/**
				 *  Number of slots = circumference / (grid size * 2)
				 *  eg. assuming grid size = 30: 3, 6, 9, 12, 15, 18, etc.
				 */
				int pop = Math.max((int)(circ / (GRID_SIZE * 2)), set.size());
				double rot = Math.toRadians(180D / pop);
				double cos = Math.cos(rot), sin = Math.sin(rot);
				
				GridTile point = new GridTile(radius, 0);
				List<GridTile> positions = Lists.newArrayList();
				for(int i=0; i<pop; i++)
				{
					positions.add(point);
					point = new GridTile(
							(int)(point.x * cos - point.y * sin),
							(int)(point.x * sin + point.y * cos)
							);
				}
				
				// Place all rooms in positions closest to their parents at their radius
				for(BlueprintRoom room : set)
				{
					if(positions.isEmpty())
						break;
					
					GridTile parent = room.getParentPosition(chart);
					positions.sort((a,b) -> 
					{
						double dA = a.distance(parent);
						double dB = b.distance(parent);
						if(dA == dB)
						{
							int mA = Math.abs(a.x) + Math.abs(a.y);
							int mB = Math.abs(b.x) + Math.abs(b.y);
							if(mA == mB)
							{
								int yA = Math.abs(a.y);
								int yB = Math.abs(b.y);
								return yA > yB ? -1 : yA < yB ? 1 : 0;
							}
							
							return mA < mB ? -1 : mA > mB ? 1 : 0;
						}
						else
							return dA < dB ? -1 : dA > dB ? 1 : 0;
					});
					
					room.setTilePosition(positions.removeFirst());
				}
			}
		}
	}
}

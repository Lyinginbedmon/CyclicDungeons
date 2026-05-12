package com.lying.blueprint;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import org.joml.Vector2i;

import com.google.common.collect.Lists;
import com.lying.grid.GridTile;
import com.lying.init.CDLoggers;
import com.lying.utility.geometry.AbstractBox2f;
import com.lying.utility.logging.DebugLogger;

import net.minecraft.util.math.random.Random;

/** Utilities for organising a blueprint, prior to scrunching */
public abstract class BlueprintOrganiser
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
	
	public static class Poisson extends BlueprintOrganiser
	{
		public static Poisson create() { return new Poisson(); }
		
		public void applyLayout(Blueprint chart, Random rand)
		{
			// Establish super-grid of points
			PoissonGrid superGrid = new PoissonGrid();
			for(int i=0; i<=chart.maxDepth(); i++)
				for(BlueprintRoom room : chart.byDepth(i))
				{
					if(superGrid.isEmpty())
						break;
					
					// Assign rooms to points within super-grid
					GridTile pos = superGrid.open().getFirst();
					if(room.hasParent())
					{
						GridTile parent = room.getParentPosition(chart);
						
						List<GridTile> candidates = Lists.newArrayList();
						int minDist = Integer.MAX_VALUE;
						for(GridTile opt : superGrid.open())
						{
							int dist = opt.manhattanDistance(parent);
							if(dist < minDist)
							{
								candidates.clear();
								candidates.add(opt);
								minDist = dist;
							}
							else if(dist == minDist)
								candidates.add(opt);
						}
						
						pos = 
								candidates.size() == 1 ? 
									candidates.getFirst() : 
									candidates.get(rand.nextInt(candidates.size()));
					}
					
					room.setTilePosition(pos);
					superGrid.close(pos);
				}
			
			// Find largest radius within dungeon
			int diameter = 1;
			for(BlueprintRoom room : chart)
			{
				Vector2i size = room.metadata().size();
				int longestSide = Math.max(size.x, size.y);
				if(longestSide > diameter)
					diameter = longestSide;
			}
			final int radius = Math.ceilDiv(diameter, 2);
			
			// Convert super-grid positions to tile grid positions
			for(BlueprintRoom room : chart)
			{
				final Vector2i pos = room.position();
				int x = pos.x;
				int y = pos.y;
				
				// Convert to standard grid position
				x *= radius;
				y *= radius;
				
				// Offset randomly to add variety
				x += (int)((rand.nextFloat() - 0.5F) * radius);
				y += (int)((rand.nextFloat() - 0.5F) * radius);
				
				room.setPosition(x, y);
			}
		}
		
		private static class PoissonGrid
		{
			// Precalculated offset positions
			private static GridTile[] OFFSETS = new GridTile[] 
					{
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
					};
			private List<GridTile> 
				closed = Lists.newArrayList(), 
				open = Lists.newArrayList(GridTile.ZERO);
			
			public boolean isEmpty() { return open.isEmpty(); }
			
			public List<GridTile> open() { return open; }
			
			public void close(GridTile tile)
			{
				closed.add(tile);
				
				// Remove positions invalidated by this placement
				open.removeIf(this::isInvalidated);
				
				// Append new viable positions
				for(GridTile offset : OFFSETS)
				{
					GridTile point = tile.add(offset);
					if(
							point.y < 0 ||
							closed.contains(point) || 
							open.contains(point) ||
							isInvalidated(point))
						;
					else
						open.add(point);
				}
			}
			
			protected boolean isInvalidated(GridTile tile)
			{
				return closed.stream().anyMatch(t -> 
							t.manhattanDistance(tile) <= 2 && 
							Math.abs(tile.x - t.x) < 2 && 
							Math.abs(tile.y - t.y) < 2);
			}
		}
	}
	
	public static class Tree extends BlueprintOrganiser
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
				nodes.stream().map(r -> r.getParents(chart)).forEach(prevTierParents::addAll);
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
	
	public static class Circular extends BlueprintOrganiser
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

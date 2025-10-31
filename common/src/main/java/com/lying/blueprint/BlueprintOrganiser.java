package com.lying.blueprint;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

import org.apache.commons.lang3.tuple.Pair;

import com.google.common.collect.Lists;
import com.lying.grid.GridTile;
import com.lying.init.CDLoggers;
import com.lying.utility.AbstractBox2f;
import com.lying.utility.CDUtils;
import com.lying.utility.DebugLogger;

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
	
	public final void organise(Blueprint chart, Random rand)
	{
		LOGGER.info(" # Applying organiser to {}:{} planar graph", chart.size(), chart.maxDepth());
		
		// Clears all preceding structural information
		chart.forEach(n -> n.setPosition(0, 0));
		
		applyLayout(chart, rand);
	}
	
	public abstract void applyLayout(Blueprint chart, Random rand);
	
	public static List<AbstractBox2f> getBounds(Collection<BlueprintRoom> chart)
	{
		return chart.stream().map(BlueprintRoom::tileBounds).toList();
	}
	
	/** Returns a list of all paths between the given nodes, with post-processing applied */
	public static List<BlueprintPassage> getFinalisedPassages(Blueprint chart)
	{
		List<BlueprintPassage> passages = chart.passages();
		// FIXME Re-enable after merging is finalised
//		mergePassages(passages, getBounds(chart));
		return passages;
	}
	
	/** Returns a list of all paths between the given nodes, without post-processing */
	public static List<BlueprintPassage> getPassages(Collection<BlueprintRoom> chart)
	{
		List<BlueprintPassage> paths = Lists.newArrayList();
		for(BlueprintRoom n : chart)
			n.getChildren(chart).stream().map(c -> new BlueprintPassage(n, c)).forEach(paths::add);
		return paths;
	}
	
	public static List<BlueprintPassage> mergePassages(Collection<BlueprintPassage> pathsIn, Collection<AbstractBox2f> bounds)
	{
		LOGGER.info(" # Merging entwining paths");
		
		List<BlueprintPassage> pathsToMerge = Lists.newArrayList();
		
		// Subtract the bounds from all paths to limit to outside of rooms
		pathsToMerge.addAll(pathsIn.stream().map(p -> 
		{
			bounds.forEach(p::exclude);
			return p;
		}).filter(Objects::nonNull).toList());
		
		// Merge all paths together with applicable paths
		List<BlueprintPassage> mergedPaths = Lists.newArrayList();
		while(!pathsToMerge.isEmpty())
		{
			BlueprintPassage path = pathsToMerge.removeFirst();
			List<BlueprintPassage> merge = pathsToMerge.stream().filter(path::canMergeWith).toList();
			if(!merge.isEmpty())
			{
				merge.forEach(path::mergeWith);
				bounds.forEach(path::exclude);
			}
			mergedPaths.add(path);
			pathsToMerge.removeAll(merge);
		}
		
		LOGGER.info(" ## Merging complete: reduced from {} to {}", pathsIn.size(), mergedPaths.size());
		return mergedPaths;
	}
	
	public static class Tree extends BlueprintOrganiser
	{
		public static Tree create() { return new Tree(); }
		
		public void applyLayout(Blueprint chart, Random rand)
		{
			final Comparator<BlueprintRoom> descSort = descendantSort(chart);
			for(int depth=0; depth<=chart.maxDepth(); depth++)
			{
				List<BlueprintRoom> nodes = Lists.newArrayList();
				nodes.addAll(chart.byDepth(depth));
				nodes.sort(descSort);
				
				int y = depth * GRID_SIZE;
				int rowWidth = (nodes.size() - 1) * GRID_SIZE;
				int x = -rowWidth / 2;
				for(BlueprintRoom node : nodes)
				{
					node.setPosition(x, y);
					x += GRID_SIZE;
				}
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
	
	public static abstract class Grid extends BlueprintOrganiser
	{
		protected abstract GridPosition[] moveSet(GridTile position);
		
		@FunctionalInterface
		public interface GridPosition
		{
			public GridTile get(GridTile position, Map<GridTile,BlueprintRoom> occupancies, int gridSize);
		}
		
		public void applyLayout(Blueprint chart, Random rand)
		{
			Map<GridTile, BlueprintRoom> gridMap = new HashMap<>();
			
			for(int step = 0; step <= chart.maxDepth(); step++)
				organiseByGrid(chart, step, gridMap, this::moveSet, GRID_SIZE, rand);
			
			if(gridMap.size() != chart.size())
				LOGGER.warn("Grid layout size ({}) differs from input blueprint size ({})", gridMap.size(), chart.size());
		}
		
		private static void organiseByGrid(Blueprint chart, int depth, Map<GridTile,BlueprintRoom> gridMap, Function<GridTile,GridPosition[]> moveSet, int gridSize, Random rand)
		{
			List<BlueprintRoom> rooms = Lists.newArrayList();
			
			// Sort by descendant count so the nodes with the most descendants get assigned the clearest positions
			rooms.addAll(chart.byDepth(depth));
			rooms.sort(descendantSort(chart));
			
			for(BlueprintRoom node : rooms)
				if(gridMap.isEmpty())
				{
					node.setPosition(0, 0);
					gridMap.put(node.tilePosition(), node);
				}
				else if(node.hasParents())
				{
					GridTile position = new GridTile(0,0);
					
					// Find unoccupied position adjacent to parent(s)
					List<BlueprintRoom> parents = node.getParents(chart);
					if(!parents.isEmpty())
					{
						List<GridTile> options = getAvailableOptions(parents, node.childrenCount(), moveSet, gridSize, gridMap);
						if(options.isEmpty())
							continue;	// FIXME Resolve contexts where nodes have nowhere to go
						
						position = selectOption(options, parents, chart, rand);
					}
					
					gridMap.put(position, node.setTilePosition(position));
				}
		}
		
		private static GridTile selectOption(List<GridTile> options, List<BlueprintRoom> parents, Blueprint chart, Random rand)
		{
			if(options.size() > 1)
			{
				// Prioritise viable position farthest from parents of parent nodes
				List<GridTile> gPositions = Lists.newArrayList();
				parents.forEach(n -> gPositions.add(n.getParentPosition(chart)));
				if(!gPositions.isEmpty())
				{
					int gX = 0, gY = 0;
					for(GridTile gPos : gPositions)
					{
						gX += gPos.x;
						gY += gPos.y;
					}
					gX /= gPositions.size();
					gY /= gPositions.size();
					
					GridTile gPos = new GridTile(gX, gY);
					double maxDist = -1;
					Map<GridTile, Double> distMap = new HashMap<>();
					for(GridTile option : options)
					{
						double dist = option.distance(gPos);
						distMap.put(option, dist);
						if(dist > maxDist)
							maxDist = dist;
					}
					
					final double optimalDist = maxDist;
					List<Pair<GridTile,Float>> weights = Lists.newArrayList();
					distMap.entrySet().forEach(e -> weights.add(Pair.of(e.getKey(), (float)(e.getValue() / optimalDist))));
					
					return CDUtils.selectFromWeightedList(weights, rand.nextFloat());
				}
				else
					return options.get(rand.nextInt(options.size()));
			}
			else
				return options.get(0);
		}
		
		public static List<GridTile> getAvailableOptions(List<BlueprintRoom> parents, int childTally, Function<GridTile, GridPosition[]> moveSet, int gridSize, Map<GridTile,BlueprintRoom> gridMap)
		{
			List<GridTile> options = Lists.newArrayList();
			for(BlueprintRoom parent : parents)
				getAvailableOptions(parent.tilePosition(), childTally, moveSet, gridSize, gridMap).stream().filter(p -> !options.contains(p)).forEach(options::add);
			
			// Copy of the blueprint presently represented by the gridMap's values
			Blueprint posit = new Blueprint();
			gridMap.values().stream().map(BlueprintRoom::clone).forEach(posit::add);
			
			// Hypothetical room we are trying to place, with the same parent nodes
			BlueprintRoom concept = BlueprintRoom.create();
			parents.stream().forEach(p -> posit.stream().filter(pH -> pH.uuid().equals(p.uuid())).forEach(pH -> pH.addChild(concept)));
			posit.add(concept);
			
			options.removeIf(pos -> 
			{
				concept.setTilePosition(pos);
				return Blueprint.hasErrors(posit);
			});
			
			return options;
		}
		
		public static List<GridTile> getAvailableOptions(GridTile position, int minExits, Function<GridTile,GridPosition[]> moveSet, int gridSize, Map<GridTile,BlueprintRoom> gridMap)
		{
			List<GridTile> options = Lists.newArrayList();
			for(GridPosition offset : moveSet.apply(position))
			{
				GridTile neighbour = offset.get(position, gridMap, gridSize);
				if(!gridMap.containsKey(neighbour) && neighbour.y >= 0)
				{
					// Ensure the position has at least as many moves itself as the node has children
					if(minExits > 0 && getAvailableOptions(neighbour, -1, moveSet, gridSize, gridMap).size() <= minExits)
						continue;
					
					options.add(neighbour);
				}
			}
			return options;
		}
		
		protected static class ScaledGridPosition implements GridPosition
		{
			private final double scale;
			private final int x, y;
			
			protected ScaledGridPosition(int xIn, int yIn, double scaleIn)
			{
				scale = scaleIn;
				x = xIn;
				y = yIn;
			}
			
			public static GridPosition of(double val, int x, int y) { return new ScaledGridPosition(x, y, val); }
			
			public GridTile get(GridTile position, Map<GridTile,BlueprintRoom> occupancies, int gridSize)
			{
				int size = (int)((double)gridSize * scale);
				return position.add(x * size, y * size);
			}
		}
		
		public static class Square extends Grid
		{
			public static final GridPosition[] QUAD_GRID = new GridPosition[]
					{
						(p,o,g) -> p.add(g, 0),
						(p,o,g) -> p.add(-g, 0),
						(p,o,g) -> p.add(0, g),
						(p,o,g) -> p.add(0, -g)
					};
			
			public static Square create() { return new Square(); }
			
			protected GridPosition[] moveSet(GridTile position) { return QUAD_GRID; }
		}
		
		public static class Octagonal extends Grid
		{
			public static final GridPosition[] OCT_GRID_A = new GridPosition[]
					{
						(p,o,g) -> p.add(g, 0),
						(p,o,g) -> p.add(-g, 0),
						(p,o,g) -> p.add(0, g),
						(p,o,g) -> p.add(0, -g),
						
						ScaledGridPosition.of(0.5D, 1, 1),
						ScaledGridPosition.of(0.5D, 1, -1),
						ScaledGridPosition.of(0.5D, -1, 1),
						ScaledGridPosition.of(0.5D, -1, -1)
					};
			public static final GridPosition[] OCT_GRID_B = new GridPosition[]
					{
						ScaledGridPosition.of(0.5D, 1, 1),
						ScaledGridPosition.of(0.5D, 1, -1),
						ScaledGridPosition.of(0.5D, -1, 1),
						ScaledGridPosition.of(0.5D, -1, -1)
					};
			
			public static Octagonal create() { return new Octagonal(); }
			
			protected GridPosition[] moveSet(GridTile position)
			{
				return position.y%1 == 0 ? OCT_GRID_A : OCT_GRID_B;
			}
		}
	}
}

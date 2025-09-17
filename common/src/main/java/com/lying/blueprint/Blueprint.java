package com.lying.blueprint;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector2i;

import com.google.common.base.Predicates;
import com.google.common.collect.Lists;
import com.lying.grammar.GrammarPhrase;
import com.lying.grammar.GrammarRoom;
import com.lying.init.CDTerms;
import com.lying.utility.Box2;
import com.lying.utility.Line2;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

@SuppressWarnings("serial")
public class Blueprint extends ArrayList<BlueprintRoom>
{
	protected int maxDepth = 0;
	protected Map<Integer, List<BlueprintRoom>> byDepth = new HashMap<>();
	private List<BlueprintRoom> goldenPath = Lists.newArrayList();
	
	public static Blueprint fromGraph(GrammarPhrase graphIn)
	{
		Blueprint graph = new Blueprint();
		graphIn.getStart().ifPresent(r -> addNodeToBlueprint(r, null, graph, graphIn));
		return graph;
	}
	
	private static BlueprintRoom addNodeToBlueprint(GrammarRoom room, @Nullable BlueprintRoom parent, Blueprint graph, GrammarPhrase graphIn)
	{
		BlueprintRoom node = new BlueprintRoom(room.uuid(), room.metadata(), room.getChildLinks(), room.getParentLinks());
		graph.add(node);
		if(room.hasLinks())
			room.getChildRooms(graphIn).forEach(r -> addNodeToBlueprint(r, parent, graph, graphIn));
		return node;
	}
	
	public Optional<BlueprintRoom> start() { return stream().filter(n -> n.metadata().is(CDTerms.START.get())).findFirst(); }
	
	public Optional<BlueprintRoom> end() { return stream().filter(n -> n.metadata().is(CDTerms.END.get())).findFirst(); }
	
	public boolean add(BlueprintRoom node)
	{
		boolean result = super.add(node);
		if(result)
		{
			// Update the depth range
			maxDepth = 0;
			for(BlueprintRoom n : this)
				if(n.metadata().depth() > maxDepth)
					maxDepth = n.metadata().depth();
			
			// Update the depth map
			byDepth.clear();
			for(int i=0; i<=maxDepth; i++)
			{
				final int depth = i;
				byDepth.put(i, stream().filter(n -> n.metadata().depth() == depth).toList());
			}
		}
		return result;
	}
	
	/** Returns the deepest level of this dungeon */
	public int maxDepth() { return maxDepth; }
	
	public void updateGoldenPath()
	{
		goldenPath = BlueprintPather.calculateGoldenPath(this);
	}
	
	/** Returns a list of nodes representing the path from the start to the end of this dungeon */
	public List<BlueprintRoom> getGoldenPath() { return this.goldenPath; }
	
	@NotNull
	public List<BlueprintRoom> byDepth(int depth) { return byDepth.getOrDefault(depth, Lists.newArrayList()); }
	
	/** Returns true if this blueprint contains any errors that may interfere with generation */
	public boolean hasErrors()
	{
		// Check if any bounding boxes intersect
		List<Box2> bounds = stream().map(BlueprintRoom::bounds).toList();
		for(Box2 boundA : bounds)
			if(bounds.stream().filter(b -> !b.equals(boundA)).anyMatch(b -> boundA.intersects(b)))
				return true;
		
		List<Line2> paths = getAllPaths(this);
		for(Line2 path : paths)
		{
			// Check if any paths intersect unrelated nodes
			if(stream().filter(n -> !(n.position().equals(path.getLeft()) || n.position().equals(path.getRight()))).map(BlueprintRoom::bounds).anyMatch(b -> b.intersects(path)))
				return true;
			
			// Check if any paths intersect other paths
			if(paths.stream().filter(p -> !p.equals(path)).anyMatch(p -> p.intersects(path)))
				return true;
		}
		
		return false;
	}
	
	public boolean build(BlockPos position, ServerWorld world)
	{
		if(isEmpty() || hasErrors())
			return false;
		
		forEach(node -> 
		{
			Vector2i nodePos = node.position();
			BlockPos pos = position.add(nodePos.x, 0, nodePos.y);
			Vector2i scale = node.metadata().size();
			
			BlockPos min = pos.add(-scale.x / 2, 0, -scale.y / 2);
			BlockPos max = min.add(scale.x, 0, scale.y);
			node.metadata().type().generate(min, max, world, node, this);
		});
		
		buildExteriorPaths(position, world);
		return true;
	}
	
	public void buildExteriorPaths(BlockPos position, ServerWorld world)
	{
		// FIXME Restrict to only constructing outside room boundaries
		getAllPaths(this).forEach(path ->
		{
			Vector2i parentPos = path.getLeft();
			Vector2i childPos = path.getRight();
			BlockPos pos1 = position.add(parentPos.x, 0, parentPos.y);
			BlockPos pos2 = position.add(childPos.x, 0, childPos.y);
			
			BlockPos current = pos1;
			while(current.getSquaredDistance(pos2) > 0)
			{
				double minDist = Double.MAX_VALUE;
				Direction face = Direction.NORTH;
				for(Direction facing : Direction.Type.HORIZONTAL)
				{
					double dist = current.offset(facing).getSquaredDistance(pos2);
					if(minDist > dist)
					{
						face = facing;
						minDist = dist;
					}
				}
				
				tryPlaceAt(Blocks.SMOOTH_STONE.getDefaultState(), current, world);
				for(int i=2; i>0; i--)
					tryPlaceAt(Blocks.AIR.getDefaultState(), current.up(i), world);
				current = current.offset(face);
			}
		});
	}
	
	public static void tryPlaceAt(BlockState state, BlockPos pos, ServerWorld world)
	{
		BlockState stateAt = world.getBlockState(pos);
		if(!stateAt.isIn(BlockTags.WITHER_IMMUNE))
			world.setBlockState(pos, state);
	}
	
	/** Returns a list of all paths between the given nodes */
	public static List<Line2> getAllPaths(Collection<BlueprintRoom> chart)
	{
		List<Line2> existingPaths = Lists.newArrayList();
		for(BlueprintRoom node : chart)
			node.getChildren(chart).forEach(c -> existingPaths.add(new Line2(node.position(), c.position())));
		return existingPaths;
	}
	
	/** Returns a list of all paths in the given blueprint, excluding any that connect to the given node */
	public static List<Line2> getPathsExcluding(Collection<BlueprintRoom> chart, @Nullable List<BlueprintRoom> excludeList)
	{
		Predicate<BlueprintRoom> exclusion = 
				(excludeList.isEmpty() || excludeList == null) ? 
					Predicates.alwaysFalse() : 
					n -> excludeList.stream().map(BlueprintRoom::uuid).anyMatch(id -> id.equals(n.uuid()));
		List<Line2> paths = Lists.newArrayList();
		for(BlueprintRoom node : chart)
			node.getChildren(chart).forEach(child -> 
			{
				if(exclusion.test(node) || exclusion.test(child))
					return;
				
				paths.add(new Line2(node.position(), child.position()));
			});
		return paths;
	}
	
	public static List<Box2> getAllBounds(Collection<BlueprintRoom> chart)
	{
		List<Box2> existingPaths = Lists.newArrayList();
		for(BlueprintRoom node : chart)
			existingPaths.add(node.bounds());
		return existingPaths;
	}
	
	public static List<Box2> getBoundsExcluding(Collection<BlueprintRoom> chart, @Nullable List<BlueprintRoom> excludeList)
	{
		Predicate<BlueprintRoom> exclusion = 
				(excludeList.isEmpty() || excludeList == null) ? 
					Predicates.alwaysFalse() : 
					n -> excludeList.stream().map(BlueprintRoom::uuid).anyMatch(id -> id.equals(n.uuid()));
		List<Box2> bounds = Lists.newArrayList();
		for(BlueprintRoom node : chart)
			node.getChildren(chart).forEach(child -> 
			{
				if(!exclusion.test(child))
					bounds.add(child.bounds());
			});
		return bounds;
	}
	
	
	public static boolean intersectsAnyBoundingBox(Box2 box, Collection<BlueprintRoom> chart)
	{
		return chart.stream().anyMatch(b -> b.intersects(box));
	}
	
	public static boolean intersectsAnyPath(Box2 box, Collection<BlueprintRoom> chart)
	{
		return getAllPaths(chart).stream()
				.anyMatch(p -> box.intersects(p));
	}
	
	public static boolean boxHasIntersection(Box2 box, Collection<BlueprintRoom> chart)
	{
		return intersectsAnyBoundingBox(box, chart) || intersectsAnyPath(box, chart);
	}
	
	public static boolean pathHasIntersection(Line2 line, Collection<BlueprintRoom> chart)
	{
		// Intersection with node bounding boxes
		if(chart.stream()
				.filter(n -> !(n.position().equals(line.getLeft()) || n.position().equals(line.getRight())))
				.anyMatch(n -> n.bounds().intersects(line)))
			return false;
		
		// Intersection with inter-node paths
		return getAllPaths(chart).stream()
				.filter(p -> !line.equals(p))
				.anyMatch(p -> p.intersects(line));
	}
}

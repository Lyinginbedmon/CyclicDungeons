package com.lying.blueprint;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector2i;

import com.google.common.collect.Lists;
import com.lying.grammar.GrammarPhrase;
import com.lying.grammar.GrammarRoom;
import com.lying.init.CDTerms;
import com.lying.utility.Box2;

import net.minecraft.block.BlockState;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

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
	public boolean hasErrors() { return hasErrors(this); }
	
	/** Returns true if the set of rooms contains any errors that may interfere with generation */
	public static boolean hasErrors(List<BlueprintRoom> chart)
	{
		for(ErrorType type : ErrorType.values())
			if(tallyErrors(chart, type) > 0)
				return true;
		return false;
	}
	
	public static int tallyErrors(List<BlueprintRoom> chart, ErrorType type)
	{
		int tally = 0;
		List<BlueprintPassage> paths = getPassages(chart);
		
		switch(type)
		{
			case COLLISION:
				List<Box2> bounds = chart.stream().map(BlueprintRoom::bounds).toList();
				for(Box2 boundA : bounds)
					if(bounds.stream().filter(b -> !b.equals(boundA)).anyMatch(b -> boundA.intersects(b)))
						++tally;
				return tally;
			case TUNNEL:
				for(BlueprintPassage path : paths)
					if(path.hasTunnels(chart))
						++tally;
				return tally;
			case INTERSECTION:
				for(BlueprintPassage path : paths)
					if(path.hasIntersections(chart))
						++tally;
				return tally;
		}
		
		return 0;
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
			BlockPos max = min.add(scale.x, 1, scale.y);
			node.metadata().type().generate(min, max, world, node, this);
		});
		
		buildExteriorPaths(position, world);
		return true;
	}
	
	public void buildExteriorPaths(BlockPos position, ServerWorld world)
	{
		getPassages(this).forEach(p -> p.build(position, world));
	}
	
	public static void tryPlaceAt(BlockState state, BlockPos pos, ServerWorld world)
	{
		BlockState stateAt = world.getBlockState(pos);
		if(!stateAt.isIn(BlockTags.WITHER_IMMUNE))
			world.setBlockState(pos, state);
	}
	
	/** Returns a list of all paths between the given nodes */
	public static List<BlueprintPassage> getPassages(Collection<BlueprintRoom> chart)
	{
		List<BlueprintPassage> paths = Lists.newArrayList();
		for(BlueprintRoom n : chart)
		{
			final Vector2i point = n.position();
			n.getChildren(chart).stream().map(BlueprintRoom::position).map(c -> new BlueprintPassage(point, c)).forEach(paths::add);
		}
		return paths;
	}
	
	public static List<BlueprintPassage> getPassagesOf(BlueprintRoom room, Collection<BlueprintRoom> chart)
	{
		return getPassages(chart).stream().filter(p -> p.isTerminus(room)).toList();
	}
	
	public static enum ErrorType
	{
		/** Rooms that share space with other rooms */
		COLLISION,
		/** Passages that intersect other passages */
		INTERSECTION,
		/** Passages that pass through unrelated rooms */
		TUNNEL;
	}
}

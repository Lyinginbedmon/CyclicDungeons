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

import com.google.common.collect.Lists;
import com.lying.grammar.GrammarPhrase;
import com.lying.grammar.GrammarRoom;
import com.lying.grammar.GrammarTerm;
import com.lying.grammar.RoomMetadata;
import com.lying.init.CDLoggers;
import com.lying.init.CDTerms;
import com.lying.utility.AbstractBox2f;
import com.lying.utility.DebugLogger;
import com.lying.worldgen.Tile;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

@SuppressWarnings("serial")
public class Blueprint extends ArrayList<BlueprintRoom>
{
	public static final DebugLogger LOGGER = CDLoggers.WORLDGEN;
	
	private static final int ROOM_HEIGHT = Tile.TILE_SIZE * 4;
	protected int maxDepth = 0;
	protected Map<Integer, List<BlueprintRoom>> byDepth = new HashMap<>();
	private List<BlueprintRoom> criticalPath = Lists.newArrayList();
	
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
	
	public void updateCriticalPath()
	{
		criticalPath = BlueprintPather.calculateCriticalPath(this);
	}
	
	/** Returns a list of nodes representing the path from the start to the end of this dungeon */
	public List<BlueprintRoom> getCriticalPath() { return this.criticalPath; }
	
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
				for(BlueprintRoom room : chart)
				{
					AbstractBox2f bounds = room.bounds();
					if(chart.stream().filter(r -> !r.equals(room)).map(b -> b.bounds().grow(1F)).anyMatch(bounds::intersects))
						++tally;
				}
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
		
		long timeMillis = System.currentTimeMillis();
		LOGGER.info(" # Beginning blueprint generation");
		
//		buildExteriorShell(position, world);
		
		// FIXME Finalise paths (trim, merge, etc) before generating
		
		buildExteriorPaths(position, world);
		
		buildRooms(position, world);
		
		LOGGER.info(" # Blueprint generation completed, {}ms total", System.currentTimeMillis() - timeMillis);
		return true;
	}
	
	public void buildExteriorShell(BlockPos position, ServerWorld world)
	{
		long timeMillis = System.currentTimeMillis();
		LOGGER.info(" # Generating exterior shell");
		
		// Collect all bounding boxes as positions
		List<AbstractBox2f> bounds = stream().map(BlueprintRoom::bounds).toList();
		List<BlockPos> interior = Lists.newArrayList();
		bounds.stream().forEach(b -> 
		{
			BlockPos start = position.add((int)b.minX(), 0, (int)b.minY());
			BlockPos end = position.add((int)b.maxX(), ROOM_HEIGHT, (int)b.maxY());
			BlockPos.Mutable.iterate(start, end.add(-1, -1, -1)).forEach(p -> interior.add(p.toImmutable()));
		});
		
		// Expand the bounding boxes 1 block in all directions and collect the new positions
		// Exclude any positions that are in the interior set
		final Predicate<BlockPos> isExterior = p -> !interior.contains(p);
		List<BlockPos> points = Lists.newArrayList();
		bounds.stream().forEach(b -> 
		{
			BlockPos start = position.add((int)b.minX() - 1, -1, (int)b.minY() - 1);
			BlockPos end = position.add((int)b.maxX(), ROOM_HEIGHT, (int)b.maxY());
			BlockPos.Mutable.iterate(start, end).forEach(p -> 
			{
				if(isExterior.test(p))
					points.add(p.toImmutable());
			});
		});
		
		// Generate wall tile at all remaining positions
		final BlockState[] states = new BlockState[] 
				{
					Blocks.DEEPSLATE_BRICKS.getDefaultState(),
					Blocks.CRACKED_DEEPSLATE_BRICKS.getDefaultState(),
					Blocks.DEEPSLATE_TILES.getDefaultState(),
					Blocks.CRACKED_DEEPSLATE_TILES.getDefaultState()
				};
		points.forEach(p -> Tile.tryPlace(states[world.random.nextInt(states.length)], p, world));
		
		LOGGER.info(" ## Exterior shell completed in {}ms", System.currentTimeMillis() - timeMillis);
	}
	
	public void buildRooms(BlockPos position, ServerWorld world)
	{
		long timeMillis = System.currentTimeMillis();
		LOGGER.info(" # Generating rooms");
		
		final List<BlueprintPassage> passages = getPassages(this);
		int tally = 0;
		for(BlueprintRoom node : this)
		{
			Vector2i nodePos = node.position();
			BlockPos pos = position.add(nodePos.x, 0, nodePos.y);
			Vector2i scale = node.metadata().size();
			
			BlockPos min = pos.add(-scale.x / 2, 0, -scale.y / 2);
			BlockPos max = min.add(scale.x, ROOM_HEIGHT, scale.y);
			
			RoomMetadata meta = node.metadata();
			GrammarTerm type = meta.type();
			LOGGER.info(" # Room {} of {}: {}x{} {}", ++tally, size(), meta.size().x(), meta.size().y(), type.registryName().getPath());
			if(type.generate(position, min, max, world, node, this, passages))
				LOGGER.info(" ## Finished");
			else
				LOGGER.error(" ## Error during room generation");
		};
		
		LOGGER.info(" ## Rooms completed in {}ms", System.currentTimeMillis() - timeMillis);
	}
	
	public void buildExteriorPaths(BlockPos position, ServerWorld world)
	{
		long timeMillis = System.currentTimeMillis();
		LOGGER.info(" # Generating exterior passages");
		
		/**
		 * Merge intersecting paths together
		 * Calculate total volume of each path
		 * Subtract any tiles occupied by rooms OR exterior shell
		 * Generate exterior shell of each path
		 * Populate path interior with WFC
		 */
		
//		List<BlueprintPassage> totalPassages = getPassages(this);
//		
//		List<BlueprintPassage> intersecting = Lists.newArrayList();
//		totalPassages.forEach(p -> 
//		{
//			if(totalPassages.stream().filter(Predicates.not(p::equals)).anyMatch(p::canMergeWith))
//				intersecting.add(p);
//		});
//		
//		// List of all paths to actually draw
//		List<PolyPath> pathsToDraw = Lists.newArrayList();
//		
//		// Collect all un-mergeable passages as 2-point poly paths
//		totalPassages.stream().filter(Predicates.not(intersecting::contains)).map(p -> new PolyPath(p.asLine().getLeft(), p.asLine().getRight())).forEach(pathsToDraw::add);
//		
//		// Collect all mergeable passages poly paths
//		while(!intersecting.isEmpty())
//		{
//			BlueprintPassage root = intersecting.removeFirst();
//			List<BlueprintPassage> buddies = intersecting.stream().filter(Predicates.not(root::equals)).filter(root::canMergeWith).toList();
//			
//			PolyPath poly = new PolyPath(root.asLine().getLeft());
//			buddies.stream().map(BlueprintPassage::asLine).map(Line2f::getRight).forEach(poly::addEnd);
//			
//			pathsToDraw.add(poly);
//			intersecting.removeAll(buddies);
//		}
		
		final BlockState[] concretes = new BlockState[]
				{
					Blocks.BLACK_CONCRETE.getDefaultState(),
					Blocks.BLUE_CONCRETE.getDefaultState(),
					Blocks.BROWN_CONCRETE.getDefaultState(),
					Blocks.CYAN_CONCRETE.getDefaultState(),
					Blocks.GRAY_CONCRETE.getDefaultState(),
					Blocks.GREEN_CONCRETE.getDefaultState(),
					Blocks.LIGHT_BLUE_CONCRETE.getDefaultState(),
					Blocks.LIGHT_GRAY_CONCRETE.getDefaultState(),
					Blocks.LIME_CONCRETE.getDefaultState(),
					Blocks.MAGENTA_CONCRETE.getDefaultState(),
					Blocks.ORANGE_CONCRETE.getDefaultState(),
					Blocks.PINK_CONCRETE.getDefaultState(),
					Blocks.PURPLE_CONCRETE.getDefaultState(),
					Blocks.RED_CONCRETE.getDefaultState(),
					Blocks.YELLOW_CONCRETE.getDefaultState(),
					Blocks.WHITE_CONCRETE.getDefaultState()
				};
//		List<AbstractBox2f> bounds = stream().map(BlueprintRoom::bounds).toList();
//		bounds.forEach(box -> 
//		{
//			for(int x=(int)box.minX(); x<box.maxX(); x++)
//				for(int z=(int)box.minY(); z<box.maxY(); z++)
//					tryPlaceAt(Blocks.IRON_BLOCK.getDefaultState(), position.add(x, 0, z), world);
//		});
//		
//		// Draw all assembled paths
//		final Predicate<List<Line2f>> qualifier = line -> line.stream()
//				.anyMatch(segment -> bounds.stream()
//					.filter(box -> !(box.contains(segment.getLeft()) || box.contains(segment.getRight())))
//					.anyMatch(box -> box.intersects(segment)));
//		for(PolyPath path : pathsToDraw)
//		{
//			BlockState marker = concretes[world.random.nextInt(concretes.length)];
//			for(Line2f line : path.asLines(qualifier))
//			{
//				// Skip any line fully contained within a bounding box
//				if(bounds.stream().anyMatch(b -> b.contains(line.getLeft()) && b.contains(line.getRight())))
//					continue;
//				
//				Vec2f start = line.getLeft();
//				Vec2f end = line.getRight();
//				
//				// Clip any line partially intersecting a bounding box to the intersection point
//				for(AbstractBox2f box : bounds)
//				{
//					Optional<Line2f> intersector = box.asEdges().stream().filter(line::intersects).findFirst();
//					if(intersector.isEmpty())
//						continue;
//					
//					if(box.contains(start))
//						start = line.intercept(intersector.get());
//					else if(box.contains(end))
//						end = line.intercept(intersector.get());
//				}
//				
//				Vec2f offset = end.add(start.negate());
//				float len = offset.length();
//				offset = offset.normalize();
//				
//				for(int i=0; i<=len; i++)
//				{
//					Vec2f point = start.add(offset.multiply(i));
//					BlockPos block = position.add((int)point.x, 0, (int)point.y);
//					tryPlaceAt(marker, block, world);
//				}
//			}
//		};
		
		List<AbstractBox2f> bounds = stream().map(BlueprintRoom::bounds).toList();
		getPassages(this).forEach(p -> p.build(position, world, bounds, concretes[world.random.nextInt(concretes.length)]));
		
		LOGGER.info(" ## Passages completed in {}ms", System.currentTimeMillis() - timeMillis);
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
			n.getChildren(chart).stream().map(c -> new BlueprintPassage(n, c)).forEach(paths::add);
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

package com.lying.blueprint;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;

import org.jetbrains.annotations.Nullable;
import org.joml.Vector2i;

import com.google.common.base.Predicates;
import com.google.common.collect.Lists;
import com.lying.grammar.RoomMetadata;
import com.lying.grid.GraphTileGrid;
import com.lying.grid.GridTile;
import com.lying.utility.geometry.AbstractBox2f;
import com.lying.utility.geometry.Box2f;
import com.lying.worldgen.tile.Tile;

import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;

public class BlueprintRoom
{
	private static final int GRID_SIZE = Tile.TILE_SIZE;
	
	private final UUID id;
	private final RoomMetadata metadata;
	private List<UUID> childLinks = Lists.newArrayList();
	private Optional<UUID> parentId = Optional.empty();
	private GridTile tilePosition = GridTile.ZERO.copy();
	private Optional<GridTile> entryTile = Optional.empty();
	
	private Optional<Blueprint> blueprint = Optional.empty();
	
	private Optional<List<GridTile>> tiles = Optional.empty();
	private Optional<GraphTileGrid> tileGrid = Optional.empty();
	
	public BlueprintRoom(UUID idIn, RoomMetadata termIn, List<UUID> childLinksIn, Optional<UUID> parentLinksIn)
	{
		id = idIn;
		metadata = termIn;
		childLinks.addAll(childLinksIn);
		parentId = parentLinksIn;
	}
	
	public static BlueprintRoom create(){ return new BlueprintRoom(UUID.randomUUID(), new RoomMetadata(), List.of(), Optional.empty()); }
	
	public boolean equals(Object obj) { return obj instanceof BlueprintRoom && ((BlueprintRoom)obj).id.equals(id); }
	
	public String toString()
	{
		Vector2i size = metadata.size().div(GRID_SIZE);
		String type = metadata.type().name().getString();
		return type+"["+size.x+" by "+size.y+" at "+tilePosition.shortString()+"]";
	}
	
	public UUID uuid() { return id; }
	
	public BlueprintRoom clone()
	{
		return new BlueprintRoom(id, metadata.clone(), childLinks, parentId).setTilePosition(tilePosition);
	}
	
	public void attachToBlueprint(Blueprint blueprint) { this.blueprint = Optional.of(blueprint); }
	
	public Predicate<GridTile> getExclusionCheck()
	{
		return blueprint.isPresent() ? t -> this.blueprint.get().stream().noneMatch(r -> r.occupiesOrIsAdjacent(t)) : Predicates.alwaysTrue();
	}
	
	public RoomMetadata metadata() { return metadata; }
	
	/** Tile-grid position, in the same scale and relation to the rest of the dungeon */
	public GridTile tilePosition() { return tilePosition; }
	
	/** World-grid position, calculated as the core of the tile closest to the center of the room */
	public Vector2i position()
	{
		Vector2i tileSize = metadata().tileSize();
		GridTile tile = tileMin().add(tileSize.div(2));
		return tile.toVec2i().mul(GRID_SIZE).add(GRID_SIZE / 2, GRID_SIZE / 2);
	}
	
	public BlueprintRoom setPosition(Vector2i vec)
	{
		return setPosition(vec.x, vec.y);
	}
	
	public BlueprintRoom setPosition(int x, int y)
	{
		return setTilePosition(new GridTile(Math.floorDiv(x, GRID_SIZE), Math.floorDiv(y, GRID_SIZE)));
	}
	
	public BlueprintRoom setTilePosition(GridTile tile)
	{
		tilePosition = tile;
		tiles = Optional.empty();
		tileGrid = Optional.empty();
		
		blueprint.ifPresent(Blueprint::clearPassageCache);
		return this;
	}
	
	public BlueprintRoom setEntryTile(@Nullable GridTile tile)
	{
		entryTile = tile == null ? Optional.empty() : Optional.of(tile);
		return this;
	}
	
	/** Returns the doorway tile that this room is entered from */
	@Nullable
	public GridTile getEntryTile() { return entryTile.orElse(null); }
	
	public BlueprintRoom offset(Vector2i vec)
	{
		return offset(vec.x, vec.y);
	}
	
	public BlueprintRoom offset(int x, int y)
	{
		x = (int)Math.signum(x) * GRID_SIZE;
		y = (int)Math.signum(y) * GRID_SIZE;
		return setTilePosition(tilePosition().add(x, y));
	}
	
	public BlueprintRoom nudge(Vector2i vec)
	{
		return nudge(vec.x, vec.y);
	}
	
	public BlueprintRoom nudge(int x, int y)
	{
		return move(MathHelper.clamp(x, -1, 1), MathHelper.clamp(y, -1, 1));
	}
	
	public BlueprintRoom move(Vector2i vec)
	{
		return move(vec.x, vec.y);
	}
	
	public BlueprintRoom move(int x, int y)
	{
		return setTilePosition(tilePosition().add(x, y));
	}
	
	public boolean hasParent() { return parentId.isPresent(); }
	
	public Optional<UUID> parentID() { return this.parentId; }
	
	public GridTile getParentPosition(Blueprint chart)
	{
		GridTile defaultPos = tilePosition().add(0, 1);
		if(!hasParent())
			return defaultPos;
		
		Optional<BlueprintRoom> parentOpt = chart.stream().filter(n -> n.uuid().equals(parentId.get())).findAny();
		if(parentOpt.isPresent())
			return parentOpt.get().tilePosition();
		else
			return defaultPos;
	}
	
	/** Returns the closest tile (inclusive) occupied by this room at the given position */
	public GridTile tileMin()
	{
		return metadata().tileMin(tilePosition());
	}
	
	/** Returns the farthest tile (exclusive) occupied by this room at the given position */
	public GridTile tileMax()
	{
		return metadata().tileMax(tilePosition()).sub(1, 1);
	}
	
	public AbstractBox2f tileBounds()
	{
		return tileBounds(tilePosition());
	}
	
	public AbstractBox2f tileBounds(GridTile position)
	{
		GridTile min = metadata().tileMin(position);
		GridTile max = metadata().tileMax(position);
		return new Box2f(min.x, max.x, min.y, max.y);
	}
	
	public AbstractBox2f worldBounds()
	{
		GridTile min = metadata().tileMin(tilePosition()).mul(GRID_SIZE);
		GridTile max = metadata().tileMax(tilePosition()).mul(GRID_SIZE);
		return new Box2f(min.x, max.x, min.y, max.y);
	}
	
	public Box worldBox()
	{
		GridTile min = metadata().tileMin(tilePosition()).mul(GRID_SIZE);
		GridTile max = metadata().tileMax(tilePosition()).mul(GRID_SIZE);
		return new Box(min.x, 0, min.y, max.x, Blueprint.ROOM_HEIGHT, max.y);
	}
	
	public List<GridTile> tiles()
	{
		if(tiles.isEmpty())
			tiles = Optional.of(metadata().tileFootprint(tilePosition()));
		return tiles.get();
	}
	
	public GraphTileGrid tileGrid()
	{
		if(tileGrid.isEmpty())
			tileGrid = Optional.of((GraphTileGrid)new GraphTileGrid().addAllToVolume(tiles()));
		return tileGrid.get();
	}
	
	/** Returns true if the given tile is occupied by this room */
	public boolean occupies(GridTile tile)
	{
		GridTile min = tileMin(), max = tileMax();
		return tile.x >= min.x && tile.x <= max.x && tile.y >= min.y && tile.y <= max.y;
	}
	
	/**
	 * Returns true if the given tile is adjacent to any tile within this room.<br>
	 * This also functionally includes {@link BlueprintRoom.occupies}, but is slower.
	 */
	public boolean isAdjacent(GridTile tile)
	{
		GridTile max = tileMax().add(1, 1);
		GridTile min = tileMin().sub(1, 1);
		
		// If any component is more than 1 tile from room boundaries, it cannot be adjacent
		if(tile.x > max.x || tile.y > max.y || tile.x < min.x || tile.y < min.y)
			return false;
		
		// If the tile is a corner of the expanded bounds, it cannot be adjacent to any tile within the actual bounds
		return !(
				tile.equals(max) || 
				tile.equals(min) || 
				(tile.isParallel(max) && tile.isParallel(min))	// Any tile parallel to both corners has to be one of the opposing corners
				);
	}
	
	/** Returns true if the given tile is adjacent or equal to any tile in this room */
	public boolean occupiesOrIsAdjacent(GridTile tile)
	{
		return occupies(tile) || isAdjacent(tile);
	}
	
	/** Returns a list of all tiles adjacent to this grid that can be used as doorways */
	public List<GridTile> getDoorwayTiles()
	{
		List<GridTile> doors = Lists.newArrayList();
		
		final GridTile min = tileMin();
		final GridTile max = tileMax();
		
		// Horizontal sides
		for(int x=min.x+1; x<max.x; x++)
		{
			doors.add(new GridTile(x, min.y - 1));
			doors.add(new GridTile(x, max.y + 1));
		}
		
		// Vertical sides
		for(int y=min.y+1; y<max.y; y++)
		{
			doors.add(new GridTile(min.x - 1, y));
			doors.add(new GridTile(max.x + 1, y));
		}
		
		return doors;
	}
	
	public boolean intersects(AbstractBox2f boundsB)
	{
		AbstractBox2f bounds = tileBounds();
		return bounds.intersects(boundsB) || boundsB.intersects(bounds);
	}
	
	public boolean intersects(BlueprintRoom other)
	{
		List<GridTile> myTiles = tiles();
		return other.tiles().stream().anyMatch(p2 -> myTiles.stream().anyMatch(p2::isAdjacentOrSame));
	}
	
	public boolean hasChildren() { return !childLinks.isEmpty(); }
	
	public boolean isChild(BlueprintRoom child) { return childLinks.contains(child.uuid()); }
	
	public int childrenCount() { return childLinks.size(); }
	
	public int descendantCount(Collection<BlueprintRoom> chart)
	{
		int tally = childrenCount();
		for(BlueprintRoom child : getChildren(chart))
			tally += child.descendantCount(chart);
		return tally;
	}
	
	/** Adds a child to this room. Rarely used, since structure is largely determined by graph phase */
	public void addChild(BlueprintRoom child) { childLinks.add(child.id); }
	
	/** Returns a list of all nodes this node is parented to in the given selection */
	public List<BlueprintRoom> getParents(Collection<BlueprintRoom> graph)
	{
		return hasParent() ? graph.stream().filter(n -> n.uuid().equals(parentId.get())).toList() : List.of();
	}
	
	/** Returns a list of all nodes parented to this node in the given selection */
	public List<BlueprintRoom> getChildren(Collection<BlueprintRoom> graph)
	{
		List<BlueprintRoom> set = Lists.newArrayList();
		set.addAll(graph.stream().filter(n -> childLinks.contains(n.id)).toList());
		set.forEach(c -> c.parentId = Optional.of(id));
		return set;
	}
	
	/** Collects all nodes down-stream of the given node */
	public static List<BlueprintRoom> getDescendants(BlueprintRoom node, Blueprint graph)
	{
		List<BlueprintRoom> children = Lists.newArrayList();
		node.getChildren(graph).forEach(child -> 
		{
			if(children.contains(child))
				return;
			
			children.add(child);
			if(child.hasChildren())
				getDescendants(child, graph).forEach(child2 -> 
				{
					if(!children.contains(child2))
						children.add(child2);
				});
		});
		return children;
	}
}
package com.lying.blueprint;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.jetbrains.annotations.Nullable;
import org.joml.Vector2i;

import com.google.common.collect.Lists;
import com.lying.grammar.RoomMetadata;
import com.lying.grid.GraphTileGrid;
import com.lying.grid.GridTile;
import com.lying.utility.AbstractBox2f;
import com.lying.utility.Box2f;
import com.lying.worldgen.tile.Tile;

import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;

public class BlueprintRoom
{
	private static final int GRID_SIZE = Tile.TILE_SIZE;
	
	private final UUID id;
	private final RoomMetadata metadata;
	private List<UUID> childLinks = Lists.newArrayList();
	private List<UUID> parentLinks = Lists.newArrayList();
	private GridTile tilePosition = GridTile.ZERO.copy();
	private Optional<GridTile> entryTile = Optional.empty();
	
	private Optional<Blueprint> blueprint = Optional.empty();
	
	private Optional<List<GridTile>> tiles = Optional.empty();
	private Optional<GraphTileGrid> tileGrid = Optional.empty();
	
	public BlueprintRoom(UUID idIn, RoomMetadata termIn, List<UUID> childLinksIn, List<UUID> parentLinksIn)
	{
		id = idIn;
		metadata = termIn;
		childLinks.addAll(childLinksIn);
		parentLinks.addAll(parentLinksIn);
	}
	
	public static BlueprintRoom create(){ return new BlueprintRoom(UUID.randomUUID(), new RoomMetadata(), List.of(), List.of()); }
	
	public boolean equals(Object obj) { return obj instanceof BlueprintRoom && ((BlueprintRoom)obj).id.equals(id); }
	
	public UUID uuid() { return id; }
	
	public BlueprintRoom clone()
	{
		return new BlueprintRoom(id, metadata.clone(), childLinks, parentLinks).setTilePosition(tilePosition);
	}
	
	public void attachToBlueprint(Blueprint blueprint) { this.blueprint = Optional.of(blueprint); }
	
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
		
		// TODO Check if full passageway recalculation is necessary?
		blueprint.ifPresent(b -> b.clearPassageCache());
		return this;
	}
	
	public BlueprintRoom setEntryTile(@Nullable GridTile tile)
	{
		entryTile = tile == null ? Optional.empty() : Optional.of(tile);
		return this;
	}
	
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
	
	public boolean hasParents() { return !parentLinks.isEmpty(); }
	
	public List<UUID> parentIDs() { return this.parentLinks; }
	
	public GridTile getParentPosition(Blueprint chart)
	{
		GridTile defaultPos = tilePosition().add(0, 1);
		if(!hasParents())
			return defaultPos;
		
		int x = 0, y = 0;
		if(parentLinks.size() > 1)
		{
			for(BlueprintRoom parent : getParents(chart))
			{
				x += parent.position().x;
				y += parent.position().y;
			}
			
			x /= parentLinks.size();
			y /= parentLinks.size();
			return new GridTile(x, y);
		}
		
		Optional<BlueprintRoom> parentOpt = chart.stream().filter(n->n.uuid().equals(parentLinks.get(0))).findAny();
		if(parentOpt.isPresent())
			return parentOpt.get().tilePosition();
		else
			return defaultPos;
	}
	
	public GridTile tileMin()
	{
		return metadata().tileMin(tilePosition());
	}
	
	public GridTile tileMax()
	{
		return metadata().tileMax(tilePosition());
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
	
	/** Returns true if the given tile is adjacent or equal to any tile in this room */
	public boolean occupiesOrIsAdjacent(GridTile tile)
	{
		return occupies(tile) || isAdjacent(tile);
	}
	
	/** Returns true if the given tile is adjacent to any tile within this room */
	public boolean isAdjacent(GridTile tile)
	{
		// If any component is more than 1 tile from room boundaries, it cannot be adjacent
		GridTile max = tileMax().add(1, 1), min = tileMin().sub(1, 1);
		if(tile.x > max.x || tile.y > max.y || tile.x < min.x || tile.y < min.y)
			return false;
		
		// If the tile is within the expanded bounds but is not a corner tile, it must be adjacent
		if(tile.equals(max) || tile.equals(min))
			return false;
		else if(tile.equals(new GridTile(max.x, min.y))|| tile.equals(new GridTile(min.x, max.y)))
			return false;
		else
			return true;
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
		return graph.stream().filter(n -> parentLinks.contains(n.id)).toList();
	}
	
	/** Returns a list of all nodes parented to this node in the given selection */
	public List<BlueprintRoom> getChildren(Collection<BlueprintRoom> graph)
	{
		List<BlueprintRoom> set = Lists.newArrayList();
		set.addAll(graph.stream().filter(n -> childLinks.contains(n.id)).toList());
		return set;
	}
}
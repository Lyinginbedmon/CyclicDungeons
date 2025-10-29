package com.lying.blueprint;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.joml.Vector2i;

import com.google.common.collect.Lists;
import com.lying.grammar.RoomMetadata;
import com.lying.grid.GraphTileGrid;
import com.lying.grid.GridTile;
import com.lying.utility.AbstractBox2f;
import com.lying.utility.Box2f;
import com.lying.worldgen.Tile;

import net.minecraft.util.math.MathHelper;

public class BlueprintRoom
{
	private static final int GRID_SIZE = Tile.TILE_SIZE;
	
	private final UUID id;
	private final RoomMetadata metadata;
	private List<UUID> childLinks = Lists.newArrayList();
	private List<UUID> parentLinks = Lists.newArrayList();
	private GridTile tilePosition = GridTile.ZERO.copy();
	
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
		return this;
	}
	
	public BlueprintRoom offset(Vector2i vec)
	{
		return offset(vec.x, vec.y);
	}
	
	public BlueprintRoom offset(int x, int y)
	{
		x = (int)Math.signum(x) * GRID_SIZE;
		y = (int)Math.signum(y) * GRID_SIZE;
		tilePosition = tilePosition.add(x, y);
		return this;
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
		setTilePosition(tilePosition.add(x, y));
		return this;
	}
	
	public boolean hasParents() { return !parentLinks.isEmpty(); }
	
	public GridTile getParentPosition(Blueprint chart)
	{
		GridTile defaultPos = tilePosition.add(0, 1);
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
	
	public List<GridTile> tiles()
	{
		return metadata().tileFootprint(tilePosition);
	}
	
	public GraphTileGrid tileGrid()
	{
		return (GraphTileGrid)new GraphTileGrid().addAllToVolume(tiles());
	}
	
	/** Returns true if the given tile is occupied by this room */
	public boolean occupies(GridTile tile)
	{
		return tiles().contains(tile);
	}
	
	/** Returns true if the given tile is adjacent or equal to any tile in this room */
	public boolean isAdjacent(GridTile tile)
	{
		return tiles().stream().anyMatch(tile::isAdjacentTo);
	}
	
	public boolean intersects(AbstractBox2f boundsB)
	{
		AbstractBox2f bounds = tileBounds();
		return bounds.intersects(boundsB) || boundsB.intersects(bounds);
	}
	
	public boolean intersects(BlueprintRoom other)
	{
		List<GridTile> myTiles = tiles();
		return other.tiles().stream().anyMatch(p2 -> myTiles.stream().anyMatch(p2::isAdjacentTo));
	}
	
	public boolean hasChildren() { return !childLinks.isEmpty(); }
	
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
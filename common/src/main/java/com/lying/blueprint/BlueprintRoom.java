package com.lying.blueprint;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.joml.Vector2i;

import com.google.common.collect.Lists;
import com.lying.grammar.RoomMetadata;
import com.lying.utility.AbstractBox2f;
import com.lying.utility.Box2f;
import com.lying.utility.GridTile;
import com.lying.worldgen.Tile;

import net.minecraft.util.math.MathHelper;

public class BlueprintRoom
{
	private static final int GRID_SIZE = Tile.TILE_SIZE;
	
	private final UUID id;
	private final RoomMetadata metadata;
	private List<UUID> childLinks = Lists.newArrayList();
	private List<UUID> parentLinks = Lists.newArrayList();
	private GridTile position = new GridTile(0, 0);
	
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
		return new BlueprintRoom(id, metadata, childLinks, parentLinks).setTilePosition(position);
	}
	
	public RoomMetadata metadata() { return metadata; }
	
	public Vector2i position() { return position.mul(GRID_SIZE).toVec2i(); }
	
	public GridTile tilePosition() { return position; }
	
	public BlueprintRoom setPosition(int x, int y)
	{
		x -= x%GRID_SIZE;
		y -= y%GRID_SIZE;
		setTilePosition(new GridTile(Math.floorDiv(x, GRID_SIZE), Math.floorDiv(y, GRID_SIZE)));
		return this;
	}
	
	public BlueprintRoom setPosition(Vector2i vec)
	{
		return setPosition(vec.x, vec.y);
	}
	
	public BlueprintRoom setTilePosition(GridTile tile)
	{
		position = tile;
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
		position = position.add(x, y);
		return this;
	}
	
	public BlueprintRoom nudge(Vector2i vec)
	{
		return nudge(vec.x, vec.y);
	}
	
	public BlueprintRoom nudge(int x, int y)
	{
		x = MathHelper.clamp(x, -1, 1);
		y = MathHelper.clamp(y, -1, 1);
		setTilePosition(position.add(x, y));
		return this;
	}
	
	public boolean hasParents() { return !parentLinks.isEmpty(); }
	
	public GridTile getParentPosition(Blueprint chart)
	{
		GridTile defaultPos = position.add(0, 1);
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
	
	public Vector2i min()
	{
		return min(position());
	}
	
	public Vector2i min(Vector2i position)
	{
		Vector2i size = metadata().size();
		int tX = (Math.floorDiv(size.x, GRID_SIZE) / 2) * GRID_SIZE;
		int tY = (Math.floorDiv(size.y, GRID_SIZE) / 2) * GRID_SIZE;
		return new Vector2i(position.x - tX, position.y - tY);
	}
	
	public Vector2i max()
	{
		return max(position());
	}
	
	public Vector2i max(Vector2i position)
	{
		Vector2i size = metadata().size();
		return min(position).add(size.x, size.y);
	}
	
	public AbstractBox2f bounds()
	{
		return bounds(position());
	}
	
	public AbstractBox2f bounds(Vector2i position)
	{
		Vector2i min = min(position);
		Vector2i max = max(position);
		return new Box2f(min.x, max.x, min.y, max.y);
	}
	
	public List<GridTile> tiles()
	{
		List<GridTile> tiles = Lists.newArrayList();
		
		GridTile pos = tilePosition();
		int mX = pos.x;
		int mY = pos.y;
		
		Vector2i size = metadata().tileSize();
		int tilesX = size.x;
		int tilesY = size.y;
		
		mX -= (tilesX / 2);
		mY -= (tilesY / 2);
		
		for(int x = 0; x<tilesX; x++)
			for(int y = 0; y<tilesY; y++)
			{
				GridTile tile = new GridTile(mX + x, mY + y);
				if(tiles.stream().noneMatch(t -> t.distance(tile) == 0))
					tiles.add(tile);
			}
		return tiles;
	}
	
	public boolean intersects(AbstractBox2f boundsB)
	{
		AbstractBox2f bounds = bounds();
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
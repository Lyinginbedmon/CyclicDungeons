package com.lying.blueprint;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.joml.Vector2i;

import com.google.common.collect.Lists;
import com.lying.grammar.CDMetadata;
import com.lying.utility.Box2;
import com.lying.utility.Vector2iUtils;

public class Node
{
	private final UUID id;
	private final CDMetadata metadata;
	private List<UUID> childLinks = Lists.newArrayList();
	private List<UUID> parentLinks = Lists.newArrayList();
	private Vector2i position = new Vector2i(0,0);
	
	public Node(UUID idIn, CDMetadata termIn, List<UUID> childLinksIn, List<UUID> parentLinksIn)
	{
		id = idIn;
		metadata = termIn;
		childLinks.addAll(childLinksIn);
		parentLinks.addAll(parentLinksIn);
	}
	
	public UUID uuid() { return id; }
	
	public CDMetadata metadata() { return metadata; }
	
	public Vector2i position() { return new Vector2i(position.x, position.y); }
	
	public Node setPosition(int x, int y)
	{
		position = new Vector2i(x, y);
		return this;
	}
	
	public Node offset(Vector2i vec)
	{
		return offset(vec.x, vec.y);
	}
	
	public Node offset(int x, int y)
	{
		position = position.add(x, y);
		return this;
	}
	
	public boolean hasParents() { return !parentLinks.isEmpty(); }
	
	public Vector2i getParentPosition(Blueprint chart)
	{
		Vector2i defaultPos = Vector2iUtils.add(position, new Vector2i(0, 1));
		if(!hasParents())
			return defaultPos;
		
		int x = 0, y = 0;
		if(parentLinks.size() > 1)
		{
			for(Node parent : getParents(chart))
			{
				x += parent.position().x;
				y += parent.position().y;
			}
			
			x /= parentLinks.size();
			y /= parentLinks.size();
			return new Vector2i(x, y);
		}
		
		Optional<Node> parentOpt = chart.stream().filter(n->n.uuid().equals(parentLinks.get(0))).findAny();
		if(parentOpt.isPresent())
			return parentOpt.get().position();
		else
			return defaultPos;
	}
	
	public boolean hasChildren() { return !childLinks.isEmpty(); }
	
	public int childrenCount() { return childLinks.size(); }
	
	/** Returns a list of all nodes this node is parented to in the given selection */
	public List<Node> getParents(Collection<Node> graph)
	{
		return graph.stream().filter(n -> parentLinks.contains(n.id)).toList();
	}
	
	/** Returns a list of all nodes parented to this node in the given selection */
	public List<Node> getChildren(Collection<Node> graph)
	{
		List<Node> set = Lists.newArrayList();
		set.addAll(graph.stream().filter(n -> childLinks.contains(n.id)).toList());
		return set;
	}
	
	public Box2 bounds()
	{
		return bounds(position);
	}
	
	public Box2 bounds(Vector2i position)
	{
		int sizeX = metadata.size().x;
		int sizeY = metadata.size().y;
		int minX = position.x - (sizeX / 2);
		int minY = position.y - (sizeY / 2);
		return new Box2(minX, minX + sizeX, minY, minY + sizeY);
	}
	
	public boolean intersects(Box2 boundsB)
	{
		Box2 bounds = bounds();
		return bounds.intersects(boundsB) || boundsB.intersects(bounds);
	}
}
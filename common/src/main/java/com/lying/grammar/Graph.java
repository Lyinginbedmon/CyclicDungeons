package com.lying.grammar;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.google.common.collect.Lists;
import com.lying.init.CDTerms;

import net.minecraft.text.MutableText;
import net.minecraft.text.Text;

public class Graph
{
	List<Room> rooms = Lists.newArrayList();
	
	public static Graph parsePhrase(String[] phrase)
	{
		Graph graph = new Graph();
		
		Room prev = null;
		for(int i=0; i<phrase.length; i++)
		{
			Optional<Term> term = CDTerms.get(phrase[i]);
			if(term.isEmpty())
				continue;
			
			Room room = new Room().setTerm(term.get());
			if(prev != null)
				prev.linkTo(room.uuid());
			
			graph.add(room);
			prev = room;
		}
		
		return graph;
	}
	
	public Optional<Room> get(UUID idIn)
	{
		return rooms.stream().filter(r -> r.uuid().equals(idIn)).findAny();
	}
	
	public Optional<Room> get(int index)
	{
		return index < rooms.size() ? Optional.of(rooms.get(index)) : Optional.empty();
	}
	
	public List<Room> getLinksTo(UUID uuid)
	{
		List<Room> links = Lists.newArrayList();
		links.addAll(rooms.stream().filter(r -> r.hasLinkTo(uuid)).toList());
		return links;
	}
	
	public int tally(Term term) { return (int)rooms.stream().filter(r -> r.is(term)).count(); }
	
	public boolean isEmpty() { return rooms.isEmpty(); }
	
	public int size() { return rooms.size(); }
	
	public boolean hasBlanks()
	{
		return rooms.stream().anyMatch(Room::isReplaceable);
	}
	
	public List<Room> getBlanks()
	{
		return rooms.stream().filter(Room::isReplaceable).toList();
	}
	
	public int depth()
	{
		int max = Integer.MIN_VALUE;
		for(Room room : rooms)
			if(room.depth > max)
				max = room.depth;
		return max;
	}
	
	public void add(Room roomIn)
	{
		rooms.add(roomIn);
		
		rooms.get(0).depth = 0;
		updateDepth(rooms.get(0));
	}
	
	protected void updateDepth(Room host)
	{
		int depth = host.depth + 1;
		if(host.hasLinks())
			host.getLinksFrom(this).forEach(r -> 
			{
				r.depth = depth;
				updateDepth(r);
			});
	}
	
	public String asString()
	{
		Room room = rooms.get(0);
		String result = room.asString();
		while(room.hasLinks())
		{
			List<Room> links = room.getLinksFrom(this);
			if(links.isEmpty())
				break;
			
			room = links.get(0);
			result = result + " -> " + room.asString();
		}
		return result;
	}
	
	public Text describe()
	{
		Room room = rooms.get(0);
		MutableText result = room.name();
		while(room.hasLinks())
		{
			List<Room> links = room.getLinksFrom(this);
			if(links.isEmpty())
				break;
			
			room = links.get(0);
			result = result.append(" -> ").append(room.name());
		}
		return result;
	}
}
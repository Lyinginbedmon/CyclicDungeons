package com.lying.grammar;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.google.common.collect.Lists;

import net.minecraft.text.MutableText;
import net.minecraft.text.Text;

public class Graph
{
	List<Room> rooms = Lists.newArrayList();
	
	public Optional<Room> get(UUID idIn)
	{
		return rooms.stream().filter(r -> r.uuid().equals(idIn)).findAny();
	}
	
	public Optional<Room> get(int index)
	{
		return index < rooms.size() ? Optional.of(rooms.get(index)) : Optional.empty();
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
		
		Room prev = rooms.get(0);
		while(prev.hasLink())
		{
			/** If the room has no links, we know it's a dead end */
			Optional<Room> linkOpt = get(prev.getLink().get());
			if(linkOpt.isEmpty())
				break;
			
			/** Room being modified */
			Room current = linkOpt.get();
			current.depth = prev.depth + 1;
			
			prev = current;
		}
	}
	
	public Text describe()
	{
		Room room = rooms.get(0);
		MutableText result = room.name();
		while(room.hasLink())
		{
			Optional<Room> linkOpt = get(room.getLink().get());
			if(linkOpt.isEmpty())
				break;
			
			room = linkOpt.get();
			result = result.append(" -> ").append(room.name());
		}
		return result;
	}
}
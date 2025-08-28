package com.lying.grammar;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;

import org.jetbrains.annotations.NotNull;

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
	
	@NotNull
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
	
	public <T extends Object> void printAsTree(Consumer<T> print, Function<Room,T> getter)
	{
		if(isEmpty())
			return;
		
		printRecursive(print, rooms.get(0), getter);
	}
	
	private <T extends Object> void printRecursive(Consumer<T> print, Room room, Function<Room,T> getter)
	{
		print.accept(getter.apply(room));
		Comparator<Room> sorter = Room.branchSort(this);
		room.getLinksFrom(this).stream().sorted(sorter).forEach(r -> printRecursive(print, r, getter));
	}
	
	public String asString()
	{
		if(isEmpty())
			return "NULL";
		
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
		if(isEmpty())
			return Text.literal("NULL");
		
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
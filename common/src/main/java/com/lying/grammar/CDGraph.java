package com.lying.grammar;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.google.common.collect.Lists;
import com.lying.init.CDTerms;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;

import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtOps;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;

/** Rewritable structure composed of {@link CDRoom} objects */
public class CDGraph
{
	public static final Codec<CDGraph> CODEC	= CDRoom.CODEC.listOf().xmap(CDGraph::new, CDGraph::rooms);
	List<CDRoom> rooms = Lists.newArrayList();
	
	public CDGraph() {}
	
	protected CDGraph(List<CDRoom> roomsIn)
	{
		this();
		rooms.addAll(roomsIn);
	}
	
	public static CDGraph parsePhrase(String[] phrase)
	{
		CDGraph graph = new CDGraph();
		
		CDRoom prev = null;
		for(int i=0; i<phrase.length; i++)
		{
			Optional<GrammarTerm> term = CDTerms.get(phrase[i]);
			if(term.isEmpty())
				continue;
			
			CDRoom room = new CDRoom();
			room.metadata().setType(term.get());
			if(prev != null)
				prev.linkTo(room);
			
			graph.add(room);
			prev = room;
		}
		
		return graph;
	}
	
	public CDGraph clone()
	{
		return fromNbt(toNbt());
	}
	
	public final NbtElement toNbt()
	{
		return CODEC.encodeStart(NbtOps.INSTANCE, this).getOrThrow();
	}
	
	@NotNull
	public static CDGraph fromNbt(@Nullable NbtElement element)
	{
		if(element != null)
		{
			DataResult<CDGraph> result = CODEC.parse(NbtOps.INSTANCE, element);
			if(result.isSuccess())
				return result.getOrThrow();
		}
		return new CDGraph();
	}
	
	protected List<CDRoom> rooms() { return this.rooms; }
	
	public Optional<CDRoom> get(UUID idIn)
	{
		return rooms.stream().filter(r -> r.uuid().equals(idIn)).findAny();
	}
	
	public Optional<CDRoom> get(int index)
	{
		return index < rooms.size() ? Optional.of(rooms.get(index)) : Optional.empty();
	}
	
	@NotNull
	public List<CDRoom> getLinksTo(UUID uuid)
	{
		List<CDRoom> links = Lists.newArrayList();
		links.addAll(rooms.stream().filter(r -> r.hasLinkTo(uuid)).toList());
		return links;
	}
	
	public int tally(GrammarTerm term) { return (int)rooms.stream().filter(r -> r.metadata().is(term)).count(); }
	
	public boolean isEmpty() { return rooms.isEmpty(); }
	
	public int size() { return rooms.size(); }
	
	public Optional<CDRoom> getStart() { return isEmpty() ? Optional.empty() : Optional.of(rooms.get(0)); }
	
	public boolean hasBlanks()
	{
		return rooms.stream().map(CDRoom::metadata).anyMatch(CDMetadata::isReplaceable);
	}
	
	public List<CDRoom> getBlanks()
	{
		return rooms.stream().filter(r -> r.metadata().isReplaceable()).toList();
	}
	
	public int depth()
	{
		int max = Integer.MIN_VALUE;
		for(CDRoom room : rooms)
			if(room.metadata().depth() > max)
				max = room.metadata().depth();
		return max;
	}
	
	public void add(CDRoom roomIn)
	{
		rooms.add(roomIn);
		
		getStart().ifPresent(start -> 
		{
			start.metadata().setDepth(0);
			updateDepth(start);
		});
	}
	
	protected void updateDepth(CDRoom host)
	{
		int depth = host.metadata().depth() + 1;
		if(host.hasLinks())
			host.getChildRooms(this).forEach(r -> 
			{
				r.metadata().setDepth(depth);
				updateDepth(r);
			});
	}
	
	public <T extends Object> void printAsTree(Consumer<T> print, Function<CDRoom,T> getter)
	{
		if(isEmpty())
			return;
		
		getStart().ifPresent(start -> printRecursive(print, start, getter));
	}
	
	private <T extends Object> void printRecursive(Consumer<T> print, CDRoom room, Function<CDRoom,T> getter)
	{
		print.accept(getter.apply(room));
		Comparator<CDRoom> sorter = CDRoom.branchSort(this);
		room.getChildRooms(this).stream().sorted(sorter).forEach(r -> printRecursive(print, r, getter));
	}
	
	public String asString()
	{
		if(isEmpty())
			return "NULL";
		
		CDRoom room = getStart().get();
		String result = room.asString();
		while(room.hasLinks())
		{
			List<CDRoom> links = room.getChildRooms(this);
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
		
		CDRoom room = getStart().get();
		MutableText result = room.name();
		while(room.hasLinks())
		{
			List<CDRoom> links = room.getChildRooms(this);
			if(links.isEmpty())
				break;
			
			room = links.get(0);
			result = result.append(" -> ").append(room.name());
		}
		return result;
	}
}
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
import com.lying.init.CDThemes;
import com.lying.init.CDThemes.Theme;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtOps;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;

/** Rewritable structure composed of {@link GrammarRoom} objects */
public class GrammarPhrase
{
	public static final Codec<GrammarPhrase> CODEC	= RecordCodecBuilder.create(instance -> instance.group(
			Theme.CODEC.fieldOf("Theme").forGetter(GrammarPhrase::theme),
			GrammarRoom.CODEC.listOf().fieldOf("Rooms").forGetter(GrammarPhrase::rooms)
			).apply(instance, GrammarPhrase::new));
	private final Theme theme;
	private List<GrammarRoom> rooms = Lists.newArrayList();
	
	public GrammarPhrase(Theme themeIn)
	{
		theme = themeIn;
	}
	
	public GrammarPhrase() { this(CDThemes.GENERIC.get()); }
	
	protected GrammarPhrase(Theme themeIn, List<GrammarRoom> roomsIn)
	{
		this(themeIn);
		rooms.addAll(roomsIn);
	}
	
	public static GrammarPhrase parsePhrase(String[] phrase, Theme theme)
	{
		GrammarPhrase graph = new GrammarPhrase(theme);
		
		GrammarRoom prev = null;
		for(int i=0; i<phrase.length; i++)
		{
			Optional<GrammarTerm> term = CDTerms.get(phrase[i]);
			if(term.isEmpty())
				continue;
			
			GrammarRoom room = new GrammarRoom();
			room.metadata().setType(term.get()).setTheme(theme);
			if(prev != null)
				prev.linkTo(room);
			
			graph.add(room);
			prev = room;
		}
		
		return graph;
	}
	
	public GrammarPhrase clone()
	{
		return fromNbt(toNbt());
	}
	
	public final NbtElement toNbt()
	{
		return CODEC.encodeStart(NbtOps.INSTANCE, this).getOrThrow();
	}
	
	@NotNull
	public static GrammarPhrase fromNbt(@Nullable NbtElement element)
	{
		if(element != null)
		{
			DataResult<GrammarPhrase> result = CODEC.parse(NbtOps.INSTANCE, element);
			if(result.isSuccess())
				return result.getOrThrow();
		}
		return new GrammarPhrase();
	}
	
	protected Theme theme() { return this.theme; }
	
	protected List<GrammarRoom> rooms() { return this.rooms; }
	
	public Optional<GrammarRoom> get(UUID idIn)
	{
		return rooms.stream().filter(r -> r.uuid().equals(idIn)).findAny();
	}
	
	public Optional<GrammarRoom> get(int index)
	{
		return index < rooms.size() ? Optional.of(rooms.get(index)) : Optional.empty();
	}
	
	@NotNull
	public List<GrammarRoom> getLinksTo(UUID uuid)
	{
		List<GrammarRoom> links = Lists.newArrayList();
		links.addAll(rooms.stream().filter(r -> r.hasLinkTo(uuid)).toList());
		return links;
	}
	
	public int tally(GrammarTerm term) { return (int)rooms.stream().filter(r -> r.metadata().is(term)).count(); }
	
	public boolean isEmpty() { return rooms.isEmpty(); }
	
	public int size() { return rooms.size(); }
	
	public Optional<GrammarRoom> getStart() { return isEmpty() ? Optional.empty() : Optional.of(rooms.get(0)); }
	
	public boolean hasBlanks()
	{
		return rooms.stream().map(GrammarRoom::metadata).anyMatch(RoomMetadata::isReplaceable);
	}
	
	public List<GrammarRoom> getBlanks()
	{
		return rooms.stream().filter(r -> r.metadata().isReplaceable()).toList();
	}
	
	public int depth()
	{
		int max = Integer.MIN_VALUE;
		for(GrammarRoom room : rooms)
			if(room.metadata().depth() > max)
				max = room.metadata().depth();
		return max;
	}
	
	public void add(GrammarRoom roomIn)
	{
		rooms.add(roomIn);
		
		getStart().ifPresent(start -> 
		{
			start.metadata().setDepth(0);
			updateDepth(start);
		});
	}
	
	protected void updateDepth(GrammarRoom host)
	{
		int depth = host.metadata().depth() + 1;
		if(host.hasLinks())
			host.getChildRooms(this).forEach(r -> 
			{
				r.metadata().setDepth(depth);
				updateDepth(r);
			});
	}
	
	public <T extends Object> void printAsTree(Consumer<T> print, Function<GrammarRoom,T> getter)
	{
		if(isEmpty())
			return;
		
		getStart().ifPresent(start -> printRecursive(print, start, getter));
	}
	
	private <T extends Object> void printRecursive(Consumer<T> print, GrammarRoom room, Function<GrammarRoom,T> getter)
	{
		print.accept(getter.apply(room));
		Comparator<GrammarRoom> sorter = GrammarRoom.branchSort(this);
		room.getChildRooms(this).stream().sorted(sorter).forEach(r -> printRecursive(print, r, getter));
	}
	
	public String asString()
	{
		if(isEmpty())
			return "NULL";
		
		GrammarRoom room = getStart().get();
		String result = room.asString();
		while(room.hasLinks())
		{
			List<GrammarRoom> links = room.getChildRooms(this);
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
		
		GrammarRoom room = getStart().get();
		MutableText result = room.name();
		while(room.hasLinks())
		{
			List<GrammarRoom> links = room.getChildRooms(this);
			if(links.isEmpty())
				break;
			
			room = links.get(0);
			result = result.append(" -> ").append(room.name());
		}
		return result;
	}
}
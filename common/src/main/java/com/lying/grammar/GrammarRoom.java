package com.lying.grammar;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.jetbrains.annotations.NotNull;

import com.google.common.collect.Lists;
import com.lying.init.CDTerms;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtOps;
import net.minecraft.text.MutableText;
import net.minecraft.util.Identifier;
import net.minecraft.util.Uuids;

/** Graph object holding structural information */
public class GrammarRoom
{
	public static final Codec<GrammarRoom> CODEC	= RecordCodecBuilder.create(instance -> instance.group(
			Uuids.STRING_CODEC.fieldOf("Uuid").forGetter(GrammarRoom::uuid),
			RoomMetadata.CODEC.fieldOf("Metadata").forGetter(GrammarRoom::metadata),
			Uuids.STRING_CODEC.listOf().fieldOf("Children").forGetter(GrammarRoom::getChildLinks),
			Uuids.STRING_CODEC.listOf().fieldOf("Parents").forGetter(GrammarRoom::getParentLinks)
			).apply(instance, (id,meta,doors,entries) -> 
			{
				GrammarRoom room = new GrammarRoom(id);
				room.metadata = meta;
				doors.forEach(child -> room.childLinks.add(child));
				entries.forEach(parent -> room.parentLinks.add(parent));
				return room;
			}));
	
	/** Maximum total number of links both in and out of each room */
	public static final int MAX_LINKS = 4;
	
	private final UUID id;
	private RoomMetadata metadata = new RoomMetadata();
	private List<UUID> childLinks = Lists.newArrayList(), parentLinks = Lists.newArrayList();
	
	public GrammarRoom(UUID idIn)
	{
		id = idIn;
	}
	
	public GrammarRoom()
	{
		this(UUID.randomUUID());
	}
	
	public static Comparator<GrammarRoom> branchSort(GrammarPhrase graph)
	{
		return (a,b) -> 
		{
			int aD = a.tallyDescendants(graph);
			int bD = b.tallyDescendants(graph);
			return aD < bD ? -1 : aD > bD ? 1 : 0;
		};
	}
	
	public final UUID uuid() { return id; }
	
	public final boolean matches(GrammarRoom b) { return b.id.equals(id); }
	
	public final MutableText name() { return metadata.name(); }
	
	public final String asString() { return metadata.asString(); }
	
	public final NbtElement toNbt()
	{
		return CODEC.encodeStart(NbtOps.INSTANCE, this).getOrThrow();
	}
	
	public static final Optional<GrammarRoom> fromNbt(NbtElement nbt)
	{
		DataResult<GrammarRoom> room = CODEC.parse(NbtOps.INSTANCE, nbt);
		if(room.isSuccess())
			return Optional.of(room.getOrThrow());
		else
			return Optional.empty();
	}
	
	public RoomMetadata metadata() { return this.metadata; }
	
	public boolean hasLinks() { return !childLinks.isEmpty(); }
	
	public boolean hasLinkTo(UUID uuid)
	{
		return !childLinks.isEmpty() && childLinks.contains(uuid);
	}
	
	public List<UUID> getChildLinks() { return List.of(childLinks.toArray(new UUID[0])); }
	public List<UUID> getParentLinks() { return List.of(parentLinks.toArray(new UUID[0])); }
	
	public int getTotalLinks() { return childLinks.size() + parentLinks.size(); }
	public boolean canAddLink() { return getTotalLinks() < MAX_LINKS; }
	
	public int tallyDescendants(GrammarPhrase graph)
	{
		int tally = childLinks.size();
		for(UUID offshoot : childLinks)
		{
			Optional<GrammarRoom> r = graph.get(offshoot);
			if(r.isEmpty())
				continue;
			tally += r.get().tallyDescendants(graph);
		}
		return tally;
	}
	
	public GrammarRoom linkTo(GrammarRoom otherRoom)
	{
		childLinks.add(otherRoom.uuid());
		otherRoom.parentLinks.add(id);
		return this;
	}
	
	public GrammarRoom detachFrom(GrammarRoom otherRoom)
	{
		if(hasLinks() && hasLinkTo(otherRoom.uuid()))
		{
			childLinks.remove(otherRoom.uuid());
			otherRoom.parentLinks.remove(id);
		}
		return this;
	}
	
	/** Collects all rooms within the given graph that this room links to */
	@NotNull
	public List<GrammarRoom> getChildRooms(GrammarPhrase graph)
	{
		List<GrammarRoom> links = Lists.newArrayList();
		childLinks.stream()
			.map(graph::get)
			.filter(Optional::isPresent)
			.map(Optional::get)
			.forEach(links::add);
		return links;
	}
	
	/** Collects all rooms within the given graph that link to this room */
	@NotNull
	public List<GrammarRoom> getParentRooms(GrammarPhrase graph)
	{
		List<GrammarRoom> links = Lists.newArrayList();
		parentLinks.stream()
			.map(graph::get)
			.filter(Optional::isPresent)
			.map(Optional::get)
			.forEach(links::add);
		return links;
	}
	
	public GrammarRoom applyTerm(Identifier termIn, GrammarPhrase graph)
	{
		return applyTerm(CDTerms.instance().get(termIn).orElse(CDTerms.instance().blank()), graph);
	}
	public GrammarRoom applyTerm(GrammarTerm termIn, GrammarPhrase graph)
	{
		termIn.applyTo(this, graph);
		return this;
	}
}
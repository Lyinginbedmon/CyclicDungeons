package com.lying.grammar;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.jetbrains.annotations.NotNull;

import com.google.common.collect.Lists;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtOps;
import net.minecraft.text.MutableText;
import net.minecraft.util.Uuids;

/** Graph object holding structural information */
public class CDRoom
{
	public static final Codec<CDRoom> CODEC	= RecordCodecBuilder.create(instance -> instance.group(
			Uuids.STRING_CODEC.fieldOf("Uuid").forGetter(CDRoom::uuid),
			CDMetadata.CODEC.fieldOf("Metadata").forGetter(CDRoom::metadata),
			Uuids.STRING_CODEC.listOf().fieldOf("Children").forGetter(CDRoom::getChildLinks),
			Uuids.STRING_CODEC.listOf().fieldOf("Parents").forGetter(CDRoom::getParentLinks)
			).apply(instance, (id,meta,doors,entries) -> 
			{
				CDRoom room = new CDRoom(id);
				room.metadata = meta;
				doors.forEach(child -> room.childLinks.add(child));
				entries.forEach(parent -> room.parentLinks.add(parent));
				return room;
			}));
	
	/** Maximum total number of links both in and out of each room */
	public static final int MAX_LINKS = 4;
	
	private final UUID id;
	private CDMetadata metadata = new CDMetadata();
	private List<UUID> childLinks = Lists.newArrayList(), parentLinks = Lists.newArrayList();
	
	public CDRoom(UUID idIn)
	{
		id = idIn;
	}
	
	public CDRoom()
	{
		this(UUID.randomUUID());
	}
	
	public static Comparator<CDRoom> branchSort(CDGraph graph)
	{
		return (a,b) -> 
		{
			int aD = a.tallyDescendants(graph);
			int bD = b.tallyDescendants(graph);
			return aD < bD ? -1 : aD > bD ? 1 : 0;
		};
	}
	
	public final UUID uuid() { return id; }
	
	public final boolean matches(CDRoom b) { return b.id.equals(id); }
	
	public final MutableText name() { return metadata.name(); }
	
	public final String asString() { return metadata.asString(); }
	
	public final NbtElement toNbt()
	{
		return CODEC.encodeStart(NbtOps.INSTANCE, this).getOrThrow();
	}
	
	public static final Optional<CDRoom> fromNbt(NbtElement nbt)
	{
		DataResult<CDRoom> room = CODEC.parse(NbtOps.INSTANCE, nbt);
		if(room.isSuccess())
			return Optional.of(room.getOrThrow());
		else
			return Optional.empty();
	}
	
	public CDMetadata metadata() { return this.metadata; }
	
	public boolean hasLinks() { return !childLinks.isEmpty(); }
	
	public boolean hasLinkTo(UUID uuid)
	{
		return !childLinks.isEmpty() && childLinks.contains(uuid);
	}
	
	public List<UUID> getChildLinks() { return List.of(childLinks.toArray(new UUID[0])); }
	public List<UUID> getParentLinks() { return List.of(parentLinks.toArray(new UUID[0])); }
	
	public int getTotalLinks() { return childLinks.size() + parentLinks.size(); }
	public boolean canAddLink() { return getTotalLinks() < MAX_LINKS; }
	
	public int tallyDescendants(CDGraph graph)
	{
		int tally = childLinks.size();
		for(UUID offshoot : childLinks)
		{
			Optional<CDRoom> r = graph.get(offshoot);
			if(r.isEmpty())
				continue;
			tally += r.get().tallyDescendants(graph);
		}
		return tally;
	}
	
	public CDRoom linkTo(CDRoom otherRoom)
	{
		childLinks.add(otherRoom.uuid());
		otherRoom.parentLinks.add(id);
		return this;
	}
	
	public CDRoom detachFrom(CDRoom otherRoom)
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
	public List<CDRoom> getChildRooms(CDGraph graph)
	{
		List<CDRoom> links = Lists.newArrayList();
		childLinks.stream()
			.map(graph::get)
			.filter(Optional::isPresent)
			.map(Optional::get)
			.forEach(links::add);
		return links;
	}
	
	/** Collects all rooms within the given graph that link to this room */
	@NotNull
	public List<CDRoom> getParentRooms(CDGraph graph)
	{
		List<CDRoom> links = Lists.newArrayList();
		parentLinks.stream()
			.map(graph::get)
			.filter(Optional::isPresent)
			.map(Optional::get)
			.forEach(links::add);
		return links;
	}
	
	public CDRoom applyTerm(GrammarTerm termIn, CDGraph graph)
	{
		termIn.applyTo(this, graph);
		return this;
	}
}
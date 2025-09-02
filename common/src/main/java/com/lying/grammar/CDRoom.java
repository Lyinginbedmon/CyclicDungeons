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
			Uuids.STRING_CODEC.listOf().fieldOf("Links").forGetter(CDRoom::getLinkIds)
			).apply(instance, (id,meta,doors) -> 
			{
				CDRoom room = new CDRoom(id);
				room.metadata = meta;
				doors.forEach(room::linkTo);
				return room;
			}));
	
	private final UUID id;
	private CDMetadata metadata = new CDMetadata();
	private List<UUID> linksTo = Lists.newArrayList();
	
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
	
	public boolean hasLinks() { return !linksTo.isEmpty(); }
	
	public boolean hasLinkTo(UUID uuid) { return !linksTo.isEmpty() && linksTo.contains(uuid); }
	
	public int tallyDescendants(CDGraph graph)
	{
		int tally = linksTo.size();
		for(UUID offshoot : linksTo)
		{
			Optional<CDRoom> r = graph.get(offshoot);
			if(r.isEmpty())
				continue;
			tally += r.get().tallyDescendants(graph);
		}
		return tally;
	}
	
	public CDRoom linkTo(UUID otherRoom)
	{
		linksTo.add(otherRoom);
		return this;
	}
	
	public CDRoom detachFrom(UUID otherRoom)
	{
		if(hasLinks() && hasLinkTo(otherRoom))
			linksTo.remove(otherRoom);
		return this;
	}
	
	/** Collects all rooms within the given graph that this room links to */
	@NotNull
	public List<CDRoom> getLinksFrom(CDGraph graph)
	{
		List<CDRoom> links = Lists.newArrayList();
		linksTo.stream()
			.map(graph::get)
			.filter(Optional::isPresent)
			.map(Optional::get)
			.forEach(links::add);
		return links;
	}
	
	public List<UUID> getLinkIds() { return List.of(linksTo.toArray(new UUID[0])); }
	
	public CDRoom applyTerm(GrammarTerm termIn, CDGraph graph)
	{
		termIn.applyTo(this, graph);
		return this;
	}
}
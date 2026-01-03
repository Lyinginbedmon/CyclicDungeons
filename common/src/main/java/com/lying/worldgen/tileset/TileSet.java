package com.lying.worldgen.tileset;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import com.lying.init.CDTiles;
import com.lying.worldgen.tile.DefaultTiles;
import com.lying.worldgen.tile.Tile;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.util.Identifier;

public class TileSet extends HashMap<Identifier,Float>
{
	private static final long serialVersionUID = 1L;
	public static final Codec<TileSet> CODEC	= RecordCodecBuilder.create(instance -> instance.group(
			Identifier.CODEC.fieldOf("id").forGetter(TileSet::registryName),
			TileEntry.CODEC.listOf().fieldOf("values").forGetter(t -> t.entrySet().stream().map(e -> new TileEntry(e.getKey(), e.getValue())).toList())
			).apply(instance, (id,values) -> 
			{
				TileSet tileSet = new TileSet(id);
				values.forEach(e -> tileSet.add(e.tile, e.weight));
				return tileSet;
			}));
	private final Identifier registryName;
	
	public TileSet(Identifier idIn)
	{
		registryName = idIn;
	}
	
	public Identifier registryName() { return registryName; }
	
	public Collection<Tile> keys()
	{
		return keySet().stream().map(CDTiles.instance()::get).filter(Optional::isPresent).map(Optional::get).toList();
	}
	
	public boolean containsKey(Tile tileIn) { return containsKey(tileIn.registryName()); }
	
	public float get(Tile tileIn) { return get(tileIn.registryName()); }
	
	public TileSet add(Tile tileIn, float weightIn)
	{
		return add(tileIn.registryName(), weightIn);
	}
	
	public TileSet add(Identifier tileIn, float weightIn)
	{
		super.put(tileIn, weightIn);
		return this;
	}
	
	public TileSet addAll(Map<Identifier,Float> mapIn)
	{
		mapIn.entrySet().forEach(e -> add(e.getKey(), e.getValue()));
		return this;
	}
	
	/** Adds pristine and regular flooring, things most decor can be placed on */
	public TileSet addPlaceableFlooring()
	{
		add(DefaultTiles.ID_PRISTINE_FLOOR, 3000F);
		add(DefaultTiles.ID_FLOOR, 1000F);
		return this;
	}
	
	/** Adds the flooring tiles used in the default tileset */
	public TileSet addGenericFlooring()
	{
		addPlaceableFlooring();
		add(DefaultTiles.ID_PUDDLE, 1000F);
		add(DefaultTiles.ID_WET_FLOOR, 200F);
		add(DefaultTiles.ID_POOL, 10F);
		return this;
	}
	
	public <T> DataResult<T> encode(final DynamicOps<T> ops)
	{
		return CODEC.encodeStart(ops, this);
	}
	
	public static <T> TileSet decode(final DynamicOps<T> ops, final T input)
	{
		return CODEC.parse(ops, input).getOrThrow();
	}
	
	private static record TileEntry(Identifier tile, float weight)
	{
		public static final Codec<TileSet.TileEntry> CODEC	= RecordCodecBuilder.create(instance -> instance.group(
				Identifier.CODEC.fieldOf("tile").forGetter(TileEntry::tile), 
				Codec.FLOAT.fieldOf("weight").forGetter(TileEntry::weight))
				.apply(instance, TileSet.TileEntry::new));
	}
}
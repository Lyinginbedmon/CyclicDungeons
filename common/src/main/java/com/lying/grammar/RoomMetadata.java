package com.lying.grammar;

import java.util.List;
import java.util.Optional;

import org.joml.Vector2i;

import com.google.common.collect.Lists;
import com.lying.blueprint.processor.IRoomProcessor;
import com.lying.grid.GridTile;
import com.lying.init.CDTerms;
import com.lying.init.CDThemes;
import com.lying.reference.Reference;
import com.lying.utility.Vector2iUtils;
import com.lying.worldgen.theme.Theme;
import com.lying.worldgen.tile.Tile;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtOps;
import net.minecraft.text.MutableText;
import net.minecraft.util.Identifier;

/** Metadata describing non-structural details of a dungeon room */
public class RoomMetadata
{
	public static final int TILE_SIZE = Tile.TILE_SIZE;
	public static final Codec<Vector2i> VEC_CODEC	= RecordCodecBuilder.create(instance -> instance.group(
			Codec.INT.fieldOf("X").forGetter(Vector2i::x),
			Codec.INT.fieldOf("Y").forGetter(Vector2i::y)
			).apply(instance, (x,y) -> new Vector2i(x,y)));
	public static final Codec<RoomMetadata> CODEC	= RecordCodecBuilder.create(instance -> instance.group(
			Codec.INT.fieldOf("Depth").forGetter(RoomMetadata::depth),
			Identifier.CODEC.fieldOf("Theme").forGetter(RoomMetadata::themeId),
			VEC_CODEC.fieldOf("Size").forGetter(RoomMetadata::size),
			GrammarTerm.CODEC.fieldOf("Type").forGetter(RoomMetadata::type),
			Identifier.CODEC.optionalFieldOf("Variant").forGetter(RoomMetadata::processorID),
			NbtCompound.CODEC.fieldOf("VariantData").forGetter(r -> r.processorData)
			).apply(instance, (depth,theme,size,type,variant,nbt) -> 
			{
				RoomMetadata meta = new RoomMetadata().setDepth(depth).setSize(size).setType(type).setThemeId(theme);
				variant.ifPresent(id -> meta.setProcessorID(id));
				meta.processorData = nbt;
				return meta;
			}));
	
	private GrammarTerm type = CDTerms.BLANK.get();
	private Vector2i tileSize = new Vector2i(3, 3);
	private List<GridTile> tileFootprint = Lists.newArrayList();
	private int depth = 0;
	private Identifier themeId = CDThemes.ID_GENERIC;
	private Optional<Identifier> processorID = Optional.empty();
	public NbtCompound processorData = new NbtCompound();
	
	public RoomMetadata()
	{
		setTileSize(3, 3);
	}
	
	public RoomMetadata clone()
	{
		return fromNbt(toNbt());
	}
	
	public NbtElement toNbt()
	{
		return CODEC.encodeStart(NbtOps.INSTANCE, this).getOrThrow();
	}
	
	public static RoomMetadata fromNbt(NbtElement nbt)
	{
		return CODEC.parse(NbtOps.INSTANCE, nbt).getOrThrow();
	}
	
	public static String vec2ToString(Vector2i vec)
	{
		return vec.x + "x" + vec.y;
	}
	
	public final String asString()
	{
		return vec2ToString(tileSize) + " " + type.name().getString()+" ("+depth+")";
	}
	
	public final MutableText name()
	{
		return Reference.ModInfo.translate("debug", "room", vec2ToString(tileSize), type.name(), depth);
	}
	
	public RoomMetadata setThemeId(Identifier themeIn) { themeId = themeIn; return this; }
	public Identifier themeId() { return themeId; }
	public Theme theme() { return CDThemes.instance().get(themeId).orElse(Theme.BLANK); }
	
	public RoomMetadata setDepth(int d) { depth = d; return this; }
	public int depth() { return depth; }
	
	/** An ID value used by {@link IRoomProcessor} when choosing a variation */
	public Optional<Identifier> processorID() { return this.processorID; }
	public void setProcessorID(Identifier idIn)
	{
		this.processorID = idIn == null ? Optional.empty() : Optional.of(idIn);
	}
	
	public RoomMetadata setSize(Vector2i sizeIn)
	{
		return setSize(sizeIn.x, sizeIn.y);
	}
	public RoomMetadata setSize(int x, int y)
	{
		x = Math.ceilDiv(x, TILE_SIZE);
		y = Math.ceilDiv(y, TILE_SIZE);
		return setTileSize(x, y);
	}
	public RoomMetadata setTileSize(int x, int y)
	{
		tileSize = new Vector2i(x, y);
		
		tileFootprint.clear();
		GridTile min = tileMin(GridTile.ZERO);
		GridTile max = tileMax(GridTile.ZERO);
		
		for(int tileX = min.x; tileX<max.x; tileX++)
			for(int tileY = min.y; tileY<max.y; tileY++)
			{
				GridTile tile = new GridTile(tileX, tileY);
				if(!tileFootprint.contains(tile))
					tileFootprint.add(tile);
			}
		
		return this;
	}
	public Vector2i tileSize() { return Vector2iUtils.copy(tileSize); }
	public Vector2i size() { return Vector2iUtils.mul(tileSize, TILE_SIZE); }
	
	public GridTile tileMin(GridTile position)
	{
		Vector2i size = tileSize();
		int tX = Math.floorDiv(size.x, 2);
		int tY = Math.floorDiv(size.y, 2);
		return new GridTile(position.x - tX, position.y - tY);
	}
	
	public GridTile tileMax(GridTile position)
	{
		Vector2i size = tileSize();
		return tileMin(position).add(size.x, size.y);
	}
	
	public List<GridTile> tileFootprint(GridTile tilePosition)
	{
		return tileFootprint.stream().map(t -> t.add(tilePosition)).toList();
	}
	
	public RoomMetadata setType(GrammarTerm term) { type = term; return this; }
	public GrammarTerm type() { return type; }
	
	public boolean is(GrammarTerm term) { return type.matches(term); }
	
	/** Returns true if this room can be replaced during generation */
	public boolean isReplaceable() { return type.isReplaceable(); }
}

package com.lying.grammar;

import org.joml.Vector2i;

import com.lying.init.CDTerms;
import com.lying.reference.Reference;
import com.lying.worldgen.Tile;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtOps;
import net.minecraft.text.MutableText;

/** Metadata describing non-structural details of a dungeon room */
public class RoomMetadata
{
	public static final Codec<Vector2i> VEC_CODEC	= RecordCodecBuilder.create(instance -> instance.group(
			Codec.INT.fieldOf("X").forGetter(Vector2i::x),
			Codec.INT.fieldOf("Y").forGetter(Vector2i::y)
			).apply(instance, (x,y) -> new Vector2i(x,y)));
	public static final Codec<RoomMetadata> CODEC	= RecordCodecBuilder.create(instance -> instance.group(
			Codec.INT.fieldOf("Depth").forGetter(RoomMetadata::depth),
			VEC_CODEC.fieldOf("Size").forGetter(RoomMetadata::size),
			GrammarTerm.CODEC.fieldOf("Type").forGetter(RoomMetadata::type)
			).apply(instance, (d,s,t) -> new RoomMetadata().setDepth(d).setSize(s).setType(t)));
	
	private GrammarTerm type = CDTerms.BLANK.get();
	private Vector2i size = new Vector2i(3, 3);
	private int depth = 0;
	
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
		return vec2ToString(size) + " " + type.name().getString()+" ("+depth+")";
	}
	
	public final MutableText name()
	{
		return Reference.ModInfo.translate("debug", "room", vec2ToString(size), type.name(), depth);
	}
	
	public RoomMetadata setDepth(int d) { depth = d; return this; }
	public int depth() { return depth; }
	
	public RoomMetadata setSize(int x, int y) { return setSize(new Vector2i(x, y)); }
	public RoomMetadata setSize(Vector2i sizeIn)
	{
		int x = Math.ceilDiv(sizeIn.x(), Tile.TILE_SIZE) * Tile.TILE_SIZE;
		int y = Math.ceilDiv(sizeIn.y(), Tile.TILE_SIZE) * Tile.TILE_SIZE;
		size = new Vector2i(x, y);
		return this;
	}
	public Vector2i size() { return size; }
	
	public RoomMetadata setType(GrammarTerm term) { type = term; return this; }
	public GrammarTerm type() { return type; }
	
	public boolean is(GrammarTerm term) { return type.matches(term); }
	
	/** Returns true if this room can be replaced during generation */
	public boolean isReplaceable() { return type.isReplaceable(); }
}

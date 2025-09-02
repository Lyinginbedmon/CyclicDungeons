package com.lying.grammar;

import com.lying.init.CDTerms;
import com.lying.reference.Reference;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtOps;
import net.minecraft.text.MutableText;

/** Metadata describing non-structural details of a dungeon room */
public class CDMetadata
{
	public static final Codec<CDMetadata> CODEC	= RecordCodecBuilder.create(instance -> instance.group(
			Codec.INT.fieldOf("Depth").forGetter(CDMetadata::depth),
			GrammarTerm.CODEC.fieldOf("Type").forGetter(CDMetadata::type)
			).apply(instance, (d,t) -> 
			{
				return new CDMetadata().setDepth(d).setType(t);
			}));
	
	private GrammarTerm type = CDTerms.BLANK.get();
	private int depth = 0;
	
	public NbtElement toNbt()
	{
		return CODEC.encodeStart(NbtOps.INSTANCE, this).getOrThrow();
	}
	
	public static CDMetadata fromNbt(NbtElement nbt)
	{
		return CODEC.parse(NbtOps.INSTANCE, nbt).getOrThrow();
	}
	
	public final String asString()
	{
		return type.name().getString()+" ("+depth+")";
	}
	
	public final MutableText name()
	{
		return Reference.ModInfo.translate("debug", "room", type.name(), depth);
	}
	
	public CDMetadata setDepth(int d) { depth = d; return this; }
	public int depth() { return depth; }
	
	public CDMetadata setType(GrammarTerm term) { type = term; return this; }
	public GrammarTerm type() { return type; }
	
	public boolean is(GrammarTerm term) { return type.matches(term); }
	
	/** Returns true if this room can be replaced during generation */
	public boolean isReplaceable() { return type.isReplaceable(); }
}

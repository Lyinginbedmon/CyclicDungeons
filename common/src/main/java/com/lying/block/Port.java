package com.lying.block;

import java.util.Comparator;

import org.jetbrains.annotations.NotNull;

import com.ibm.icu.text.Collator;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;

public record Port(String name)
{
	public static final Codec<Port> CODEC	= Codec.STRING.comapFlatMap(s -> DataResult.success(new Port(s)), Port::name);
	public static final PacketCodec<ByteBuf, Port> PACKET_CODEC	= PacketCodecs.STRING.xmap(Port::new, Port::name);
	public static final Comparator<Port> SORT	= (a,b) -> Collator.getInstance().compare(a.name.toLowerCase(), b.name.toLowerCase());
	
	public static Port of(@NotNull String name) throws IllegalArgumentException
	{
		if(name == null || name.length() < 1)
			throw new IllegalArgumentException();
		
		return new Port(name.toLowerCase().replace(' ', '_'));
	}
	
	public String toString() { return name; }
	
	public boolean equals(Object obj)
	{
		if(!(obj instanceof Port))
			return false;
		
		Port other = (Port)obj;
		return other.name.equalsIgnoreCase(name);
	}
}
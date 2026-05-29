package com.lying.block;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;

public record Port(String name)
{
	public static final Codec<Port> CODEC	= Codec.STRING.comapFlatMap(s -> DataResult.success(new Port(s)), Port::name);
	public static final PacketCodec<ByteBuf, Port> PACKET_CODEC	= PacketCodecs.STRING.xmap(Port::new, Port::name);
	
	public static Port of(String name) { return new Port(name); }
	
	public String toString() { return name; }
	
	public boolean equals(Object obj) { return obj instanceof Port && ((Port)obj).name.equalsIgnoreCase(name); }
}
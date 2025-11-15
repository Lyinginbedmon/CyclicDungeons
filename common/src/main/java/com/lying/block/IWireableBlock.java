package com.lying.block;

import com.mojang.serialization.Codec;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.util.StringIdentifiable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public interface IWireableBlock
{
	public static IWireableBlock getWireable(BlockPos pos, World world)
	{
		return (IWireableBlock)world.getBlockState(pos).getBlock();
	}
	
	public boolean acceptWireTo(WireRecipient type, BlockPos target, BlockPos pos, World world);
	
	public WireRecipient type();
	
	public default void trigger(BlockPos pos, World world) { }
	
	public default void activate(BlockPos pos, World world) { if(!isActive(pos, world)) trigger(pos, world); }
	public default void deactivate(BlockPos pos, World world) { if(isActive(pos, world)) trigger(pos, world); }
	
	public default int wireCount(BlockPos pos, World world) { return 0; }
	
	public default void clearWires(BlockPos pos, World world) { }
	
	public default boolean isActive(BlockPos pos, World world) { return false; }
	
	public static enum WireRecipient implements StringIdentifiable
	{
		ACTOR,
		SENSOR,
		LOGIC;
		
		public static final Codec<WireRecipient> CODEC = StringIdentifiable.createCodec(WireRecipient::values);
		public static final PacketCodec<ByteBuf, WireRecipient> PACKET_CODEC = PacketCodecs.indexed(id -> values()[id], value -> value.ordinal());
		
		public String asString() { return name().toLowerCase(); }
	}
}

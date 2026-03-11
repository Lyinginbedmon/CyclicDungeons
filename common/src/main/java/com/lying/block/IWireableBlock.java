package com.lying.block;

import org.jetbrains.annotations.Nullable;

import com.lying.item.WiringGunItem.WireMode;
import com.mojang.serialization.Codec;

import io.netty.buffer.ByteBuf;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.util.StringIdentifiable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public interface IWireableBlock
{
	@SuppressWarnings("unchecked")
	@Nullable
	public static <E extends BlockEntity, A extends BlockEntity> BlockEntityTicker<A> validateTicker(BlockEntityType<A> given, BlockEntityType<E> expected, BlockEntityTicker<? super E> ticker)
	{
		return expected == given ? (BlockEntityTicker<A>)ticker : null;
	}
	
	public static IWireableBlock getWireable(BlockPos pos, World world)
	{
		return (IWireableBlock)world.getBlockState(pos).getBlock();
	}
	
	public boolean acceptWireTo(WireRecipient type, BlockPos target, WireMode space, BlockPos pos, World world);
	
	public WireRecipient type();
	
	public default void trigger(BlockPos pos, World world) { }
	
	public default void activate(BlockPos pos, World world) { if(!isActive(pos, world)) trigger(pos, world); }
	public default void deactivate(BlockPos pos, World world) { if(isActive(pos, world)) trigger(pos, world); }
	
	public default int wireCount(BlockPos pos, World world) { return 0; }
	
	public default void clearWires(BlockPos pos, World world) { }
	
	public default Vec3d wireRenderOffset(BlockState state) { return Vec3d.ZERO; }
	
	public default boolean isActive(BlockPos pos, World world) { return activity(pos, world) > 0; }
	
	public default int activity(BlockPos pos, World world) { return 0; }
	
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

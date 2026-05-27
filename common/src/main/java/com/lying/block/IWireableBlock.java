package com.lying.block;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.lying.item.WiringGunItem.WireMode;
import com.mojang.serialization.Codec;

import io.netty.buffer.ByteBuf;
import net.minecraft.block.Block;
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
	
	@Nullable
	public static IWireableBlock getWireable(BlockPos pos, World world)
	{
		if(pos.getY() < world.getBottomY() || pos.getY() > 256)
			return null;
		
		Block block = world.getBlockState(pos).getBlock();
		return block instanceof IWireableBlock ? (IWireableBlock)block : null;
	}
	
	public List<String> inputPorts(BlockPos pos, World world);
	public List<String> outputPorts(BlockPos pos, World world);
	
	public void respondToPorts(BlockPos pos, World world);
	
	/** Called when the block receives a port connection from the given block */
	public boolean acceptWireFrom(String input, BlockPos me, WireMode space, BlockPos pos, String output, World world);
	
	/** Called when the block delivers a port connection to the given block */
	public boolean acceptWireTo(String output, BlockPos me, WireMode space, BlockPos pos, String input, World world);
	
	public WireRecipient type();
	
	public default int wireCount(BlockPos pos, World world) { return 0; }
	
	public default void clearWires(BlockPos pos, World world) { }
	
	public default Vec3d wireRenderOffset(BlockState state) { return Vec3d.ZERO; }
	
	public default boolean isActive(BlockPos pos, World world) { return activity(pos, world) > 0; }
	
	/** Returns true if the given output port of the block is active */
	public default boolean isPortActive(String port, BlockPos pos, World world) { return false; }
	
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

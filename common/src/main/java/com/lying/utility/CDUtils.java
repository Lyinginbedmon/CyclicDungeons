package com.lying.utility;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.IntStream;

import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector2i;

import com.google.common.collect.Lists;
import com.mojang.serialization.Codec;

import io.netty.buffer.ByteBuf;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.HoverEvent.Action;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.DyeColor;
import net.minecraft.util.Util;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec2f;
import net.minecraft.world.World;

public class CDUtils
{
	public static final Codec<Vector2i> VEC2I_CODEC	= Codec.INT_STREAM
			.<Vector2i>comapFlatMap(
					stream -> Util.decodeFixedLengthArray(stream, 2).map(values -> new Vector2i(values[0], values[1])),
					pos -> IntStream.of(new int[]{pos.x(), pos.y()})
				)
				.stable();

	public static final PacketCodec<ByteBuf, Vector2i> VEC2I_PACKET_CODEC = new PacketCodec<ByteBuf, Vector2i>()
	{
		public Vector2i decode(ByteBuf byteBuf)
		{
			return new Vector2i(byteBuf.readInt(), byteBuf.readInt());
		}
		
		public void encode(ByteBuf byteBuf, Vector2i vector)
		{
			byteBuf.writeInt(vector.x());
			byteBuf.writeInt(vector.y());
		}
	};
	
	private static final Map<DyeColor, Pair<Block,Block>> DYE_TO_CONCRETE = new HashMap<>();
	
	static
	{
		DYE_TO_CONCRETE.put(DyeColor.BLACK, Pair.of(Blocks.BLACK_CONCRETE_POWDER, Blocks.BLACK_CONCRETE));
		DYE_TO_CONCRETE.put(DyeColor.BLUE, Pair.of(Blocks.BLUE_CONCRETE_POWDER, Blocks.BLUE_CONCRETE));
		DYE_TO_CONCRETE.put(DyeColor.BROWN, Pair.of(Blocks.BROWN_CONCRETE_POWDER, Blocks.BROWN_CONCRETE));
		DYE_TO_CONCRETE.put(DyeColor.CYAN, Pair.of(Blocks.CYAN_CONCRETE_POWDER, Blocks.CYAN_CONCRETE));
		DYE_TO_CONCRETE.put(DyeColor.GRAY, Pair.of(Blocks.GRAY_CONCRETE_POWDER, Blocks.GRAY_CONCRETE));
		DYE_TO_CONCRETE.put(DyeColor.GREEN, Pair.of(Blocks.GREEN_CONCRETE_POWDER, Blocks.GREEN_CONCRETE));
		DYE_TO_CONCRETE.put(DyeColor.LIGHT_BLUE, Pair.of(Blocks.LIGHT_BLUE_CONCRETE_POWDER, Blocks.LIGHT_BLUE_CONCRETE));
		DYE_TO_CONCRETE.put(DyeColor.LIGHT_GRAY, Pair.of(Blocks.LIGHT_GRAY_CONCRETE_POWDER, Blocks.LIGHT_GRAY_CONCRETE));
		DYE_TO_CONCRETE.put(DyeColor.LIME, Pair.of(Blocks.LIME_CONCRETE_POWDER, Blocks.LIME_CONCRETE));
		DYE_TO_CONCRETE.put(DyeColor.MAGENTA, Pair.of(Blocks.MAGENTA_CONCRETE_POWDER, Blocks.MAGENTA_CONCRETE));
		DYE_TO_CONCRETE.put(DyeColor.ORANGE, Pair.of(Blocks.ORANGE_CONCRETE_POWDER, Blocks.ORANGE_CONCRETE));
		DYE_TO_CONCRETE.put(DyeColor.PINK, Pair.of(Blocks.PINK_CONCRETE_POWDER, Blocks.PINK_CONCRETE));
		DYE_TO_CONCRETE.put(DyeColor.PURPLE, Pair.of(Blocks.PURPLE_CONCRETE_POWDER, Blocks.PURPLE_CONCRETE));
		DYE_TO_CONCRETE.put(DyeColor.RED, Pair.of(Blocks.RED_CONCRETE_POWDER, Blocks.RED_CONCRETE));
		DYE_TO_CONCRETE.put(DyeColor.WHITE, Pair.of(Blocks.WHITE_CONCRETE_POWDER, Blocks.WHITE_CONCRETE));
		DYE_TO_CONCRETE.put(DyeColor.YELLOW, Pair.of(Blocks.YELLOW_CONCRETE_POWDER, Blocks.YELLOW_CONCRETE));
	}
	
	public static Block dyeToConcretePowder(DyeColor color) { return DYE_TO_CONCRETE.get(color).getLeft(); }
	public static Block dyeToConcrete(DyeColor color) { return DYE_TO_CONCRETE.get(color).getRight(); }
	
	@Nullable
	public static <T extends Object> T selectFromWeightedList(List<Pair<T,Float>> weightedList, final float selector)
	{
		if(weightedList.isEmpty())
			return null;
		else if(weightedList.size() == 1)
			return weightedList.get(0).getLeft();
		
		// Step 1 - Calculate the sum of all weights
		float totalWeight = 0F;
		for(float weight : weightedList.stream().map(Pair::getRight).map(Math::abs).filter(w -> w>0).toList())
			totalWeight += weight;
		
		// Step 2 - Use that sum to calculate the percentile of each choice within the set
		List<Pair<T,Float>> percentileList = Lists.newArrayList();
		for(Pair<T,Float> entry : weightedList)
			percentileList.add(Pair.of(entry.getLeft(), entry.getRight() / totalWeight));
		percentileList.sort((a,b) -> a.getRight() < b.getRight() ? -1 : a.getRight() > b.getRight() ? 1 : 0);
		
		// Step 3 - Select the first entry in the list whose weight val exceeds the selector value
		float lowerBound = 0F;
		for(int i=0; i<percentileList.size(); i++)
		{
			Pair<T,Float> entry = percentileList.get(i);
			float upperBound = lowerBound + entry.getRight();
			if(selector >= lowerBound && selector <= upperBound)
				return entry.getLeft();
			lowerBound = upperBound;
		}
		return percentileList.getLast().getLeft();
	}
	
	public static Vec2f rotate(Vec2f a, float radians)
	{
		if(radians == Math.toRadians(90D))
			return new Vec2f(-a.y, a.x);
		
		final double cos = Math.cos(radians), sin = Math.sin(radians);
		return new Vec2f(
				(float)(a.x * cos - a.y * sin),
				(float)(a.x * sin + a.y * cos)
				);
	}
	
	/** Returns a pair containing an optional for:<br> * the input list (if it is present and greater than size 2)<br>* the only value in that list (if it is present and of size 1) */
	public static <T extends Object> Pair<Optional<List<T>>, Optional<T>> listOrSolo(Optional<List<T>> primary)
	{
		List<T> list = primary.orElse(Lists.newArrayList());
		Optional<List<T>> a = list.size() < 2 ? Optional.empty() : primary;
		Optional<T> b = list.size() == 1 ? Optional.of(list.get(0)) : Optional.empty();
		return Pair.of(a, b);
	}
	
	/** Returns an optional of the given list or an empty optional if it is empty */
	public static <T extends Object> Optional<List<T>> orEmpty(List<T> list)
	{
		return list.isEmpty() ? Optional.empty() : Optional.of(list);
	}
	
	public static Optional<BlockPos> getCeilingAbove(BlockPos pos, ServerWorld world) { return getCeilingAbove(pos, world, 10); }
	
	public static Optional<BlockPos> getCeilingAbove(BlockPos pos, ServerWorld world, int maxRange)
	{
		for(int i=1; i<maxRange; i++)
		{
			BlockPos point = pos.offset(Direction.UP, i);
			BlockState state = world.getBlockState(point);
			if(Block.isFaceFullSquare(state.getCollisionShape(world, point), Direction.DOWN))
				return Optional.of(point);
		}
		return Optional.empty();
	}
	
	public static String formatVec2i(Vector2i vec) { return "("+vec.x+", "+vec.y+")"; }
	
	@Nullable
	public static <T extends Object> T objectFromIndex(List<T> list, final int index)
	{
		if(list.isEmpty())
			return null;
		
		int i = index;
		while(i < 0)
			i += list.size();
		
		return list.get(i%list.size());
	}
	
	/** Returns a text with the name of the block at the given position and a hover event showing its coordinates */
	public static MutableText blockWithPos(BlockPos pos, World world)
	{
		return world.getBlockState(pos).getBlock().getName().styled(s -> s.withHoverEvent(new HoverEvent(Action.SHOW_TEXT, Text.literal(pos.toShortString()))));
	}
	
	public static int binaryToDecimal(boolean... bits)
	{
		int result = 0;
		for(int i=0; i<bits.length; i++)
			result += (int)Math.pow(2, i) * (bits[i] ? 1 : 0);
		return result;
	}
	
	public static boolean[] decimalToBinary(int val)
	{
		if(val <= 1)
			return new boolean[] { val == 1 };
		
		if(val > 15)
			val = 15;
		
		boolean[] result = new boolean[4];
		for(int i=result.length; i>0; i--)
			if(val <= 0)
				result[i-1] = false;
			else
			{
				int d = (int)Math.pow(2, i);
				result[i-1] = val >= d;
				val -= d;
			}
		return result;
	}
}

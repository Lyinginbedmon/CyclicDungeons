package com.lying.blueprint;

import java.util.List;

import com.lying.utility.AbstractBox2f;
import com.lying.utility.LineSegment2f;
import com.lying.worldgen.Tile;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec2f;

public class PassageBuilder
{
	public static final BlockState[] CONCRETE = new BlockState[]
			{
				Blocks.BLACK_CONCRETE.getDefaultState(),
				Blocks.BLUE_CONCRETE.getDefaultState(),
				Blocks.BROWN_CONCRETE.getDefaultState(),
				Blocks.CYAN_CONCRETE.getDefaultState(),
				Blocks.GRAY_CONCRETE.getDefaultState(),
				Blocks.GREEN_CONCRETE.getDefaultState(),
				Blocks.LIGHT_BLUE_CONCRETE.getDefaultState(),
				Blocks.LIGHT_GRAY_CONCRETE.getDefaultState(),
				Blocks.LIME_CONCRETE.getDefaultState(),
				Blocks.MAGENTA_CONCRETE.getDefaultState(),
				Blocks.ORANGE_CONCRETE.getDefaultState(),
				Blocks.PINK_CONCRETE.getDefaultState(),
				Blocks.PURPLE_CONCRETE.getDefaultState(),
				Blocks.RED_CONCRETE.getDefaultState(),
				Blocks.YELLOW_CONCRETE.getDefaultState(),
				Blocks.WHITE_CONCRETE.getDefaultState()
			};
	
	public static void build(BlueprintPassage passage, BlockPos origin, ServerWorld world, List<AbstractBox2f> boundaries)
	{
		// Any passage that is shorter than twice the thickness of the exterior walls is functionally internal and doesn't need special generation
		if(passage.length() <= 2D)
			return;
		
		// FIXME Calculate tile set and generate with WFC
		BlockState placeState = CONCRETE[world.random.nextInt(CONCRETE.length)];
		
		for(LineSegment2f segment : passage.asLines())
		{
			Vec2f offset = segment.getRight().add(segment.getLeft().negate());
			float len = offset.length();
			offset = offset.normalize();
			for(int i=0; i<len; i++)
			{
				Vec2f point = segment.getLeft().add(offset.multiply(i));
				BlockPos pos = origin.add((int)point.x, 0, (int)point.y);
				Tile.tryPlace(placeState, pos, world);
				Tile.tryPlace(placeState, pos.up(), world);
			}
		}
	}
}

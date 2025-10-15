package com.lying.blueprint;

import java.util.List;
import java.util.Map;

import com.lying.init.CDTiles;
import com.lying.utility.AbstractBox2f;
import com.lying.utility.LineSegment2f;
import com.lying.worldgen.Tile;
import com.lying.worldgen.TileGenerator;
import com.lying.worldgen.TileSet;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec2f;

public class PassageBuilder
{
	private static final int TILE_SIZE = Tile.TILE_SIZE;
	private static final int PASSAGE_HEIGHT = 3;
	private static final Map<Tile, Float> PASSAGE_TILE_SET = Map.of(
			CDTiles.PASSAGE_FLOOR.get(), 10000F,
			CDTiles.AIR.get(), 10F,
			CDTiles.FLOOR_LIGHT.get(), 1F
			);
	
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
		if(passage.length() <= TILE_SIZE)
			return;
		
		List<LineSegment2f> lines = passage.asLines();
		Vec2f startVec = lines.getFirst().getLeft();
		TileSet map = buildTileSet(passage.asLines(), startVec);
		TileGenerator.generate(map, PASSAGE_TILE_SET, world.random);
		map.finalise();
		map.generate(origin.add(toTileIndex(lines.getFirst().getLeft(), Vec2f.ZERO).multiply(TILE_SIZE)), world);
	}
	
	public static TileSet buildTileSet(List<LineSegment2f> lines, Vec2f mapOrigin)
	{
		TileSet map = new TileSet();
		for(LineSegment2f line : lines)
		{
			// Tile index of line start
			BlockPos startIndex = toTileIndex(line.getLeft(), mapOrigin);
			
			// Tile index of line end
			BlockPos endIndex = toTileIndex(line.getRight(), mapOrigin);
			
			// List of moves available based on line direction
			Vec2f dir = line.direction().normalize();
			List<Direction> moves = Direction.Type.HORIZONTAL.stream().filter(d -> 
			{
				return d.getOffsetX() == Math.signum(dir.x) || d.getOffsetZ() == Math.signum(dir.y);
			}).toList();
			
			// March from start to end
			Direction bestMove = null, normal = null;
			while(startIndex.getSquaredDistance(endIndex) > 0)
			{
				double minDist = Double.MAX_VALUE;
				for(Direction move : moves)
				{
					BlockPos tile = startIndex.offset(move);
					double dist = tile.getSquaredDistance(endIndex);
					if(dist < minDist)
					{
						bestMove = move;
						minDist = dist;
					}
				}
				
				normal = bestMove.rotateYClockwise();
				map.addToVolume(startIndex.offset(normal), startIndex.offset(normal.getOpposite()));
				startIndex = startIndex.offset(bestMove);
			}
			
			normal = bestMove.rotateYClockwise();
			map.addToVolume(startIndex.offset(normal), startIndex.offset(normal.getOpposite()));
		}
		
		map.grow(Direction.UP, PASSAGE_HEIGHT);
		
		return map;
	}
	
	private static BlockPos toTileIndex(Vec2f point, Vec2f origin)
	{
		Vec2f offset = point.add(origin.negate());
		int tX = (int)Math.floor(offset.x / TILE_SIZE);
		int tY = (int)Math.floor(offset.y / TILE_SIZE);
		return new BlockPos(tX, 0, tY);
	}
}

package com.lying.blueprint.processor;

import java.util.List;
import java.util.Optional;

import com.google.common.collect.Lists;
import com.lying.block.IWireableBlock.WireRecipient;
import com.lying.block.entity.TrapLogicBlockEntity;
import com.lying.blueprint.BlueprintRoom;
import com.lying.blueprint.processor.TrapRoomProcessor.TrapEntry;
import com.lying.grammar.RoomMetadata;
import com.lying.grid.BlueprintTileGrid;
import com.lying.init.CDBlockEntityTypes;
import com.lying.init.CDBlocks;
import com.lying.init.CDThemes.Theme;
import com.lying.init.CDTiles;
import com.lying.worldgen.Tile;

import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;

public class TrapRoomProcessor extends RegistryRoomProcessor<TrapEntry>
{
	public void buildRegistry(Theme theme)
	{
		theme.traps().forEach(trap -> register(trap.registryName(), trap));
	}
	
	public static abstract class TrapEntry implements IProcessorEntry
	{
		private final Identifier id;
		
		private TrapEntry(Identifier idIn)
		{
			id = idIn;
		}
		
		public Identifier registryName() { return id; }
	}
	
	public static class PitfallTrapEntry extends TrapEntry
	{
		public PitfallTrapEntry(Identifier name)
		{
			super(name);
		}
		
		public void prepare(BlueprintRoom room, BlueprintTileGrid tileMap, ServerWorld world)
		{
			// Pre-seed tile map with hatches
			
			final Tile hatchTile = CDTiles.HATCH.get();
			
			List<BlockPos> blanks = Lists.newArrayList();
			blanks.addAll(tileMap.getBoundaries(List.of(Direction.DOWN)).stream()
					.filter(pos -> hatchTile.canExistAt(pos, tileMap))
					.filter(pos -> 
					{
						Optional<Tile> tileAt = tileMap.get(pos);
						return tileAt.isPresent() && tileAt.get().isBlank();
						})
					.toList());
			
			if(blanks.isEmpty())
				return;
			
			final int count = (int)((float)blanks.size() * (0.15F + (world.random.nextFloat() * 0.15F)));
			int tally = 0;
			for(int i=0; i<count; i++)
			{
				if(blanks.isEmpty())
					break;
				else
				{
					tileMap.put(blanks.remove(world.random.nextInt(blanks.size())), hatchTile);
					blanks.removeIf(pos -> !hatchTile.canExistAt(pos, tileMap));
					tally++;
				}
			}
			
			room.metadata().processorData.putInt("PitsPlaced", tally);
		}
		
		public void apply(BlockPos min, BlockPos max, ServerWorld world, RoomMetadata meta)
		{
			int count = meta.processorData.getInt("PitsPlaced");
			if(count == 0)
				return;
			
			// Place trap logic block
			BlockPos logicPos = min;	// FIXME Ensure placed under passage tile at entry door
			world.setBlockState(logicPos, CDBlocks.TRAP_LOGIC.get().getDefaultState());
			TrapLogicBlockEntity logic = world.getBlockEntity(logicPos, CDBlockEntityTypes.TRAP_LOGIC.get()).get().setLogic(TrapLogicBlockEntity.ID_1S_FALLOFF);
			
			// Place sensors
			Random rand = world.random;
			for(int i=0; i<count * 2; i++)
				tryPlaceSensor(min, max, world, rand).ifPresent(p -> 
					logic.processWireConnection(p, WireRecipient.SENSOR));
			
			// Wire hatches to logic
			IProcessorEntry.getTileEntities(min, max, world, CDBlockEntityTypes.TRAP_ACTOR.get()).forEach(hatch -> 
				logic.processWireConnection(hatch.getPos(), WireRecipient.ACTOR));
		}
		
		protected Optional<BlockPos> tryPlaceSensor(BlockPos min, BlockPos max, ServerWorld world, Random rand)
		{
			int attempts = 20;
			BlockPos pos;
			do
			{
				pos = new BlockPos(
					rand.nextBetween(min.getX(), max.getX()),
					min.getY() + 2,
					rand.nextBetween(min.getZ(), max.getZ()));
			}
			while(!isValidSensorPos(pos, world) && --attempts > 0);
			
			if(isValidSensorPos(pos, world))
			{
				world.setBlockState(pos, CDBlocks.SENSOR_COLLISION.get().getDefaultState());
				return Optional.of(pos);
			}
			
			return Optional.empty();
		}
		
		protected static boolean isValidSensorPos(BlockPos pos, ServerWorld world)
		{
			return world.isAir(pos) && world.isAir(pos.up());
		}
	}
	
	public static class LavaRiverTrapEntry extends TrapEntry
	{
		public LavaRiverTrapEntry(Identifier name)
		{
			super(name);
		}
		
		public boolean isApplicableTo(BlueprintRoom room, RoomMetadata meta, Theme theme) { return room.hasChildren(); }
		
		public void prepare(BlueprintRoom room, BlueprintTileGrid tileMap, ServerWorld world)
		{
			final Tile lavaTile = CDTiles.LAVA_RIVER.get();
			
			List<BlockPos> blanks = Lists.newArrayList();
			blanks.addAll(tileMap.getBoundaries(List.of(Direction.DOWN)).stream()
					.filter(pos -> lavaTile.canExistAt(pos, tileMap))
					.filter(pos -> 
					{
						Optional<Tile> tileAt = tileMap.get(pos);
						return tileAt.isPresent() && tileAt.get().isBlank();
						})
					.toList());
			
			if(blanks.isEmpty())
				return;
			
			final int count = (int)((float)blanks.size() * 0.70F);
			for(int i=0; i<count; i++)
			{
				if(blanks.isEmpty())
					break;
				else
				{
					tileMap.put(blanks.remove(world.random.nextInt(blanks.size())), lavaTile);
					blanks.removeIf(pos -> !lavaTile.canExistAt(pos, tileMap));
				}
			}
		}
		
		public void apply(BlockPos min, BlockPos max, ServerWorld world, RoomMetadata meta) { }
	}
}

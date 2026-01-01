package com.lying.blueprint.processor.trap;

import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import com.google.common.collect.Lists;
import com.lying.block.HatchBlock;
import com.lying.block.IWireableBlock.WireRecipient;
import com.lying.block.entity.TrapActorBlockEntity;
import com.lying.block.entity.TrapLogicBlockEntity;
import com.lying.blueprint.BlueprintRoom;
import com.lying.blueprint.processor.IProcessorEntry;
import com.lying.blueprint.processor.TrapRoomProcessor.TrapEntry;
import com.lying.grammar.RoomMetadata;
import com.lying.grid.BlueprintTileGrid;
import com.lying.init.CDBlockEntityTypes;
import com.lying.init.CDBlocks;
import com.lying.init.CDLoggers;
import com.lying.init.CDTiles;
import com.lying.init.CDTrapLogicHandlers;
import com.lying.worldgen.Tile;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;

public class PitfallTrapEntry extends TrapEntry
{
	private static final Tile HATCH = CDTiles.HATCH.get();
	
	public PitfallTrapEntry(Identifier name)
	{
		super(name);
	}
	
	public void prepare(BlueprintRoom room, BlueprintTileGrid tileMap, ServerWorld world)
	{
		// Pre-seed tile map with hatches
		List<BlockPos> blanks = Lists.newArrayList();
		blanks.addAll(tileMap.getBoundaries(List.of(Direction.DOWN)).stream()
			.filter(pos -> tileMap.get(pos).get().isBlank())
			.filter(pos -> HATCH.canExistAt(pos, tileMap))
			.toList());
		
		if(blanks.isEmpty())
		{
			CDLoggers.WORLDGEN.warn("Couldn't find anywhere to place hatches in room");
			return;
		}
		
		final int count = (int)((float)blanks.size() * (0.35F + (world.random.nextFloat() * 0.15F)));
		int tally = 0;
		for(int i=0; i<count; i++)
		{
			if(blanks.isEmpty())
				break;
			else
			{
				tileMap.put(blanks.remove(world.random.nextInt(blanks.size())), HATCH);
				blanks.removeIf(pos -> !HATCH.canExistAt(pos, tileMap));
				tally++;
			}
		}
		
		room.metadata().processorData.putInt("PitsPlaced", tally);
	}
	
	public void apply(BlockPos min, BlockPos max, ServerWorld world, RoomMetadata meta)
	{
		int count = meta.processorData.getInt("PitsPlaced");
		if(count == 0)
		{
			CDLoggers.WORLDGEN.warn("No hatches found to wire to");
			return;
		}

		final Random rand = world.random;
		final int floorY = min.getY() + 2;
		
		// Place trap logic block
		BlockPos logicPos = null;
		Iterator<BlockPos> iterator = BlockPos.Mutable.iterate(min, max.withY(min.getY())).iterator();
		while(iterator.hasNext())
		{
			BlockPos pos = iterator.next();
			BlockState state = world.getBlockState(pos);
			if(state.isOpaqueFullCube())
			{
				logicPos = pos;
				break;
			}
		}
		if(logicPos == null)
			logicPos = min;
		
		world.setBlockState(logicPos, CDBlocks.TRAP_LOGIC.get().getDefaultState());
		final TrapLogicBlockEntity logic = world.getBlockEntity(logicPos, CDBlockEntityTypes.TRAP_LOGIC.get()).get().setLogic(CDTrapLogicHandlers.ID_1S_FALLOFF);
		
		// Wire hatches to logic
		List<TrapActorBlockEntity> hatches = IProcessorEntry.getTileEntities(min, max, world, CDBlockEntityTypes.TRAP_ACTOR.get());
		hatches.forEach(hatch -> 
			logic.processWireConnection(hatch.getPos(), WireRecipient.ACTOR));
		
		// Place sensors and wire to logic
		final Function<BlockPos,Double> distFunc = a -> hatches.stream()
				.map(BlockEntity::getPos)
				.map(p -> a.getSquaredDistance(p))
				.min(Comparator.naturalOrder()).get();
		List<BlockPos> points = Lists.newArrayList();
		BlockPos.Mutable.iterate(min.withY(floorY), max.withY(floorY)).forEach(pos -> 
		{
			if(isValidSensorPos(pos, world))
				points.add(pos.toImmutable());
		});
		points.sort((a,b) -> 
		{
			double aDist = distFunc.apply(a);
			double bDist = distFunc.apply(b);
			return aDist < bDist ? -1 : aDist > bDist ? 1 : (rand.nextInt(3) - 1);
		});
		
		for(int i=0; i<count * 2; i++)
			tryPlaceSensor(points.removeFirst(), world).ifPresent(p -> 
				logic.processWireConnection(p, WireRecipient.SENSOR));
	}
	
	protected Optional<BlockPos> tryPlaceSensor(BlockPos pos, ServerWorld world)
	{
		if(isValidSensorPos(pos, world))
		{
			world.setBlockState(pos, CDBlocks.SENSOR_COLLISION.get().getDefaultState());
			return Optional.of(pos);
		}
		
		return Optional.empty();
	}
	
	protected static boolean isValidSensorPos(BlockPos pos, ServerWorld world)
	{
		if(!world.isAir(pos))
			return false;
		if(!world.isAir(pos.up()))
			return false;
		BlockState below = world.getBlockState(pos.down());
		if(below.getBlock() instanceof HatchBlock)
			return false;
		if(!below.isOpaqueFullCube())
			return false;
		return true;
	}
}
package com.lying.grammar.content.trap;

import com.lying.grammar.content.RoomNumberProvider;
import com.lying.reference.Reference;
import com.lying.utility.BlockPredicate;
import com.lying.utility.BlockPredicate.BlockFlags;
import com.lying.utility.BlockPredicate.SubPredicate;

import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

public class BearTrap extends SatelliteStructurePlacerTrap
{
	private static final BlockPredicate PLACEABLE	= BlockPredicate.Builder.create()
			.addFlag(BlockFlags.PLAYER_ACCESSIBLE)
			.child(new SubPredicate(BlockPos.ORIGIN.down(), BlockPredicate.Builder.create().addFlag(BlockFlags.SOLID).build()))
			.build();
	
	// TODO Replace usage with super class using actual structures
	public BearTrap(Identifier idIn)
	{
		super(idIn, 
				new RoomNumberProvider.RandBetween(3, 8, 0),
				PLACEABLE, 
				1, 
				3,
				Reference.ModInfo.prefix("trap/floor_collision"),
				BlockPos.ORIGIN,
				PLACEABLE,
				Reference.ModInfo.prefix("trap/floor_spike"),
				BlockPos.ORIGIN);
	}
	
//	protected void placeTrap(BlockPos pos, ServerWorld world, Random rand)
//	{
//		// Valid positions for spikes
//		List<BlockPos> options = Lists.newArrayList();
//		Direction.Type.HORIZONTAL.stream().map(pos::offset).filter(p -> canPlacePart(p, world)).forEach(options::add);
//		if(options.isEmpty())
//		{
//			CDLoggers.WORLDGEN.warn("No valid positions found to place spike traps around sensor");
//			return;
//		}
//		
//		world.setBlockState(pos, CDBlocks.SENSOR_COLLISION.get().getDefaultState().with(CollisionSensorBlock.FACING, Direction.UP));
//		final BlockState spikeState = CDBlocks.SPIKE_TRAP.get().getDefaultState().with(SpikeTrapBlock.FACING, Direction.UP);
//		
//		// Place spikes
//		List<BlockPos> spikes = Lists.newArrayList();
//		for(int i=rand.nextBetween(1, 2); i>0; i--)
//		{
//			BlockPos spikePos = options.remove(options.size() == 1 ? 0 : rand.nextInt(options.size()));
//			world.setBlockState(spikePos, spikeState);
//			spikes.add(spikePos);
//			
//			if(options.isEmpty())
//				break;
//		}
//		
//		// Wire spikes to sensor
//		for(BlockPos spike : spikes)
//			world.getBlockEntity(spike, CDBlockEntityTypes.SPIKE_TRAP.get()).ifPresent(t -> t.processWireConnection(pos, WireMode.GLOBAL, WireRecipient.SENSOR));
//	}
}

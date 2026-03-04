package com.lying.grammar.content.trap;

import java.util.Optional;

import com.lying.block.FlameJetBlock;
import com.lying.block.IWireableBlock.WireRecipient;
import com.lying.block.entity.FlameJetBlockEntity;
import com.lying.grammar.content.RoomNumberProvider;
import com.lying.init.CDBlockEntityTypes;
import com.lying.init.CDBlocks;
import com.lying.item.WiringGunItem.WireMode;
import com.lying.reference.Reference;
import com.lying.utility.BlockPredicate;
import com.lying.utility.BlockPredicate.BlockFlags;
import com.lying.utility.BlockPredicate.SubPredicate;

import net.minecraft.block.Blocks;
import net.minecraft.block.ChestBlock;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;

public class GreedTrap extends AbstractPlacerTrap
{
	public static final Identifier ID	= Reference.ModInfo.prefix("greed");
	
	public GreedTrap(Identifier idIn)
	{
		super(idIn, new RoomNumberProvider.Absolute(1), BlockPredicate.Builder.create()
			.child(new SubPredicate(BlockPos.ORIGIN, BlockPredicate.Builder.create().invert().addFlag(BlockFlags.AIR).build()))
			.child(new SubPredicate(BlockPos.ORIGIN.up(), BlockPredicate.Builder.create().invert().addFlag(BlockFlags.SOLID).build()))
			.build(), 1, 3);
	}
	
	protected boolean isPosViableForTrap(BlockPos pos, ServerWorld world)
	{
		if(!super.isPosViableForTrap(pos, world))
			return false;
		
		// Check 2: Could a player stand next to this space
		// Check 3: Is the ceiling above an adjacent space solid
		return Direction.Type.HORIZONTAL.stream().map(pos::offset).anyMatch(p -> BlockFlags.PLAYER_ACCESSIBLE.test(world, pos, world.getBlockState(p)));
	}
	
	protected void placeTrap(BlockPos pos, ServerWorld world, Random rand)
	{
		world.setBlockState(pos, Blocks.TRAPPED_CHEST.getDefaultState().with(ChestBlock.FACING, Direction.fromHorizontalQuarterTurns(rand.nextInt(4))));
		final BlockPos sensorPos = pos.down();
		world.setBlockState(sensorPos, CDBlocks.SENSOR_REDSTONE.get().getDefaultState());
		
		Direction.Type.HORIZONTAL.stream()
			.map(pos::offset)
			.filter(p -> BlockFlags.PLAYER_ACCESSIBLE.test(world, p, world.getBlockState(p)))
			.map(p -> tryPlaceJet(p, world))
			.filter(Optional::isPresent)
			.map(Optional::get)
			.forEach(p -> p.processWireConnection(sensorPos, WireMode.GLOBAL, WireRecipient.SENSOR));
	}
	
	protected Optional<FlameJetBlockEntity> tryPlaceJet(BlockPos pos, ServerWorld world)
	{
		Optional<BlockPos> ceilingOpt = getCeilingAbove(pos, world);
		if(ceilingOpt.isEmpty())
			return Optional.empty();
		
		BlockPos ceilingPos = ceilingOpt.get();
		BlockPos jetPos = ceilingPos.down();
		world.setBlockState(jetPos, CDBlocks.FLAME_JET.get().getDefaultState().with(FlameJetBlock.FACING, Direction.DOWN));
		
		Optional<FlameJetBlockEntity> jet = world.getBlockEntity(jetPos, CDBlockEntityTypes.FLAME_JET.get());
		jet.ifPresent(j -> j.setRange(jetPos.getY() - pos.getY()));
		return jet;
	}
}

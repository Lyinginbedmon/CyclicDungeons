package com.lying.grammar.content.trap;

import java.util.Optional;

import org.joml.Vector2i;

import com.lying.block.FlameJetBlock;
import com.lying.block.IWireableBlock.WireRecipient;
import com.lying.block.entity.FlameJetBlockEntity;
import com.lying.init.CDBlockEntityTypes;
import com.lying.init.CDBlocks;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ChestBlock;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;

public class GreedTrapEntry extends AbstractPlacerTrapEntry
{
	public GreedTrapEntry(Identifier idIn)
	{
		super(idIn);
	}
	
	protected int getTrapCountForRoom(Random rand, Vector2i roomSize) { return 1; }
	
	protected boolean isPosViableForTrap(BlockPos pos, ServerWorld world)
	{
		// Check 1: Could a chest here be opened
		if(!world.isAir(pos) || ChestBlock.isChestBlocked(world, pos))
			return false;
		
		// Check 2: Could a player stand next to this space
		// Check 3: Is the ceiling above an adjacent space solid
		return Direction.Type.HORIZONTAL.stream().map(pos::offset).anyMatch(p -> isPlayerAccessible(p, world));
	}
	
	protected static boolean isPlayerAccessible(BlockPos pos, ServerWorld world)
	{
		if(!world.isAir(pos))
			return false;
		else if(!world.isAir(pos.up()))
			return false;
		
		Optional<BlockPos> trace = getCeilingAbove(pos, world);
		if(trace.isEmpty())
			return false;
		
		BlockPos hit = trace.get();
		BlockState hitState = world.getBlockState(hit);
		return Block.isFaceFullSquare(hitState.getCollisionShape(world, hit), Direction.DOWN);
	}
	
	protected void placeTrap(BlockPos pos, ServerWorld world, Random rand)
	{
		world.setBlockState(pos, Blocks.TRAPPED_CHEST.getDefaultState().with(ChestBlock.FACING, Direction.fromHorizontalQuarterTurns(rand.nextInt(4))));
		final BlockPos sensorPos = pos.down();
		world.setBlockState(sensorPos, CDBlocks.SENSOR_REDSTONE.get().getDefaultState());
		
		Direction.Type.HORIZONTAL.stream()
			.map(pos::offset)
			.filter(p -> isPlayerAccessible(p, world))
			.map(p -> tryPlaceJet(p, world))
			.filter(Optional::isPresent)
			.map(Optional::get)
			.forEach(p -> p.processWireConnection(sensorPos, WireRecipient.SENSOR));
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
}

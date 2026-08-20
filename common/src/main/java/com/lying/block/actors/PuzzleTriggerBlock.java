package com.lying.block.actors;

import com.lying.block.Port;
import com.lying.block.actors.entity.PuzzleTriggerBlockEntity;
import com.lying.block.entity.logic.PortEntry;
import com.lying.init.CDBlockEntityTypes;
import com.lying.item.WiringGunItem.WireMode;
import com.mojang.serialization.MapCodec;

import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class PuzzleTriggerBlock extends AbstractTrapActorBlock
{
	public static final MapCodec<PuzzleTriggerBlock> CODEC = PuzzleTriggerBlock.createCodec(PuzzleTriggerBlock::new);
	
	public PuzzleTriggerBlock(Settings settingsIn)
	{
		super(settingsIn);
	}
	
	protected MapCodec<? extends BlockWithEntity> getCodec() { return CODEC; }
	
	public BlockEntity createBlockEntity(BlockPos pos, BlockState state)
	{
		return new PuzzleTriggerBlockEntity(pos, state);
	}
	
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type)
	{
		return PuzzleTriggerBlockEntity.getTicker(world, state, type);
	}
	
	public int wireCount(BlockPos pos, World world) { return world.getBlockEntity(pos, CDBlockEntityTypes.PUZZLE_TRIGGER.get()).get().wireCount(); }
	
	public boolean acceptWireFrom(Port input, BlockPos target, WireMode space, PortEntry output, World world)
	{
		world.getBlockEntity(target, CDBlockEntityTypes.PUZZLE_TRIGGER.get()).ifPresent(t -> t.processInputConnection(input, output, space));
		return true;
	}
	
	public void clearWires(BlockPos pos, World world)
	{
		world.getBlockEntity(pos, CDBlockEntityTypes.PUZZLE_TRIGGER.get()).get().reset();
	}
	
	public boolean isActive(BlockPos pos, World world) { return false; }
	
	public void trigger(BlockPos pos, World world)
	{
		world.getBlockEntity(pos, CDBlockEntityTypes.PUZZLE_TRIGGER.get()).ifPresent(PuzzleTriggerBlockEntity::onTrigger);
	}
}

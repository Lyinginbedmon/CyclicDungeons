package com.lying.block;

import java.util.List;

import com.lying.block.entity.TrapLogicBlockEntity;
import com.lying.init.CDBlockEntityTypes;
import com.lying.init.CDLogicGates;
import com.lying.item.WiringGunItem.WireMode;
import com.mojang.serialization.MapCodec;

import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class TrapLogicBlock extends BlockWithEntity implements IWireableBlock
{
	public static final MapCodec<TrapLogicBlock> CODEC = TrapLogicBlock.createCodec(TrapLogicBlock::new);
	
	public TrapLogicBlock(Settings settings)
	{
		super(settings);
	}
	
	public BlockEntity createBlockEntity(BlockPos pos, BlockState state)
	{
		return new TrapLogicBlockEntity(pos, state);
	}
	
	protected MapCodec<? extends BlockWithEntity> getCodec()
	{
		return CODEC;
	}
	
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type)
	{
		return TrapLogicBlockEntity.getTicker(world, state, type);
	}
	
	public WireRecipient type() { return WireRecipient.LOGIC; }
	
	public void respondToPorts(BlockPos pos, World world)
	{
		world.getBlockEntity(pos, CDBlockEntityTypes.TRAP_LOGIC.get()).ifPresent(t -> t.respondToPorts());
	}
	
	public List<String> inputPorts(BlockPos pos, World world) { return List.of(CDLogicGates.INPUT); }
	public List<String> outputPorts(BlockPos pos, World world) { return List.of(CDLogicGates.OUTPUT); }
	
	public boolean acceptWireTo(String output, BlockPos target, WireMode space, BlockPos pos, String input, World world)
	{
		world.getBlockEntity(pos, CDBlockEntityTypes.TRAP_LOGIC.get()).ifPresent(t -> t.processOutputConnection(output, pos, input, space));
		return true;
	}
	
	public boolean acceptWireFrom(String input, BlockPos target, WireMode space, BlockPos pos, String output, World world)
	{
		world.getBlockEntity(pos, CDBlockEntityTypes.TRAP_LOGIC.get()).ifPresent(t -> t.processInputConnection(input, pos, output, space));
		return true;
	}
	
	public void clearWires(BlockPos pos, World world)
	{
		world.getBlockEntity(pos, CDBlockEntityTypes.TRAP_LOGIC.get()).get().reset();
	}
	
	public int wireCount(BlockPos pos, World world) { return world.getBlockEntity(pos, CDBlockEntityTypes.TRAP_LOGIC.get()).get().wireCount(); }
}

package com.lying.block;

import org.jetbrains.annotations.Nullable;

import com.lying.block.entity.TrapLogicBlockEntity;
import com.lying.init.CDBlockEntityTypes;
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
	
	@SuppressWarnings("unchecked")
	@Nullable
	public static <E extends BlockEntity, A extends BlockEntity> BlockEntityTicker<A> validateTicker(BlockEntityType<A> given, BlockEntityType<E> expected, BlockEntityTicker<? super E> ticker)
	{
		return expected == given ? (BlockEntityTicker<A>)ticker : null;
	}
	
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type)
	{
		return TrapLogicBlockEntity.getTicker(world, state, type);
	}
	
	public WireRecipient type() { return WireRecipient.LOGIC; }
	
	public boolean acceptWireTo(WireRecipient type, BlockPos target, BlockPos pos, World world)
	{
		TrapLogicBlockEntity tile = world.getBlockEntity(pos, CDBlockEntityTypes.TRAP_LOGIC.get()).get();
		return type != WireRecipient.LOGIC && tile.processWireConnection(target, type);
	}
	
	public void clearWires(BlockPos pos, World world)
	{
		world.getBlockEntity(pos, CDBlockEntityTypes.TRAP_LOGIC.get()).get().reset();
	}
	
	public int wireCount(BlockPos pos, World world) { return world.getBlockEntity(pos, CDBlockEntityTypes.TRAP_LOGIC.get()).get().wireCount(); }
}

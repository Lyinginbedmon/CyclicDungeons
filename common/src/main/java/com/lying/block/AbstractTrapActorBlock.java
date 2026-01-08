package com.lying.block;

import org.jetbrains.annotations.Nullable;

import com.lying.block.entity.TrapActorBlockEntity;
import com.lying.init.CDBlockEntityTypes;

import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public abstract class AbstractTrapActorBlock extends BlockWithEntity implements IWireableBlock
{
	protected AbstractTrapActorBlock(Settings settingsIn)
	{
		super(settingsIn.strength(50F, 0F).dropsNothing());
	}
	
	public BlockEntity createBlockEntity(BlockPos pos, BlockState state)
	{
		return new TrapActorBlockEntity(pos, state);
	}
	
	@SuppressWarnings("unchecked")
	@Nullable
	public static <E extends BlockEntity, A extends BlockEntity> BlockEntityTicker<A> validateTicker(BlockEntityType<A> given, BlockEntityType<E> expected, BlockEntityTicker<? super E> ticker)
	{
		return expected == given ? (BlockEntityTicker<A>)ticker : null;
	}
	
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type)
	{
		return TrapActorBlockEntity.getTicker(world, state, type);
	}
	
	public WireRecipient type() { return WireRecipient.ACTOR; }
	
	public boolean acceptWireTo(WireRecipient type, BlockPos target, BlockPos pos, World world)
	{
		return world.getBlockEntity(pos, CDBlockEntityTypes.TRAP_ACTOR.get()).get().processWireConnection(target, type);
	}
	
	public void clearWires(BlockPos pos, World world)
	{
		world.getBlockEntity(pos, CDBlockEntityTypes.TRAP_ACTOR.get()).get().reset();
	}
}

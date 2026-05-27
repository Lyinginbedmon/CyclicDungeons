package com.lying.block;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.lying.block.entity.TrapActorBlockEntity;
import com.lying.init.CDBlockEntityTypes;
import com.lying.init.CDLogicGates;
import com.lying.item.WiringGunItem.WireMode;

import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public abstract class AbstractTrapActorBlock extends BlockWithEntity implements IWireableBlock, ITrapActor
{
	protected AbstractTrapActorBlock(Settings settingsIn)
	{
		super(settingsIn.strength(50F, 0F).dropsNothing());
	}
	
	public BlockEntity createBlockEntity(BlockPos pos, BlockState state)
	{
		return new TrapActorBlockEntity<>(pos, state);
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
	
	public List<String> inputPorts(BlockPos pos, World world) { return List.of(CDLogicGates.INPUT); }
	public List<String> outputPorts(BlockPos pos, World world) { return List.of(); }
	
	public void respondToPorts(BlockPos pos, World world)
	{
		((TrapActorBlockEntity<?>)world.getBlockEntity(pos)).respondToPorts();
	}
	
	public boolean acceptWireTo(String output, BlockPos target, WireMode space, BlockPos pos, String input, World world) { return false; }
	
	public boolean acceptWireFrom(String input, BlockPos me, WireMode space, BlockPos pos, String output, World world)
	{
		world.getBlockEntity(me, CDBlockEntityTypes.TRAP_ACTOR.get()).ifPresent(t -> t.processInputConnection(input, pos, output, space));
		return true;
	}
	
	public void clearWires(BlockPos pos, World world)
	{
		world.getBlockEntity(pos, CDBlockEntityTypes.TRAP_ACTOR.get()).get().reset();
	}
}

package com.lying.block.actors;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.lying.block.ITrapActor;
import com.lying.block.Port;
import com.lying.block.actors.entity.TrapActorBlockEntity;
import com.lying.block.entity.logic.WiringManifest.PortEntry;
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

public abstract class AbstractTrapActorBlock extends BlockWithEntity implements ITrapActor
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
	
	public List<Port> inputPorts(BlockPos pos, World world) { return List.of(CDLogicGates.INPUT); }
	public List<Port> outputPorts(BlockPos pos, World world) { return List.of(); }
	
	public void respondToPorts(BlockPos pos, World world)
	{
		world.getBlockEntity(pos, CDBlockEntityTypes.TRAP_ACTOR.get()).ifPresent(t -> t.respondToPorts());
	}
	
	public boolean isPortActive(Port port, BlockPos pos, World world)
	{
		return false;
	}
	
	public boolean acceptWireTo(Port output, BlockPos me, WireMode space, PortEntry input, World world)
	{
		return false;
	}
	
	public boolean acceptWireFrom(Port input, BlockPos me, WireMode space, PortEntry output, World world)
	{
		world.getBlockEntity(me, CDBlockEntityTypes.TRAP_ACTOR.get()).ifPresent(t -> t.processInputConnection(input, output, space));
		return true;
	}
	
	public void clearWires(BlockPos pos, World world)
	{
		world.getBlockEntity(pos, CDBlockEntityTypes.TRAP_ACTOR.get()).get().reset();
	}
}

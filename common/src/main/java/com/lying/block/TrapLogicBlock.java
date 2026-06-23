package com.lying.block;

import java.util.List;
import java.util.Optional;

import com.lying.block.entity.TrapLogicBlockEntity;
import com.lying.block.entity.logic.PortEntry;
import com.lying.init.CDBlockEntityTypes;
import com.lying.init.CDLogicGates;
import com.lying.item.WiringGunItem.WireMode;
import com.mojang.serialization.MapCodec;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

public class TrapLogicBlock extends BlockWithEntity implements IWireableBlock
{
	public static final MapCodec<TrapLogicBlock> CODEC = TrapLogicBlock.createCodec(TrapLogicBlock::new);
	public static final EnumProperty<Direction> FACING	= Properties.HORIZONTAL_FACING;
	
	// FIXME Convert to modular logic when clicked with punch card
	
	public TrapLogicBlock(Settings settings)
	{
		super(settings);
		setDefaultState(getDefaultState().with(FACING, Direction.NORTH));
	}
	
	public BlockEntity createBlockEntity(BlockPos pos, BlockState state)
	{
		return new TrapLogicBlockEntity(pos, state);
	}
	
	protected void appendProperties(StateManager.Builder<Block, BlockState> builder)
	{
		builder.add(FACING);
	}
	
	public BlockState getPlacementState(ItemPlacementContext ctx)
	{
		return getDefaultState().with(FACING, ctx.getHorizontalPlayerFacing().getOpposite());
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
	
	public List<Port> inputPorts(BlockPos pos, World world) { return List.of(CDLogicGates.INPUT); }
	public List<Port> outputPorts(BlockPos pos, World world) { return List.of(CDLogicGates.OUTPUT); }
	
	public boolean isPortActive(Port port, BlockPos pos, World world)
	{
		Optional<TrapLogicBlockEntity> tile;
		return 
				outputPorts(pos, world).contains(port) && 
				(tile = world.getBlockEntity(pos, CDBlockEntityTypes.TRAP_LOGIC.get())).isPresent() && 
				tile.get().getOutput(port);
	}
	
	public boolean acceptWireTo(Port output, BlockPos target, WireMode space, PortEntry input, World world)
	{
		world.getBlockEntity(target, CDBlockEntityTypes.TRAP_LOGIC.get()).ifPresent(t -> t.processOutputConnection(output, input, space));
		return true;
	}
	
	public boolean acceptWireFrom(Port input, BlockPos target, WireMode space, PortEntry output, World world)
	{
		world.getBlockEntity(target, CDBlockEntityTypes.TRAP_LOGIC.get()).ifPresent(t -> t.processInputConnection(input, output, space));
		return true;
	}
	
	public void clearWires(BlockPos pos, World world)
	{
		world.getBlockEntity(pos, CDBlockEntityTypes.TRAP_LOGIC.get()).get().reset();
	}
	
	public int wireCount(BlockPos pos, World world) { return world.getBlockEntity(pos, CDBlockEntityTypes.TRAP_LOGIC.get()).get().wireCount(); }
}

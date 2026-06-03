package com.lying.block;

import java.util.List;
import java.util.Optional;
import java.util.function.ToIntFunction;

import com.lying.block.entity.ModularLogicBlockEntity;
import com.lying.block.entity.logic.PortEntry;
import com.lying.init.CDBlockEntityTypes;
import com.lying.item.WiringGunItem.WireMode;
import com.lying.network.ShowCircuitScreenPacket;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.IntProperty;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class ModularLogicBlock extends TrapLogicBlock
{
	public static final IntProperty LIGHT	= IntProperty.of("light", 0, 15);
	public static final ToIntFunction<BlockState> STATE_TO_LUMINANCE = state -> (Integer)state.get(LIGHT);
	
	public ModularLogicBlock(Settings settings)
	{
		super(settings);
		setDefaultState(getDefaultState().with(LIGHT, 0));
	}
	
	public BlockEntity createBlockEntity(BlockPos pos, BlockState state)
	{
		return new ModularLogicBlockEntity(pos, state);
	}
	
	protected void appendProperties(StateManager.Builder<Block, BlockState> builder)
	{
		builder.add(LIGHT);
	}
	
	protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit)
	{
		if(!world.isClient() && player.isCreative())
			ShowCircuitScreenPacket.sendTo((ServerPlayerEntity)player);
		return ActionResult.CONSUME;
	}
	
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type)
	{
		return ModularLogicBlockEntity.getTicker(world, state, type);
	}
	
	public List<Port> inputPorts(BlockPos pos, World world)
	{
		Optional<ModularLogicBlockEntity> opt = world.getBlockEntity(pos, CDBlockEntityTypes.MODULAR_LOGIC.get());
		return opt.isEmpty() ? List.of() : opt.get().inputPorts();
	}
	
	public List<Port> outputPorts(BlockPos pos, World world)
	{
		Optional<ModularLogicBlockEntity> opt = world.getBlockEntity(pos, CDBlockEntityTypes.MODULAR_LOGIC.get());
		return opt.isEmpty() ? List.of() : opt.get().outputPorts();
	}
	
	public boolean isPortActive(Port port, BlockPos pos, World world)
	{
		Optional<ModularLogicBlockEntity> tile;
		return 
				outputPorts(pos, world).contains(port) && 
				(tile = world.getBlockEntity(pos, CDBlockEntityTypes.MODULAR_LOGIC.get())).isPresent() && 
				tile.get().getOutput(port);
	}
	
	public boolean acceptWireTo(Port output, BlockPos target, WireMode space, PortEntry input, World world)
	{
		world.getBlockEntity(target, CDBlockEntityTypes.MODULAR_LOGIC.get()).ifPresent(t -> t.processOutputConnection(output, input, space));
		return true;
	}
	
	public boolean acceptWireFrom(Port input, BlockPos target, WireMode space, PortEntry output, World world)
	{
		world.getBlockEntity(target, CDBlockEntityTypes.MODULAR_LOGIC.get()).ifPresent(t -> t.processInputConnection(input, output, space));
		return true;
	}
	
	public void clearWires(BlockPos pos, World world)
	{
		world.getBlockEntity(pos, CDBlockEntityTypes.MODULAR_LOGIC.get()).get().reset();
	}
	
	public int wireCount(BlockPos pos, World world) { return world.getBlockEntity(pos, CDBlockEntityTypes.MODULAR_LOGIC.get()).get().wireCount(); }
}

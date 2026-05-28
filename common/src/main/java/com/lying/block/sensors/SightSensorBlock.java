package com.lying.block.sensors;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.lying.block.IWireableBlock;
import com.lying.block.sensors.entity.SightSensorBlockEntity;
import com.lying.init.CDBlocks;
import com.lying.init.CDLogicGates;
import com.lying.item.WiringGunItem.WireMode;
import com.mojang.serialization.MapCodec;

import net.minecraft.block.Block;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.IntProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

public class SightSensorBlock extends AbstractTrapSensorBlock implements IWireableBlock, BlockEntityProvider
{
	public static final MapCodec<SightSensorBlock> CODEC	= createCodec(SightSensorBlock::new);
	protected static final VoxelShape SHAPE	= Block.createCuboidShape(2, 2, 2, 14, 14, 14);
	public static final IntProperty POWER	= Properties.POWER;
	public static final BooleanProperty POWERED	= Properties.POWERED;
	
	public SightSensorBlock(Settings settings)
	{
		super(settings.strength(50F, 0F).dropsNothing().nonOpaque().emissiveLighting(CDBlocks::always).luminance(state -> state.get(POWERED) ? 4 : 0));
		setDefaultState(getDefaultState().with(POWER, 0).with(POWERED, false));
	}
	
	public BlockEntity createBlockEntity(BlockPos pos, BlockState state)
	{
		return new SightSensorBlockEntity(pos, state);
	}
	
	protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context)
	{
		return SHAPE;
	}
	
	protected BlockRenderType getRenderType(BlockState state)
	{
		return BlockRenderType.INVISIBLE;
	}
	
	protected void appendProperties(StateManager.Builder<Block, BlockState> builder)
	{
		builder.add(POWER, POWERED);
	}
	
	public int portActivity(BlockPos pos, World world) { return world.getBlockState(pos).get(POWER); }
	
	public boolean isPortActive(String port, BlockPos pos, World world)
	{
		return super.isPortActive(port, pos, world) && world.getBlockState(pos).get(POWERED);
	}
	
	public boolean acceptWireTo(String output, BlockPos target, WireMode space, BlockPos pos, String input, World world) { return true; }
	
	public boolean acceptWireFrom(String input, BlockPos target, WireMode space, BlockPos pos, String output, World world) { return false; }
	
	public WireRecipient type() { return WireRecipient.SENSOR; }
	
	/** Sensors don't need to respond to ports because they only transmit signals */
	public void respondToPorts(BlockPos pos, World world) { }
	
	public List<String> inputPorts(BlockPos pos, World world) { return List.of(); }
	public List<String> outputPorts(BlockPos pos, World world) { return List.of(CDLogicGates.OUTPUT); }
	
	@SuppressWarnings("unchecked")
	@Nullable
	public static <E extends BlockEntity, A extends BlockEntity> BlockEntityTicker<A> validateTicker(BlockEntityType<A> given, BlockEntityType<E> expected, BlockEntityTicker<? super E> ticker)
	{
		return expected == given ? (BlockEntityTicker<A>)ticker : null;
	}
	
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type)
	{
		return SightSensorBlockEntity.getTicker(world, state, type);
	}
}

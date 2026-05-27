package com.lying.block;

import java.util.function.ToIntFunction;

import com.lying.block.entity.ModularLogicBlockEntity;
import com.mojang.serialization.MapCodec;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.IntProperty;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class ModularLogicBlock extends BlockWithEntity
{
	public static final MapCodec<ModularLogicBlock> CODEC = ModularLogicBlock.createCodec(ModularLogicBlock::new);
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
	
	protected MapCodec<? extends BlockWithEntity> getCodec() { return CODEC; }
	
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type)
	{
		return ModularLogicBlockEntity.getTicker(world, state, type);
	}
}

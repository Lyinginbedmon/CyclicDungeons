package com.lying.block;

import com.lying.block.entity.ModularLogicBlockEntity;
import com.mojang.serialization.MapCodec;

import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class ModularLogicBlock extends BlockWithEntity
{
	public static final MapCodec<ModularLogicBlock> CODEC = ModularLogicBlock.createCodec(ModularLogicBlock::new);
	
	public ModularLogicBlock(Settings settings)
	{
		super(settings);
	}
	
	public BlockEntity createBlockEntity(BlockPos pos, BlockState state)
	{
		return new ModularLogicBlockEntity(pos, state);
	}
	
	protected MapCodec<? extends BlockWithEntity> getCodec() { return CODEC; }
	
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type)
	{
		return ModularLogicBlockEntity.getTicker(world, state, type);
	}

}

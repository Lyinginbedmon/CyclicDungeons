package com.lying.block;

import com.lying.block.entity.SpawnerActorBlockEntity;
import com.mojang.serialization.MapCodec;

import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.util.math.BlockPos;

public class SpawnerActorBlock extends AbstractTrapActorBlock implements BlockEntityProvider
{
	public static final MapCodec<SpawnerActorBlock> CODEC = RedstoneActorBlock.createCodec(SpawnerActorBlock::new);
	
	public SpawnerActorBlock(Settings settingsIn)
	{
		super(settingsIn);
	}
	
	protected MapCodec<? extends BlockWithEntity> getCodec() { return CODEC; }
	
	public BlockEntity createBlockEntity(BlockPos pos, BlockState state)
	{
		return new SpawnerActorBlockEntity(pos, state);
	}
}

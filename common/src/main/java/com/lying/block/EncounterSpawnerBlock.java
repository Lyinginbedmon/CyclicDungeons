package com.lying.block;

import org.jetbrains.annotations.Nullable;

import com.lying.block.entity.EncounterSpawnerBlockEntity;
import com.mojang.serialization.MapCodec;

import net.minecraft.block.AbstractBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class EncounterSpawnerBlock extends BlockWithEntity
{
	public static final MapCodec<EncounterSpawnerBlock> CODEC	= createCodec(EncounterSpawnerBlock::new);
	
	public EncounterSpawnerBlock(AbstractBlock.Settings settingsIn)
	{
		super(settingsIn);
	}
	
	public BlockEntity createBlockEntity(BlockPos pos, BlockState state)
	{
		return new EncounterSpawnerBlockEntity(pos, state);
	}
	
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type)
	{
		return EncounterSpawnerBlockEntity.getTicker(world, state, type);
	}
	
	@SuppressWarnings("unchecked")
	@Nullable
	public static <E extends BlockEntity, A extends BlockEntity> BlockEntityTicker<A> validateTicker(BlockEntityType<A> given, BlockEntityType<E> expected, BlockEntityTicker<? super E> ticker)
	{
		return expected == given ? (BlockEntityTicker<A>)ticker : null;
	}
	
	protected MapCodec<? extends BlockWithEntity> getCodec() { return CODEC; }
}

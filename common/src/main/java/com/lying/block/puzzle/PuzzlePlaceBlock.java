package com.lying.block.puzzle;

import org.jetbrains.annotations.Nullable;

import com.lying.block.puzzle.entity.PuzzlePlaceBlockEntity;
import com.mojang.serialization.MapCodec;

import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class PuzzlePlaceBlock extends BlockWithEntity
{
	public static final MapCodec<PuzzlePlaceBlock> CODEC = PuzzlePlaceBlock.createCodec(PuzzlePlaceBlock::new);
	
	public PuzzlePlaceBlock(Settings settingsIn)
	{
		super(settingsIn);
	}
	
	public BlockEntity createBlockEntity(BlockPos pos, BlockState state)
	{
		return new PuzzlePlaceBlockEntity(pos, state);
	}
	
	@SuppressWarnings("unchecked")
	@Nullable
	public static <E extends BlockEntity, A extends BlockEntity> BlockEntityTicker<A> validateTicker(BlockEntityType<A> given, BlockEntityType<E> expected, BlockEntityTicker<? super E> ticker)
	{
		return expected == given ? (BlockEntityTicker<A>)ticker : null;
	}
	
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type)
	{
		return PuzzlePlaceBlockEntity.getTicker(world, state, type);
	}
	
	protected MapCodec<? extends BlockWithEntity> getCodec() { return CODEC; }
}

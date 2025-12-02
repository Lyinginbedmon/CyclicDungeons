package com.lying.block;

import com.mojang.serialization.MapCodec;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Direction.Axis;

public class SwingingBladeBlock extends Block
{
	public static final MapCodec<SwingingBladeBlock> CODEC = RedstoneActorBlock.createCodec(SwingingBladeBlock::new);
	
	public static final EnumProperty<Direction> FACING	= Properties.FACING;
	public static final EnumProperty<Direction.Axis> AXIS	= Properties.AXIS;
	
	public SwingingBladeBlock(Settings settings)
	{
		super(settings.nonOpaque());
		setDefaultState(getDefaultState().with(FACING, Direction.UP).with(AXIS, Axis.X));
	}
	
//	protected MapCodec<? extends BlockWithEntity> getCodec()
//	{
//		return CODEC;
//	}
	
	protected void appendProperties(StateManager.Builder<Block, BlockState> builder)
	{
		builder.add(FACING, AXIS);
	}
	
	public BlockState getPlacementState(ItemPlacementContext ctx)
	{
		Direction.Axis look = ctx.getPlayerLookDirection().getAxis();
		Direction face = ctx.getSide();
		if(face.getAxis() == look)
			switch(look)
			{
				case X:
					look = Direction.Axis.Z;
					break;
				case Y:
				case Z:
					look = Direction.Axis.X;
					break;
			}
		
		return getDefaultState()
				.with(FACING, face)
				.with(AXIS, look);
	}
}

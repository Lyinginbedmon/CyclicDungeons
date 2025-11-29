package com.lying.block;

import com.lying.init.CDBlocks;

import dev.architectury.event.events.common.TickEvent;
import net.minecraft.block.Block;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

public class PitBlock extends Block
{
	public PitBlock(Settings settings)
	{
		super(settings.nonOpaque().noCollision());
	}
	
	public static void registerEvent()
	{
		TickEvent.PLAYER_POST.register(p -> 
		{
			World world = p.getWorld();
			if(world.isClient())
				return;
			
			if(world.getBlockState(p.getBlockPos()).isOf(CDBlocks.PIT.get()))
				p.damage((ServerWorld)world, p.getDamageSources().fall(), 4F);
		});
	}
	
	protected VoxelShape getOutlineShape(BlockState state, BlockView World, BlockPos pos, ShapeContext context)
	{
		// FIXME Full cube when holding pit block
		return VoxelShapes.fullCube();
	}
	
	protected BlockRenderType getRenderType(BlockState state)
	{
		return BlockRenderType.INVISIBLE;
	}
}

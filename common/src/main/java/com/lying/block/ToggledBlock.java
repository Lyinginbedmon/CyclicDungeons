package com.lying.block;

import com.lying.init.CDBlockEntityTypes;
import com.mojang.serialization.MapCodec;

import net.minecraft.block.BlockWithEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class ToggledBlock extends AbstractTrapActorBlock
{
	public static final MapCodec<ToggledBlock> CODEC = RedstoneActorBlock.createCodec(ToggledBlock::new);
	
	public ToggledBlock(Settings settingsIn)
	{
		super(settingsIn);
	}
	
	protected MapCodec<? extends BlockWithEntity> getCodec() { return CODEC; }
	
	public boolean acceptWireTo(WireRecipient type, BlockPos target, BlockPos pos, World world)
	{
		return world.getBlockEntity(pos, CDBlockEntityTypes.TRAP_ACTOR.get()).get().processWireConnection(target, type);
	}
	
	public void clearWires(BlockPos pos, World world)
	{
		world.getBlockEntity(pos, CDBlockEntityTypes.TRAP_ACTOR.get()).get().reset();
	}
	
	public boolean isActive(BlockPos pos, World world)
	{
		return false;
	}
	
	public void trigger(BlockPos pos, World world)
	{
		world.breakBlock(pos, false);
	}
}

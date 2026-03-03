package com.lying.grammar.content.trap;

import org.joml.Vector2i;

import com.lying.reference.Reference;

import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;

public class LandmineTrapEntry extends StructurePlacerTrapEntry
{
	public LandmineTrapEntry(Identifier idIn)
	{
		super(idIn, Reference.ModInfo.prefix("trap/landmine"), new BlockPos(0, -2, 0));
	}
	
	protected int getTrapCountForRoom(Random rand, Vector2i roomSize) { return (roomSize.x * roomSize.y) / 8; }
	
	protected int minimumSpacing() { return 4; }
	
	protected boolean isPosViableForTrap(BlockPos pos, ServerWorld world)
	{
		return world.isAir(pos) && world.getBlockState(pos.down()).isOpaqueFullCube() && !world.getBlockState(pos.down(2)).isAir();
	}
}

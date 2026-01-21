package com.lying.client.renderer.block;

import com.lying.block.SpikeTrapBlock;
import com.lying.block.SpikesBlock;
import com.lying.block.SpikesBlock.SpikePart;
import com.lying.block.entity.SpikeTrapBlockEntity;
import com.lying.init.CDBlocks;

import net.minecraft.block.BlockState;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.BlockRenderManager;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory.Context;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;

public class SpikeTrapBlockEntityRenderer extends WireableBlockEntityRenderer<SpikeTrapBlockEntity>
{
	private final BlockRenderManager blockRenderManager;
	
	public SpikeTrapBlockEntityRenderer(Context context)
	{
		super(context);
		blockRenderManager = context.getRenderManager();
	}
	
	public void render(SpikeTrapBlockEntity entity, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, int overlay)
	{
		super.render(entity, tickDelta, matrices, vertexConsumers, light, overlay);

		float extension = entity.extension(tickDelta);
		if(extension <= 0F)
			return;
		
		extension *= entity.maxExtension();
		extension -= 1F;
		
		BlockState trapState = entity.getCachedState();
		Direction facing = trapState.get(SpikeTrapBlock.FACING);
		BlockState tipState = CDBlocks.SPIKES.get().getDefaultState().with(SpikesBlock.FACING, facing);
		
		World world = entity.getWorld();
		matrices.translate(
				extension * facing.getOffsetX(), 
				extension * facing.getOffsetY(),
				extension * facing.getOffsetZ());
		
		blockRenderManager.getModelRenderer().render(
				world, 
				blockRenderManager.getModel(tipState), 
				tipState, 
				entity.getPos(), 
				matrices, 
				vertexConsumers.getBuffer(RenderLayer.getCutout()), 
				false, 
				Random.create(), 
				light, 
				overlay);
		
		final BlockState poleState = tipState.with(SpikesBlock.PART, SpikePart.POLE);
		while(extension-- > 0F)
		{
			matrices.translate(
					-facing.getOffsetX(), 
					-facing.getOffsetY(),
					-facing.getOffsetZ());
			blockRenderManager.getModelRenderer().render(
					world, 
					blockRenderManager.getModel(poleState), 
					poleState, 
					entity.getPos(), 
					matrices, 
					vertexConsumers.getBuffer(RenderLayer.getCutout()), 
					false, 
					Random.create(), 
					light, 
					overlay);
		}
	}
}

package com.lying.client.renderer.block;

import com.lying.block.entity.SightSensorBlockEntity;

import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.BlockRenderManager;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;

public class SightSensorBlockEntityRenderer implements BlockEntityRenderer<SightSensorBlockEntity>
{
	private final BlockRenderManager blockRenderManager;
	
	public SightSensorBlockEntityRenderer(BlockEntityRendererFactory.Context ctx)
	{
		blockRenderManager = ctx.getRenderManager();
	}
	
	public void render(SightSensorBlockEntity entity, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, int overlay)
	{
		final BlockPos tilePos = entity.getPos();
		Vec3d look = entity.clientLookVec;
		
		matrices.push();
			matrices.translate(0.5D, 0.5D, 0.5D);
			matrices.scale(0.8F, 0.8F, 0.8F);
			matrices.multiply(RotationAxis.POSITIVE_Y.rotation((float)Math.atan2(look.x, look.z)));
			matrices.multiply(RotationAxis.POSITIVE_X.rotation((float)Math.asin(-look.y)));
			matrices.translate(-0.5D, -0.5D, -0.5D);
			matrices.push();
				blockRenderManager.getModelRenderer().render(
						entity.getWorld(), 
						blockRenderManager.getModel(entity.getCachedState()), 
						entity.getCachedState(), 
						tilePos, 
						matrices, 
						vertexConsumers.getBuffer(RenderLayer.getSolid()), 
						false, 
						Random.create(), 
						0L, 
						overlay);
			matrices.pop();
		matrices.pop();
	}
}

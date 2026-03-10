package com.lying.client.renderer.block;

import com.lying.block.entity.TrapSpawnerBlockEntity;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.VertexRendering;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory.Context;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Box;

public class TrapSpawnerBlockEntityRenderer extends WireableBlockEntityRenderer<TrapSpawnerBlockEntity>
{
	public static final MinecraftClient mc = MinecraftClient.getInstance();
	
	public TrapSpawnerBlockEntityRenderer(Context context)
	{
		super(context);
	}
	
	public void render(TrapSpawnerBlockEntity entity, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, int overlay)
	{
		super.render(entity, tickDelta, matrices, vertexConsumers, light, overlay);
		
		if(!mc.player.isCreativeLevelTwoOp())
			return;
		
		Box spawnArea = entity.getFullSpawningArea();
		VertexConsumer vertexConsumer = vertexConsumers.getBuffer(RenderLayer.getLines());
		VertexRendering.drawBox(matrices, vertexConsumer, spawnArea.minX, spawnArea.minY, spawnArea.minZ, spawnArea.maxX, spawnArea.maxY, spawnArea.maxZ, 0.9F, 0.9F, 0.9F, 1.0F, 0.5F, 0.5F, 0.5F);
	}
}

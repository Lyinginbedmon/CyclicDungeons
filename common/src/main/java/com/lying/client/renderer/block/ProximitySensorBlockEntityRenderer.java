package com.lying.client.renderer.block;

import com.lying.block.entity.ProximitySensorBlockEntity;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory.Context;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;

public class ProximitySensorBlockEntityRenderer extends WireableBlockEntityRenderer<ProximitySensorBlockEntity>
{
	private static final MinecraftClient mc = MinecraftClient.getInstance();
	
	public ProximitySensorBlockEntityRenderer(Context context)
	{
		super(context);
	}
	
	public void render(ProximitySensorBlockEntity entity, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, int overlay)
	{
		super.render(entity, tickDelta, matrices, vertexConsumers, light, overlay);
		
		PlayerEntity player = mc.player;
		if(player == null || !ProximitySensorBlockEntity.PREDICATE.test(player) || !entity.shouldRenderFor(player))
			return;
		
		// TODO Add activation range rendering
		double range = entity.getSearchRange();
	}
}

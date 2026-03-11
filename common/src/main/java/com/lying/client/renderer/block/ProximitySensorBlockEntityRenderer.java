package com.lying.client.renderer.block;

import org.joml.Matrix4f;

import com.lying.block.entity.ProximitySensorBlockEntity;
import com.lying.reference.Reference;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory.Context;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Direction;

public class ProximitySensorBlockEntityRenderer extends WireableBlockEntityRenderer<ProximitySensorBlockEntity>
{
	private static final MinecraftClient mc = MinecraftClient.getInstance();
	private static final Identifier RADIUS_TEXTURE = Reference.ModInfo.prefix("textures/proximity.png");
	private static final RenderLayer LAYER	= RenderLayer.getEntityCutoutNoCull(RADIUS_TEXTURE);
	
	public ProximitySensorBlockEntityRenderer(Context context)
	{
		super(context);
	}
	
	public void render(ProximitySensorBlockEntity entity, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, int overlay)
	{
		super.render(entity, tickDelta, matrices, vertexConsumers, light, overlay);
		
		PlayerEntity player = mc.player;
		if(player == null)// || !ProximitySensorBlockEntity.PREDICATE.test(player) || !entity.shouldRenderFor(player))
			return;
		
		// FIXME Add activation range rendering
//		Vec3d eyeVec = player.getEyePos();
//		Vec3d blockVec = new Vec3d(entity.getPos().getX(), entity.getPos().getY(), entity.getPos().getZ()).add(0.5D);
//		Direction normal = Direction.getFacing(eyeVec.subtract(blockVec));
//		
//		double radius = entity.getSearchRadius();
//		Vec3d max = new Vec3d(
//				normal.getAxis() == Axis.X ? 0 : 1, 
//				normal.getAxis() == Axis.Y ? 0 : 1, 
//				normal.getAxis() == Axis.Z ? 0 : 1).multiply(radius);
//		Vec3d min = max.negate();
//		
//		Direction facing = entity.getCachedState().get(ProximitySensorBlock.FACING);
//		Vec3d facingOffset = Vec3d.ZERO.add(0.5D).add(new Vec3d(facing.getOffsetX(), facing.getOffsetY(), facing.getOffsetZ()).multiply(0.5D).negate());
//		max = max.add(facingOffset);
//		min = min.add(facingOffset);
//		
//		VertexConsumer vertexConsumer = vertexConsumers.getBuffer(LAYER);
//		MatrixStack.Entry entry = matrices.peek();
//		Matrix4f matrix = entry.getPositionMatrix();
//		matrices.push();
//			vertex(vertexConsumer, entry, matrix, (float)max.x, (float)max.y, (float)max.z, 0F, 0F, normal);
//			vertex(vertexConsumer, entry, matrix, (float)max.x, (float)min.y, (float)max.z, 0F, 1F, normal);
//			vertex(vertexConsumer, entry, matrix, (float)min.x, (float)min.y, (float)max.z, 1F, 1F, normal);
//			vertex(vertexConsumer, entry, matrix, (float)min.x, (float)max.y, (float)max.z, 1F, 0F, normal);
//		matrices.pop();
	}
	
	private static void vertex(VertexConsumer consumer, MatrixStack.Entry entry, Matrix4f matrix, float x, float y, float z, float u, float v, Direction normal)
	{
		consumer
			.vertex(matrix, x, y, z)
			.color(255, 255, 255, 255)
			.texture(u, v)
			.overlay(OverlayTexture.DEFAULT_UV)
			.light(LightmapTextureManager.MAX_LIGHT_COORDINATE)
			.normal(normal.getOffsetX(), normal.getOffsetY(), normal.getOffsetZ());
	}
}

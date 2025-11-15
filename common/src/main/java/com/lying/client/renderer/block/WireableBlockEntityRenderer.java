package com.lying.client.renderer.block;

import com.lying.block.entity.AbstractWireableBlockEntity;
import com.lying.init.CDItems;
import com.lying.reference.Reference;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;

public class WireableBlockEntityRenderer<T extends AbstractWireableBlockEntity> implements BlockEntityRenderer<T>
{
	public static final MinecraftClient mc = MinecraftClient.getInstance();
	private static final Identifier WIRE_TEXTURE = Reference.ModInfo.prefix("textures/wire.png");
	private static final RenderLayer LAYER = RenderLayer.getEntityCutoutNoCull(WIRE_TEXTURE);
	public static final int WIRE_COLOUR = 0x00A4F9;
	
	public WireableBlockEntityRenderer(BlockEntityRendererFactory.Context ctx) { }
	
	public void render(T entity, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, int overlay)
	{
		if(
				mc.player == null || 
				mc.world == null || 
				!(
					mc.player.getMainHandStack().isOf(CDItems.WIRING_GUN.get()) ||
					mc.player.getOffHandStack().isOf(CDItems.WIRING_GUN.get())
				))
			return;
		
		final BlockPos here = entity.getPos();
		entity.getSensors().forEach(pos -> drawWireBetween(pos, here, here, tickDelta, matrices, vertexConsumers));
		entity.getActors().forEach(pos -> drawWireBetween(here, pos, here, tickDelta, matrices, vertexConsumers));
	}
	
	private static void drawWireBetween(BlockPos start, BlockPos end, BlockPos origin, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers)
	{
		matrices.push();
			Vec3d o = new Vec3d(origin.getX(), origin.getY(), origin.getZ());
			Vec3d v1 = new Vec3d(start.getX(), start.getY(), start.getZ()).add(0.5D);
			Vec3d v2 = new Vec3d(end.getX(), end.getY(), end.getZ()).add(0.5D);
			
            int red = ((WIRE_COLOUR & 0xFF0000) >> 16);
            int green = ((WIRE_COLOUR & 0xFF00) >> 8);
            int blue = ((WIRE_COLOUR & 0xFF) >> 0);
			
			renderWire(v1, v2, o, (float)Math.toRadians(45D), matrices, vertexConsumers, LAYER, 1, red, green, blue);
		matrices.pop();
	}
	
	public static void renderWire(Vec3d start, Vec3d dest, Vec3d origin, float roll, MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, RenderLayer layer, int textureHeight, int red, int green, int blue)
	{
		Vec3d offset = dest.subtract(start);
        matrixStack.push();
        	matrixStack.translate(start.x - origin.x, start.y - origin.y, start.z - origin.z);
	        float chainLength = (float)offset.length();
	        offset = offset.normalize();
	        float n = (float)Math.acos(offset.y);
	        float o = (float)Math.atan2(offset.z, offset.x);
	        matrixStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((1.5707964f - o) * 57.295776f));
	        matrixStack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(n * 57.295776f));
	        float af = MathHelper.cos((float)(roll + (float)Math.PI)) * 0.2f;
	        float ag = MathHelper.sin((float)(roll + (float)Math.PI)) * 0.2f;
	        float ah = MathHelper.cos((float)(roll + 0.0f)) * 0.2f;
	        float ai = MathHelper.sin((float)(roll + 0.0f)) * 0.2f;
	        float aj = MathHelper.cos((float)(roll + 1.5707964f)) * 0.2f;
	        float ak = MathHelper.sin((float)(roll + 1.5707964f)) * 0.2f;
	        float al = MathHelper.cos((float)(roll + 4.712389f)) * 0.2f;
	        float am = MathHelper.sin((float)(roll + 4.712389f)) * 0.2f;
	        float an = chainLength;
	        float textureStart = 0F;
	        float textureEnd = chainLength / (float)textureHeight;
	        VertexConsumer vertexConsumer = vertexConsumerProvider.getBuffer(layer);
	        MatrixStack.Entry entry = matrixStack.peek();
	        vertex(vertexConsumer, entry, af, an, ag, red, green, blue, 0.4999f, textureStart);
	        vertex(vertexConsumer, entry, af, 0.0f, ag, red, green, blue, 0.4999f, textureEnd);
	        vertex(vertexConsumer, entry, ah, 0.0f, ai, red, green, blue, 0.0f, textureEnd);
	        vertex(vertexConsumer, entry, ah, an, ai, red, green, blue, 0.0f, textureStart);
	        
	        vertex(vertexConsumer, entry, aj, an, ak, red, green, blue, 0.4999f, textureStart);
	        vertex(vertexConsumer, entry, aj, 0.0f, ak, red, green, blue, 0.4999f, textureEnd);
	        vertex(vertexConsumer, entry, al, 0.0f, am, red, green, blue, 0.0f, textureEnd);
	        vertex(vertexConsumer, entry, al, an, am, red, green, blue, 0.0f, textureStart);
	    matrixStack.pop();
	}
	
	private static void vertex(VertexConsumer vertexConsumer, MatrixStack.Entry entry, float x, float y, float z, int red, int green, int blue, float u, float v)
	{
			vertexConsumer
				.vertex(entry.getPositionMatrix(), x, y, z)
				.color(red, green, blue, 255)
				.texture(u, v)
				.overlay(OverlayTexture.DEFAULT_UV)
				.light(LightmapTextureManager.MAX_LIGHT_COORDINATE)
				.normal(entry, 0.0f, 1.0f, 0.0f);
	}
}

package com.lying.client.renderer.block;

import org.joml.Matrix4f;
import org.joml.Vector3f;

import com.lying.block.ProximitySensorBlock;
import com.lying.block.entity.ProximitySensorBlockEntity;
import com.lying.reference.Reference;
import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory.Context;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.client.util.SpriteIdentifier;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3i;

public class ProximitySensorBlockEntityRenderer extends WireableBlockEntityRenderer<ProximitySensorBlockEntity>
{
	private static final MinecraftClient mc = MinecraftClient.getInstance();
	@SuppressWarnings("deprecation")
	public static final SpriteIdentifier SPRITE	= new SpriteIdentifier(SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE, Reference.ModInfo.prefix("block/proximity_sensor_overlay"));
	private static final Vector3f[] renderVertices = new Vector3f[]{
			new Vector3f(1F, 1F, 0F), 
			new Vector3f(1F, -1F, 0F), 
			new Vector3f(-1F, -1F, 0F), 
			new Vector3f(-1F, 1F, 0F)};
	static
	{
		for(int i=0; i<4; ++i)
			renderVertices[i] = renderVertices[i].mul(0.5F);
	}
	
	public ProximitySensorBlockEntityRenderer(Context context)
	{
		super(context);
	}
	
	public void render(ProximitySensorBlockEntity entity, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, int overlay)
	{
		PlayerEntity player = mc.player;
		if(player == null || !ProximitySensorBlockEntity.PREDICATE.test(player) || !entity.shouldRenderFor(player))
			;
		else
		{
			final Direction orientation = entity.getCachedState().get(ProximitySensorBlock.FACING);
			Sprite sprite = SPRITE.getSprite();
			matrices.push();
				matrices.translate(0.5D, 0.5D, 0.5D);
				matrices.translate(orientation.getOpposite().getDoubleVector().multiply(0.45D));
				matrices.push();
					switch(orientation.getAxis())
					{
						case X:
							matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(90F));
							break;
						case Y:
							matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(90F));
							break;
						case Z:
							break;
					}
					
					drawSprite(matrices, sprite, 16F * (float)entity.getSearchRadius() * 2F, 1F, 1F, 1F, orientation.getVector());
				matrices.pop();
			matrices.pop();
		}
		
		super.render(entity, tickDelta, matrices, vertexConsumers, light, overlay);
	}
	
	public static void drawSprite(MatrixStack matrixStack, Sprite sprite, float scale, float r, float g, float b, Vec3i normal)
	{
		drawTexturedQuad(matrixStack, sprite.getAtlasId(), sprite.getMinU(), sprite.getMaxU(), sprite.getMinV(), sprite.getMaxV(), scale, r, g, b, normal);
	}
	
	public static void drawTexturedQuad(MatrixStack matrixStack, Identifier texture, float u1, float u2, float v1, float v2, float scale, float r, float g, float b, Vec3i normal)
	{
		Vector3f[] vertices = new Vector3f[4];
		for(int i=0; i<4; ++i)
			vertices[i] = new Vector3f(renderVertices[i]).mul(scale / 16F);
		
		RenderSystem.disableCull();
		RenderLayer layer = RenderLayer.getCutoutMipped();
		layer.startDrawing();
		RenderSystem.setShaderTexture(0, texture);
		RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR_TEX_LIGHTMAP);
		Matrix4f model = matrixStack.peek().getPositionMatrix();
		BufferBuilder vertexConsumer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR_TEXTURE_LIGHT);
			vertexConsumer.vertex(model, vertices[0].x(), vertices[0].y(), vertices[0].z()).color(r, g, b, 1F).texture(u2, v2).light(15).normal(normal.getX(), normal.getY(), normal.getZ());
			vertexConsumer.vertex(model, vertices[1].x(), vertices[1].y(), vertices[1].z()).color(r, g, b, 1F).texture(u2, v1).light(15).normal(normal.getX(), normal.getY(), normal.getZ());
			vertexConsumer.vertex(model, vertices[2].x(), vertices[2].y(), vertices[2].z()).color(r, g, b, 1F).texture(u1, v1).light(15).normal(normal.getX(), normal.getY(), normal.getZ());
			vertexConsumer.vertex(model, vertices[3].x(), vertices[3].y(), vertices[3].z()).color(r, g, b, 1F).texture(u1, v2).light(15).normal(normal.getX(), normal.getY(), normal.getZ());
		BufferRenderer.drawWithGlobalProgram(vertexConsumer.end());
		layer.endDrawing();
		RenderSystem.enableCull();
	}
}

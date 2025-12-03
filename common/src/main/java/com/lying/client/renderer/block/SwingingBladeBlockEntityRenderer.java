package com.lying.client.renderer.block;

import com.lying.block.SwingingBladeBlock;
import com.lying.block.entity.SwingingBladeBlockEntity;
import com.lying.init.CDBlocks;

import net.minecraft.block.BlockState;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.BlockRenderManager;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.random.Random;

public class SwingingBladeBlockEntityRenderer implements BlockEntityRenderer<SwingingBladeBlockEntity>
{
	private static final BlockState BLADE_STATE = CDBlocks.BLADE.get().getDefaultState();
	private final BlockRenderManager blockRenderManager;
	
	public SwingingBladeBlockEntityRenderer(BlockEntityRendererFactory.Context ctx)
	{
		blockRenderManager = ctx.getRenderManager();
	}
	
	public void render(SwingingBladeBlockEntity entity, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, int overlay)
	{
		final BlockPos tilePos = entity.getPos();
		final BlockState tileState = entity.getCachedState();
		Direction face = tileState.get(SwingingBladeBlock.FACING);
		Direction.Axis axis = tileState.get(SwingingBladeBlock.AXIS);
		
		matrices.push();
			// Set up base rotations before accounting for swing state
			setupInitialRotations(matrices, face, axis);
			
			// FIXME Apply swing state rotation
			
			// Actually render the blade model
			renderBlade((ClientWorld)entity.getWorld(), tileState, tilePos, tickDelta, matrices, vertexConsumers, light, overlay);
		matrices.pop();
	}
	
	private void setupInitialRotations(MatrixStack matrices, Direction face, Direction.Axis axis)
	{
		matrices.translate(0.5D, 0.5D, 0.5D);
		switch(face)
		{
			case DOWN:
				matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(180F));
			case UP:
				if(axis == Direction.Axis.X)
					matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(90F));
				break;
			case NORTH:
				if(axis != Direction.Axis.Y)
					matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(90F));
				
				matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180F));
				matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(90F));
				break;
			case EAST:
				matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(90F));
				if(axis != Direction.Axis.Y)
					matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(90F));
				matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(90F));
				break;
			case SOUTH:
				if(axis != Direction.Axis.Y)
					matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(90F));
				
				matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(90F));
				break;
			case WEST:
				matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(270F));
				if(axis != Direction.Axis.Y)
					matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(90F));
				matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(90F));
				break;
		}
		matrices.translate(-0.5D, -0.5D, -0.5D);
	}
	
	private void renderBlade(ClientWorld world, BlockState state, BlockPos tilePos, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, int overlay)
	{
		matrices.push();
			blockRenderManager.getModelRenderer().render(
					world, 
					blockRenderManager.getModel(BLADE_STATE), 
					state, 
					tilePos, 
					matrices, 
					vertexConsumers.getBuffer(RenderLayer.getSolid()), 
					false, 
					Random.create(), 
					0L, 
					overlay);
		matrices.pop();
	}
}

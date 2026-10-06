package com.lying.client.particle;

import org.jetbrains.annotations.Nullable;

import com.lying.reference.Reference;

import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleFactory;
import net.minecraft.client.particle.ParticleTextureSheet;
import net.minecraft.client.particle.SpriteBillboardParticle;
import net.minecraft.client.particle.SpriteProvider;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.SimpleParticleType;
import net.minecraft.util.math.random.Random;

public class MistParticle extends SpriteBillboardParticle
{
	public MistParticle(ClientWorld clientWorld, double posX, double posY, double posZ, double velX, double velY, double velZ)
	{
		super(clientWorld, posX, posY, posZ);
		Random rand = clientWorld.getRandom();
		this.velocityY = -0.01D;
		this.velocityX = Math.clamp(0.08D * (rand.nextDouble() - 0.5D) / 0.5D, -0.01D, 0.01D);
		this.velocityZ = Math.clamp(0.08D * (rand.nextDouble() - 0.5D) / 0.5D, -0.01D, 0.01D);
		this.maxAge = Reference.Values.TICKS_PER_SECOND * rand.nextBetween(1, 3);
		this.scale = 0.6F;
	}
	
	public ParticleTextureSheet getType()
	{
		return ParticleTextureSheet.PARTICLE_SHEET_TRANSLUCENT;
	}
	
	public void tick()
	{
		this.prevPosX = this.x;
		this.prevPosY = this.y;
		this.prevPosZ = this.z;
		if (this.age++ >= this.maxAge)
			this.markDead();
		else
		{
			this.velocityY = this.velocityY - 0.04 * (double)this.gravityStrength;
			this.move(this.velocityX, this.velocityY, this.velocityZ);
			if (this.ascending && this.y == this.prevPosY) {
				this.velocityX *= 1.1;
				this.velocityZ *= 1.1;
			}
			
			this.velocityX = this.velocityX * (double)this.velocityMultiplier;
			this.velocityY = this.velocityY * (double)this.velocityMultiplier;
			this.velocityZ = this.velocityZ * (double)this.velocityMultiplier;
			this.velocityY = this.onGround ? 0 : Math.max(this.velocityY, -0.01D);
		}
		
		float time = (float)this.age / (float)this.maxAge;
		this.alpha = 1F - (float)Math.pow(time, 6D);
	}
	
	public static class Factory implements ParticleFactory<SimpleParticleType>
	{
		private final SpriteProvider spriteProvider;
		
		public Factory(SpriteProvider spriteProvider)
		{
			this.spriteProvider = spriteProvider;
		}
		
		@Nullable
		public Particle createParticle(SimpleParticleType particleType, ClientWorld clientWorld, double x, double y, double z, double velX, double velY, double velZ)
		{
			MistParticle particle = new MistParticle(clientWorld, x, y, z, velX, velY, velZ);
			particle.setSprite(spriteProvider);
			return particle;
		}
	}
}

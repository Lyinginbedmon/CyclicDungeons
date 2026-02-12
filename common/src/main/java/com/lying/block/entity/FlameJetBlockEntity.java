package com.lying.block.entity;

import java.util.Map;

import com.lying.block.FlameJetBlock;
import com.lying.block.IWireableBlock.WireRecipient;
import com.lying.init.CDBlockEntityTypes;
import com.lying.reference.Reference;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;

public class FlameJetBlockEntity extends TrapActorBlockEntity
{
	private static final Map<Direction.Axis, Vec3d[]> JETS_BY_AXIS = Map.of(
				Direction.Axis.Y, new Vec3d[] {
						new Vec3d(-1D, 0D, -1D),
						new Vec3d(+1D, 0D, -1D),
						new Vec3d(+0D, 0D, +0D),
						new Vec3d(-1D, 0D, +1D),
						new Vec3d(+1D, 0D, +1D)
				},
				Direction.Axis.X, new Vec3d[] {
						new Vec3d(0D, -1D, -1D),
						new Vec3d(0D, +1D, -1D),
						new Vec3d(0D, +0D, +0D),
						new Vec3d(0D, -1D, +1D),
						new Vec3d(0D, +1D, +1D)
				},
				Direction.Axis.Z, new Vec3d[] {
						new Vec3d(-1D, -1D, 0D),
						new Vec3d(+1D, -1D, 0D),
						new Vec3d(+0D, +0D, 0D),
						new Vec3d(-1D, +1D, 0D),
						new Vec3d(+1D, +1D, 0D)
				}
			);
	private static final double JET_OFFSET = 0.2D;
	private static final double PARTICLE_SPEED = 0.175D / 3D;
	private static final BlockState FIRE = Blocks.FIRE.getDefaultState();
	
	private int range = 3;
	private int tickCount = 0;
	private boolean prevPower = false;
	
	public FlameJetBlockEntity(BlockPos pos, BlockState state)
	{
		super(CDBlockEntityTypes.FLAME_JET.get(), pos, state);
	}
	
	protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup)
	{
		super.writeNbt(nbt, registryLookup);
		nbt.putInt("Range", range());
	}
	
	protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup)
	{
		super.readNbt(nbt, registryLookup);
		setRange(nbt.getInt("Range"));
	}
	
	public boolean processWireConnection(BlockPos pos, WireRecipient type)
	{
		if(type != WireRecipient.ACTOR)
			addWire(pos, type);
		return type != WireRecipient.ACTOR;
	}
	
	public static <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type)
	{
		return type != CDBlockEntityTypes.FLAME_JET.get() ? 
				null : 
				FlameJetBlock.validateTicker(type, CDBlockEntityTypes.FLAME_JET.get(), 
					world.isClient() ? 
						FlameJetBlockEntity::tickClient : 
						FlameJetBlockEntity::tickServer);
	}
	
	public static <T extends BlockEntity> void tickClient(World world, BlockPos pos, BlockState state, FlameJetBlockEntity tile)
	{
		TrapActorBlockEntity.tickClient(world, pos, state, tile);
		
		Random rand = world.getRandom();
		Direction facing = state.get(FlameJetBlock.FACING);
		boolean power = state.get(FlameJetBlock.POWERED);
		if(power)
		{
			if(!tile.prevPower)
				world.playSoundAtBlockCenter(pos, SoundEvents.ITEM_FIRECHARGE_USE, SoundCategory.BLOCKS, 1, 0.75F + rand.nextFloat() * 0.2F, true);
			else if(rand.nextInt(Reference.Values.TICKS_PER_SECOND) == 0)
				world.playSoundAtBlockCenter(pos, SoundEvents.BLOCK_FIRE_AMBIENT, SoundCategory.BLOCKS, 1, 0.5F + rand.nextFloat() * 0.5F, true);
			
			// Direction of flame from the jet
			Vec3d vel = new Vec3d(facing.getOffsetX(), facing.getOffsetY(), facing.getOffsetZ());
			// Central jet is positioned at the centre of the face opposite the jet's direction
			Vec3d posBase = new Vec3d(pos.getX(), pos.getY(), pos.getZ()).add(0.5D).subtract(vel.multiply(0.5D));
			// Velocity is adjusted to confine particles to the proximity of the jet
			int range = tile.effectiveRange();
			vel = vel.multiply(PARTICLE_SPEED * range);
			
			// Spawn one particle for every 3 blocks of useful range, this helps preserve overall density
			for(int i=0; i<Math.max(1, Math.floorDiv(range, 3)); i++)
			{
				// Position is offset to a random predefined position to represent a separate distinct jet of flame
				Vec3d posVec = posBase.add(JETS_BY_AXIS.get(facing.getAxis())[rand.nextInt(5)].multiply(JET_OFFSET));
				world.addParticle(ParticleTypes.FLAME, posVec.getX(), posVec.getY(), posVec.getZ(), vel.getX(), vel.getY(), vel.getZ());
			}
		}
		else if(tile.prevPower)
		{
			// Direction of flame from the jet
			Vec3d vel = new Vec3d(facing.getOffsetX(), facing.getOffsetY(), facing.getOffsetZ());
			// Central jet is positioned at the centre of the face opposite the jet's direction
			Vec3d posBase = new Vec3d(pos.getX(), pos.getY(), pos.getZ()).add(0.5D).subtract(vel.multiply(0.5D));
			vel = vel.multiply(0.1D);
			world.addParticle(ParticleTypes.LARGE_SMOKE, posBase.getX(), posBase.getY(), posBase.getZ(), vel.getX(), vel.getY(), vel.getZ());
		}
		
		tile.prevPower = power;
	}
	
	public static <T extends BlockEntity> void tickServer(World world, BlockPos pos, BlockState state, FlameJetBlockEntity tile)
	{
		TrapActorBlockEntity.tickServer(world, pos, state, tile);
		
		if(state.get(FlameJetBlock.POWERED))
		{
			Direction facing = state.get(FlameJetBlock.FACING);
			for(int i=0; i<tile.effectiveRange(); i++)
			{
				BlockPos point = pos.offset(facing, i);
				
				// Periodically fill affected spaces with fire
				if(tile.tickCount++%Reference.Values.TICKS_PER_SECOND == 0 && i > 0 && FIRE.canPlaceAt(world, point))
					world.setBlockState(point, FIRE, 3);
				
				// Ignited & damage creatures caught in the affected area
				world.getEntitiesByClass(LivingEntity.class, Box.enclosing(point, point), EntityPredicates.EXCEPT_CREATIVE_OR_SPECTATOR).forEach(tile::hitEntity);
			}
		}
		else
			tile.tickCount = 0;
	}
	
	public int range() { return this.range; }
	
	public void setRange(int range)
	{
		this.range = MathHelper.clamp(range, 1, 16);
	}
	
	/** Returns the amount of its range this jet can actually use before being blocked */
	public int effectiveRange()
	{
		Direction facing = getCachedState().get(FlameJetBlock.FACING);
		
		for(int i=0; i<range(); i++)
		{
			BlockPos point = pos.offset(facing, i);
			
			// End loop early if we reach a block the flame cannot pass through
			BlockState stateAt = world.getBlockState(point);
			if(Block.isFaceFullSquare(stateAt.getCollisionShape(world, point), facing))
				return i - 1;
		}
		
		return range();
	}
	
	protected void hitEntity(LivingEntity entity)
	{
		entity.damage((ServerWorld)entity.getWorld(), entity.getWorld().getDamageSources().inFire(), 6F);
		entity.setFireTicks(Reference.Values.TICKS_PER_SECOND * (1 + entity.getRandom().nextInt(4)));
	}
}

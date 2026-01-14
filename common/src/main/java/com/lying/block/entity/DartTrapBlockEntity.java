package com.lying.block.entity;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.google.common.collect.Lists;
import com.lying.block.IWireableBlock;
import com.lying.init.CDBlockEntityTypes;
import com.lying.reference.Reference;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Position;
import net.minecraft.world.World;

public class DartTrapBlockEntity extends TrapActorBlockEntity
{
	protected List<StatusEffectInstance> effects = Lists.newArrayList(
			new StatusEffectInstance(StatusEffects.POISON, Reference.Values.TICKS_PER_SECOND * 15, 0)
			);
	
	public DartTrapBlockEntity(BlockPos pos, BlockState state)
	{
		super(CDBlockEntityTypes.DART_TRAP.get(), pos, state);
	}
	
	protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup)
	{
		super.writeNbt(nbt, registryLookup);
		
		if(!effects.isEmpty())
		{
			NbtList effectList = new NbtList();
			effects.forEach(effect -> effectList.add(effect.writeNbt()));
			nbt.put("Effects", effectList);
		}
	}
	
	protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup)
	{
		super.readNbt(nbt, registryLookup);
		
		effects.clear();
		if(nbt.contains("Effects", NbtElement.LIST_TYPE))
		{
			NbtList effectList = nbt.getList("Effects", NbtElement.COMPOUND_TYPE);
			for(NbtElement effect : effectList)
			{
				StatusEffectInstance inst = StatusEffectInstance.fromNbt((NbtCompound)effect);
				if(inst != null)
					effects.add(inst);
			}
		}
	}
	
	@SuppressWarnings("unchecked")
	@Nullable
	public static <E extends BlockEntity, A extends BlockEntity> BlockEntityTicker<A> validateTicker(BlockEntityType<A> given, BlockEntityType<E> expected, BlockEntityTicker<? super E> ticker)
	{
		return expected == given ? (BlockEntityTicker<A>)ticker : null;
	}
	
	public static <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type)
	{
		return type != CDBlockEntityTypes.DART_TRAP.get() ? 
				null : 
				IWireableBlock.validateTicker(type, CDBlockEntityTypes.DART_TRAP.get(), 
					world.isClient() ? 
						TrapActorBlockEntity::tickClient : 
						TrapActorBlockEntity::tickServer);
	}
	
	public ProjectileEntity createDart(ServerWorld world, Position position, Direction direction)
	{
		ArrowEntity dart = new ArrowEntity(world, position.getX(), position.getY(), position.getZ(), new ItemStack(Items.ARROW), null);
		dart.pickupType = PersistentProjectileEntity.PickupPermission.CREATIVE_ONLY;
		effects.forEach(dart::addEffect);
		return dart;
	}
}

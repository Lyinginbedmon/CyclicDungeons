package com.lying.block.entity;

import java.util.Optional;

import org.jetbrains.annotations.Nullable;

import com.lying.block.EncounterSpawnerBlock;
import com.lying.grammar.RoomMetadata;
import com.lying.grammar.content.battle.Battle;
import com.lying.init.CDBlockEntityTypes;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.nbt.NbtOps;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;

public class EncounterSpawnerBlockEntity extends BlockEntity
{
	private Optional<Battle> encounter = Optional.empty();
	private BlockPos min, max;
	
	public EncounterSpawnerBlockEntity(BlockPos pos, BlockState state)
	{
		super(CDBlockEntityTypes.ENCOUNTER.get(), pos, state);
	}
	
	public void setRoomArea(BlockPos min, BlockPos max)
	{
		this.min = min;
		this.max = max;
	}
	
	public void setBattle(Battle battleIn)
	{
		this.encounter = Optional.of(battleIn);
	}
	
	protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup)
	{
		super.writeNbt(nbt, registryLookup);
		nbt.put("RoomMin", NbtHelper.fromBlockPos(min));
		nbt.put("RoomMax", NbtHelper.fromBlockPos(max));
		encounter.ifPresent(b -> nbt.put("Encounter", Battle.CODEC.encodeStart(NbtOps.INSTANCE, b).getOrThrow()));
	}
	
	protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup)
	{
		super.readNbt(nbt, registryLookup);
		NbtHelper.toBlockPos(nbt, "RoomMin").ifPresent(p -> min = p);
		NbtHelper.toBlockPos(nbt, "RoomMax").ifPresent(p -> max = p);
		encounter = Optional.empty();
		if(nbt.contains("Encounter"))
		{
			Battle battle = Battle.CODEC.parse(NbtOps.INSTANCE, nbt.get("Encounter")).getOrThrow();
			if(battle != null)
				encounter = Optional.of(battle);
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
		return type != CDBlockEntityTypes.ENCOUNTER.get() ? 
				null : 
				EncounterSpawnerBlock.validateTicker(type, CDBlockEntityTypes.ENCOUNTER.get(),
					world.isClient() ? null : EncounterSpawnerBlockEntity::tickServer);
	}
	
	public static <T extends BlockEntity> void tickServer(World world, BlockPos pos, BlockState state, EncounterSpawnerBlockEntity tile)
	{
		final BlockPos min = tile.min, max = tile.max;
		Box bounds = Box.enclosing(min, max);
		if(world.getEntitiesByClass(PlayerEntity.class, bounds, EntityPredicates.EXCEPT_CREATIVE_OR_SPECTATOR).isEmpty())
			return;
		
		tile.encounter.ifPresent(battle -> battle.apply(min, max, (ServerWorld)world, new RoomMetadata()));
		world.breakBlock(pos, false);
	}
}

package com.lying.block.entity;

import java.util.Optional;
import java.util.UUID;

import org.jetbrains.annotations.Nullable;

import com.lying.block.EncounterSpawnerBlock;
import com.lying.event.RoomTriggerEvent;
import com.lying.grammar.RoomMetadata;
import com.lying.grammar.content.entities.ActiveEncounterUtility;
import com.lying.grammar.content.entities.SpawnerEntry;
import com.lying.init.CDBattleEntries;
import com.lying.init.CDBlockEntityTypes;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;

public class EncounterSpawnerBlockEntity extends BlockEntity implements IRoomTaggedBlock
{
	private Optional<UUID> roomOrigin = Optional.empty();
	private Optional<Identifier> encounterId = Optional.empty();
	private BlockPos min, max;
	
	public EncounterSpawnerBlockEntity(BlockPos pos, BlockState state)
	{
		super(CDBlockEntityTypes.ENCOUNTER.get(), pos, state);
	}
	
	public void setRoom(UUID room)
	{
		roomOrigin = room == null ? Optional.empty() : Optional.of(room);
	}
	
	@Nullable
	public UUID getRoom() { return roomOrigin.orElse(null); }
	
	public void setRoomArea(BlockPos min, BlockPos max)
	{
		this.min = min;
		this.max = max;
	}
	
	public void setBattle(Identifier battleIn)
	{
		this.encounterId = Optional.of(battleIn);
	}
	
	protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup)
	{
		super.writeNbt(nbt, registryLookup);
		roomOrigin.ifPresent(p -> nbt.putUuid("Room", p));
		nbt.put("RoomMin", NbtHelper.fromBlockPos(min));
		nbt.put("RoomMax", NbtHelper.fromBlockPos(max));
		encounterId.ifPresent(b -> nbt.putString("Encounter", b.toString()));
	}
	
	protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup)
	{
		super.readNbt(nbt, registryLookup);
		roomOrigin = nbt.contains("Room") ? Optional.of(nbt.getUuid("Room")) : Optional.empty();
		NbtHelper.toBlockPos(nbt, "RoomMin").ifPresent(p -> min = p);
		NbtHelper.toBlockPos(nbt, "RoomMax").ifPresent(p -> max = p);
		encounterId = nbt.contains("Encounter") ? Optional.of(Identifier.of(nbt.getString("Encounter"))) : Optional.empty();
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
		if(!world.getEntitiesByClass(PlayerEntity.class, bounds, EntityPredicates.EXCEPT_CREATIVE_OR_SPECTATOR).isEmpty())
			tile.encounterTick(min, max, new RoomMetadata(), world.getRandom());
	}
	
	protected void encounterTick(BlockPos min, BlockPos max, RoomMetadata meta, Random rand)
	{
		final UUID roomId = getRoom();
		if(roomId == null)
		{
			world.breakBlock(pos, false);
			return;
		}
		
		final ActiveEncounterUtility utility = ActiveEncounterUtility.getActiveEncounterUtility(((ServerWorld)getWorld()).getServer());
		if(utility == null)
			return;
		
		// Initialise encounter
		if(!getCachedState().get(EncounterSpawnerBlock.ACTIVE))
			startEncounter(roomId, min, max, meta, utility, rand);
		else if(utility.retrieveForRoom(roomId).isEmpty())
		{
			/*
			 * Query ActiveEncounterUtility with room UUID
			 * If no such encounter, complete room & break
			 */
			
			roomOrigin.ifPresent(r -> RoomTriggerEvent.EVENT.invoker().onTriggered(r, RoomTriggerEvent.FINISH));
			world.breakBlock(pos, false);
		}
		// Monitor mob deaths until all ids nonexistent
	}
	
	protected void startEncounter(final UUID roomId, BlockPos min, BlockPos max, RoomMetadata meta, final ActiveEncounterUtility utility, Random rand)
	{
		world.setBlockState(getPos(), getCachedState().with(EncounterSpawnerBlock.ACTIVE, true), 3);
		
		roomOrigin.ifPresent(r -> RoomTriggerEvent.EVENT.invoker().onTriggered(r, RoomTriggerEvent.START));
		
		// Spawn mobs and pass the resulting UUID list to the ActiveEncounterUtility for monitoring
		encounterId.ifPresent(id -> 
		{
			final SpawnerEntry battle = CDBattleEntries.instance().get(id).orElse(null);
			if(battle != null)
				battle.beginBattle(min, max, (ServerWorld)getWorld(), meta, roomId, rand);
		});
	}
}

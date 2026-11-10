package com.lying.grammar.content.entities;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import com.lying.reference.Reference;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import dev.architectury.event.EventResult;
import net.minecraft.datafixer.DataFixTypes;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtOps;
import net.minecraft.registry.RegistryWrapper.WrapperLookup;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Uuids;
import net.minecraft.world.PersistentState;
import net.minecraft.world.PersistentStateManager;
import net.minecraft.world.World;

public class ActiveEncounterUtility extends PersistentState
{
	private static final String ENCOUNTERS	= "active_encounters";
	public static final PersistentState.Type<ActiveEncounterUtility> TYPE = new PersistentState.Type<ActiveEncounterUtility>(ActiveEncounterUtility::new, (nbt, registries) -> createFromNbt(nbt), DataFixTypes.LEVEL);
	public static final Codec<ActiveEncounterUtility> CODEC	= ActiveEncounter.CODEC.listOf().comapFlatMap(l -> 
	{
		ActiveEncounterUtility utility = new ActiveEncounterUtility();
		l.forEach(utility::add);
		return DataResult.success(utility);
	},
	ActiveEncounterUtility::records);
	
	private final Map<UUID,ActiveEncounter> map = new HashMap<>();
	
	public static ActiveEncounterUtility getActiveEncounterUtility(MinecraftServer server)
	{
		ServerWorld world = server.getWorld(World.OVERWORLD);
		PersistentStateManager manager = world.getPersistentStateManager();
		return manager.getOrCreate(ActiveEncounterUtility.TYPE, String.join("_", Reference.ModInfo.MOD_ID, ENCOUNTERS));
	}
	
	public static ActiveEncounterUtility createFromNbt(NbtCompound nbt)
	{
		return CODEC.parse(NbtOps.INSTANCE, nbt.get("Records")).getOrThrow();
	}
	
	public NbtCompound writeNbt(NbtCompound nbt, WrapperLookup registries)
	{
		nbt.put("Records", CODEC.encodeStart(NbtOps.INSTANCE, this).getOrThrow());
		return nbt;
	}
	
	public List<ActiveEncounter> records()
	{
		List<ActiveEncounter> set = new ArrayList<>();
		set.addAll(map.values());
		return set;
	}
	
	public Optional<ActiveEncounter> retrieveForRoom(UUID roomID)
	{
		return Optional.empty();
	}
	
	public void add(ActiveEncounter record)
	{
		map.put(record.roomId(), record);
		markDirty();
	}
	
	/** Notifies the ActiveEncounterUtility of non-player entity deaths */
	public static EventResult logMobDeath(LivingEntity living, DamageSource source)
	{
		if(!living.isPlayer())
			getActiveEncounterUtility(living.getServer()).logMobDeath(living.getUuid());
		return EventResult.pass();
	}
	
	protected void logMobDeath(UUID id)
	{
		List<UUID> finished = new ArrayList<>();
		for(ActiveEncounter active : map.values())
			if(active.contains(id) && active.remove(id).isEmpty())
				finished.add(active.roomId());
		finished.forEach(map::remove);
	}
	
	public static class ActiveEncounter
	{
		private static final Codec<ActiveEncounter> CODEC	= RecordCodecBuilder.create(instance -> instance.group(
				Uuids.INT_STREAM_CODEC.fieldOf("RoomId").forGetter(ActiveEncounter::roomId),
				Uuids.INT_STREAM_CODEC.listOf().fieldOf("MobIds").forGetter(ActiveEncounter::participantIds)
				).apply(instance, (room,mobs) -> new ActiveEncounter(room,mobs.toArray(new UUID[0]))));
		
		private final UUID roomId;
		private List<UUID> mobIds = new ArrayList<>();
		
		public ActiveEncounter(final UUID roomIdIn, UUID... mobIdsIn)
		{
			this.roomId = roomIdIn;
			for(UUID id : mobIdsIn)
				this.mobIds.add(id);
		}
		
		public UUID roomId() { return this.roomId; }
		
		public List<UUID> participantIds() { return this.mobIds; }
		
		public boolean isEmpty() { return mobIds.isEmpty(); }
		
		public boolean contains(UUID id) { return mobIds.contains(id); }
		
		public ActiveEncounter remove(UUID id) { mobIds.remove(id); return this; }
	}
}

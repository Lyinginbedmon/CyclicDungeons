package com.lying.block.entity;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.google.common.collect.Lists;
import com.lying.block.IWireableBlock;
import com.lying.block.IWireableBlock.WireRecipient;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.registry.RegistryWrapper.WrapperLookup;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public abstract class AbstractWireableBlockEntity extends BlockEntity
{
	private WiringManifest wiring = new WiringManifest();
	
	protected AbstractWireableBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state)
	{
		super(type, pos, state);
	}
	
	protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup)
	{
		super.writeNbt(nbt, registryLookup);
		
		// Blank NBT isn't read when the block entity updates, so we ensure there's always SOME data
		nbt.putBoolean("IsActive", IWireableBlock.getWireable(getPos(), getWorld()).isActive(getPos(), getWorld()));
		
		if(!wiring.isEmpty())
			nbt.put("Wires", wiring.toNbt());
	}
	
	protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup)
	{
		super.readNbt(nbt, registryLookup);
		
		wiring.clear();
		if(nbt.contains("Wires", NbtElement.COMPOUND_TYPE))
			wiring = WiringManifest.fromNbt(nbt.get("Wires"));
	}
	
	public final List<BlockPos> getSensors() { return wiring.getAll(WireRecipient.SENSOR); }
	public final List<BlockPos> getActors() { return wiring.getAll(WireRecipient.ACTOR); }
	
	public final boolean hasSensors() { return !getSensors().isEmpty(); }
	public final boolean hasActors() { return !getActors().isEmpty(); }
	
	public final int wireCount() { return wiring.size(); }
	
	public abstract boolean processWireConnection(BlockPos pos, WireRecipient type);
	
	public void reset()
	{
		// Deactivate any attached actors
		getActors().forEach(p -> IWireableBlock.getWireable(p, world).deactivate(p, world));
		
		// Clear connections
		wiring.clear();
		
		// Deactivate self
		IWireableBlock wireable = IWireableBlock.getWireable(getPos(), getWorld());
		if(wireable.isActive(getPos(), getWorld()))
			wireable.deactivate(getPos(), getWorld());
		
		markDirty();
	}
	
	protected void cleanActors()
	{
		if(wiring.clean(WireRecipient.ACTOR, getWorld()))
			markDirty();
	}
	
	protected void cleanSensors()
	{
		if(wiring.clean(WireRecipient.SENSOR, getWorld()))
			markDirty();
	}
	
	protected final void addWire(BlockPos pos, WireRecipient type)
	{
		switch(type)
		{
			case ACTOR:
			case SENSOR:
				if(wiring.add(pos, type))
					markDirty();
				break;
			case LOGIC:
			default:
				break;
		}
	}
	
	public BlockEntityUpdateS2CPacket toUpdatePacket() { return BlockEntityUpdateS2CPacket.create(this); }
	
	public NbtCompound toInitialChunkDataNbt(WrapperLookup registries) { return createNbt(registries); }
	
	public void markDirty()
	{
		if(world != null)
			world.updateListeners(getPos(), getCachedState(), getCachedState(), 3);
	}
	
	public static class WiringManifest
	{
		private static final Codec<WiringManifest> CODEC	= RecordCodecBuilder.create(instance -> instance.group(
			BlockPos.CODEC.listOf().optionalFieldOf("Sensors").forGetter(m -> m.tryGetAll(WireRecipient.SENSOR)),
			BlockPos.CODEC.listOf().optionalFieldOf("Actors").forGetter(m -> m.tryGetAll(WireRecipient.ACTOR)),
			BlockPos.CODEC.listOf().optionalFieldOf("Logic").forGetter(m -> m.tryGetAll(WireRecipient.LOGIC))
				).apply(instance, (s,a,l) -> 
				new WiringManifest(s.orElse(Lists.newArrayList()), a.orElse(Lists.newArrayList()), l.orElse(Lists.newArrayList()))));
		
		private Map<WireRecipient, List<BlockPos>> targets = new HashMap<>();
		
		public WiringManifest() { }
		
		public WiringManifest(List<BlockPos> sensors, List<BlockPos> actors, List<BlockPos> logic)
		{
			this();
			targets.put(WireRecipient.SENSOR, sensors);
			targets.put(WireRecipient.ACTOR, actors);
			targets.put(WireRecipient.LOGIC, logic);
		}
		
		public boolean isEmpty() { return targets.isEmpty() || targets.values().stream().allMatch(l -> l.isEmpty()); }
		
		public int size()
		{
			int tally = 0;
			for(List<BlockPos> points : targets.values())
				tally += points.size();
			return tally;
		}
		
		protected Optional<List<BlockPos>> tryGetAll(WireRecipient type)
		{
			List<BlockPos> values;
			return !targets.containsKey(type) ? Optional.empty() : (values = getAll(type)).isEmpty() ? Optional.empty() : Optional.of(values);
		}
		
		public List<BlockPos> getAll(WireRecipient type) { return targets.getOrDefault(type, Lists.newArrayList()); }
		
		public boolean add(BlockPos pos, WireRecipient type)
		{
			List<BlockPos> set = getAll(type);
			if(set.stream().anyMatch(p -> p.getManhattanDistance(pos) == 0))
				return false;
			
			set.add(pos);
			targets.put(type, set);
			return true;
		}
		
		public void clear()
		{
			targets.clear();
		}
		
		public boolean clean(WireRecipient type, World world)
		{
			List<BlockPos> points = Lists.newArrayList();
			points.addAll(getAll(type));
			if(points.isEmpty())
				return false;
			
			if(points.removeIf(pos -> 
			{
				BlockState sensorState = world.getBlockState(pos);
				return !(sensorState.getBlock() instanceof IWireableBlock) || IWireableBlock.getWireable(pos, world).type() != type;
			}))
			{
				targets.put(type, points);
				return true;
			};
			return false;
		}
		
		public NbtElement toNbt()
		{
			return CODEC.encodeStart(NbtOps.INSTANCE, this).getOrThrow();
		}
		
		public static WiringManifest fromNbt(NbtElement ele)
		{
			return CODEC.parse(NbtOps.INSTANCE, ele).getOrThrow();
		}
	}
}

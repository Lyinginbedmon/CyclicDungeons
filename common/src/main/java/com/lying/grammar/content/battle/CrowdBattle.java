package com.lying.grammar.content.battle;

import java.util.List;
import java.util.Optional;

import com.google.common.collect.Lists;
import com.lying.CyclicDungeons;
import com.lying.grammar.RoomMetadata;
import com.lying.reference.Reference;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtOps;
import net.minecraft.registry.Registries;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;

/** Spawns a group of all the same mob */
public class CrowdBattle<T extends Entity> extends Battle
{
	public static final Identifier ID	= Reference.ModInfo.prefix("crowd");
	private CrowdData data = null;
	
	public CrowdBattle(Identifier registryNameIn)
	{
		this(registryNameIn, null);
	}
	
	protected CrowdBattle(Identifier nameIn, CrowdData dataIn)
	{
		super(nameIn);
		data = dataIn;
	}
	
	public static <J extends Entity> CrowdBattle<J> of(EntityType<J> typeIn)
	{
		return of(typeIn, 1);
	}
	
	public static <J extends Entity> CrowdBattle<J> of(EntityType<J> typeIn, int count)
	{
		return new CrowdBattle<>(ID, CrowdData.of(typeIn, count));
	}
	
	public static <J extends Entity> CrowdBattle<J> of(EntityType<J> typeIn, int min, int max)
	{
		return new CrowdBattle<>(ID, CrowdData.of(typeIn, min, max));
	}
	
	public Text describe() { return ((MutableText)data.range()).append(" ").append(data.type().getUntranslatedName()); }
	
	@SuppressWarnings("unchecked")
	public void apply(BlockPos min, BlockPos max, ServerWorld world, RoomMetadata meta)
	{
		if(data == null)
		{
			CyclicDungeons.LOGGER.error("# Error loading crowd battle data");
			return;
		}
		final EntityType<T> type = (EntityType<T>)data.type();
		if(type == null)
		{
			CyclicDungeons.LOGGER.error("# Error loading crowd battle entity type");
			return;
		}
		
		Random rand = world.random;
		final int mobs = data.getCount(rand);
		if(mobs <= 0)
			return;
		
		List<BlockPos> positions = Lists.newArrayList();
		BlockPos.Mutable.iterate(min, max).forEach(p -> 
		{
			if(canPlaceAt(p, world, type))
				positions.add(p.toImmutable());
		});
		
		BlockPos pos;
		if(!positions.isEmpty())
			for(int i=mobs; i>0; --i)
				do
				{
					pos = positions.size() == 1 ? positions.remove(0) : positions.remove(rand.nextInt(positions.size()));
				}
				while(!trySpawn(type, pos, world) && !positions.isEmpty());
	}
	
	public NbtCompound writeConfig(NbtCompound nbt)
	{
		return (NbtCompound)CrowdData.CODEC.encodeStart(NbtOps.INSTANCE, data).getOrThrow();
	}
	
	public Battle readConfig(NbtCompound nbt)
	{
		return new CrowdBattle<>(ID, CrowdData.CODEC.parse(NbtOps.INSTANCE, nbt).getOrThrow());
	}
	
	private record CrowdData(EntityType<? extends Entity> type, int count, int min, int max)
	{
		public static final Codec<CrowdData> CODEC	= RecordCodecBuilder.create(instance -> instance.group(
				Registries.ENTITY_TYPE.getCodec().fieldOf("type").forGetter(CrowdData::type),
				Codec.INT.optionalFieldOf("count").forGetter(c -> c.count < 0 ? Optional.empty() : Optional.of(c.count)),
				Codec.INT.optionalFieldOf("min").forGetter(c -> c.min < 0 ? Optional.empty() : Optional.of(c.min)),
				Codec.INT.optionalFieldOf("max").forGetter(c -> c.max < 0 ? Optional.empty() : Optional.of(c.max))
				).apply(instance, (t,c,mi,ma) -> new CrowdData(t, c.orElse(-1), mi.orElse(-1), ma.orElse(-1))));
		
		public static CrowdData of(EntityType<? extends Entity> type, int count)
		{
			return new CrowdData(type, count, -1, -1);
		}
		
		public static CrowdData of(EntityType<? extends Entity> type, int min, int max)
		{
			return new CrowdData(type, -1, min, max);
		}
		
		public int getCount(Random rand)
		{
			if(count >= 0)
				return count;
			else if(min >= 0 && max >= 0)
			{
				if(min == max)
					return min;
				else
				{
					int a = Math.min(min, max);
					int b = Math.max(min, max);
					return rand.nextBetween(a, b);
				}
			}
			else
				return 1;
		}
		
		public Text range() { return count > 0 ? Text.literal(String.valueOf(count)) : Text.literal(String.valueOf(min)).append(Text.literal("-")).append(Text.literal(String.valueOf(max))); }
	}
}
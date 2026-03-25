package com.lying.grammar.content.trap;

import java.util.Iterator;
import java.util.List;

import com.google.common.collect.Lists;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.lying.block.IWireableBlock.WireRecipient;
import com.lying.block.entity.TrapLogicBlockEntity;
import com.lying.data.CDTags;
import com.lying.grammar.RoomMetadata;
import com.lying.init.CDBlockEntityTypes;
import com.lying.init.CDBlocks;
import com.lying.init.CDTrapLogicHandlers;
import com.lying.item.WiringGunItem.WireMode;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.block.BlockState;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

public abstract class LogicControlledTrap extends Trap
{
	private LogicManifest logicManifest = new LogicManifest(CDTrapLogicHandlers.ID_RELAY);
	
	protected LogicControlledTrap(Identifier nameIn)
	{
		super(nameIn);
	}
	
	public JsonObject toJson(JsonObject obj, JsonOps ops)
	{
		obj.add("Wiring", logicManifest.toJson());
		return obj;
	}
	
	public Trap fromJson(JsonOps ops, JsonObject obj)
	{
		logicManifest = LogicManifest.fromJson(obj.get("Wiring"));
		return this;
	}
	
	public void apply(BlockPos min, BlockPos max, ServerWorld world, RoomMetadata meta)
	{
		// Place any post-generation sensor blocks
		installSensors(min, max, world, meta);
		
		// Place trap logic block
		installLogic(min, max, world);
	}
	
	protected abstract void installSensors(BlockPos min, BlockPos max, ServerWorld world, RoomMetadata meta);
	
	protected void installLogic(BlockPos min, BlockPos max, ServerWorld world)
	{
		BlockPos logicPos = null;
		Iterator<BlockPos> iterator = BlockPos.Mutable.iterate(min, max.withY(min.getY())).iterator();
		while(iterator.hasNext())
		{
			BlockPos pos = iterator.next();
			BlockState state = world.getBlockState(pos);
			if(state.isOpaqueFullCube())
			{
				logicPos = pos;
				break;
			}
		}
		if(logicPos == null)
			logicPos = min;
		
		world.setBlockState(logicPos, CDBlocks.TRAP_LOGIC.get().getDefaultState());
		final TrapLogicBlockEntity logic = world.getBlockEntity(logicPos, CDBlockEntityTypes.TRAP_LOGIC.get()).get();
		logic.setLogic(logicManifest.handlerID());
		
		List<BlockPos> sensors = Lists.newArrayList(), actors = Lists.newArrayList();
		BlockPos.Mutable.iterate(min, max).forEach(p -> 
		{
			BlockPos pos = p.toImmutable();
			BlockState state = world.getBlockState(pos);
			if(state.isIn(CDTags.TRAP_ACTOR))
				actors.add(pos);
			else if(state.isIn(CDTags.TRAP_SENSOR))
				sensors.add(pos);
		});
		sensors.forEach(p -> logic.processWireConnection(p, WireMode.GLOBAL, WireRecipient.SENSOR));
		actors.forEach(p -> logic.processWireConnection(p, WireMode.GLOBAL, WireRecipient.ACTOR));
	}
	
	private static record LogicManifest(Identifier handlerID)
	{
		public static final Codec<LogicManifest> CODEC	= RecordCodecBuilder.create(instance -> instance.group(
				Identifier.CODEC.fieldOf("Logic").forGetter(LogicManifest::handlerID)
				).apply(instance, LogicManifest::new));
		
		public JsonElement toJson() { return CODEC.encodeStart(JsonOps.INSTANCE, this).getOrThrow(); }
		
		public static LogicManifest fromJson(JsonElement ele) { return CODEC.parse(JsonOps.INSTANCE, ele).getOrThrow(); }
	}
}

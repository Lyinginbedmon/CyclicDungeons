package com.lying.grammar.content.trap.modular;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;

import org.jetbrains.annotations.Nullable;

import com.google.common.base.Predicates;
import com.google.common.collect.Lists;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.lying.block.IWireableBlock;
import com.lying.block.Port;
import com.lying.block.entity.logic.PortEntry;
import com.lying.init.CDLogicGates;
import com.lying.item.WiringGunItem.WireMode;
import com.lying.utility.CDUtils;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.block.BlockState;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.StringIdentifiable;
import net.minecraft.util.math.BlockPos;

public abstract class ModuleWiring
{
	public static final Codec<ModuleWiring> CODEC	=	Codec.of(ModuleWiring::encode, ModuleWiring::decode);
	
	@SuppressWarnings("unchecked")
	private static <T> DataResult<T> encode(final ModuleWiring wiring, final DynamicOps<T> ops, final T prefix)
	{
		if(ops != JsonOps.INSTANCE)
			return DataResult.error(() -> "Storing module wiring as NBT is not supported");
		
		return (DataResult<T>)DataResult.success(wiring.toJson(JsonOps.INSTANCE));
	}
	
	private static <T> DataResult<Pair<ModuleWiring, T>> decode(final DynamicOps<T> ops, final T input)
	{
		if(ops != JsonOps.INSTANCE)
			return DataResult.error(() -> "Loading trap entry from NBT is not supported");
		
		ModuleWiring entry = fromJson(JsonOps.INSTANCE, (JsonObject)input);
		return entry == null ? DataResult.error(() -> "Error loading module wiring from JSON") : DataResult.success(Pair.of(entry, input));
	}
	
	public abstract boolean isEmpty();
	
	/** Returns true if the given module is a wiring target of the owner of this wiring object */
	public abstract boolean isWiringTarget(Module module);
	
	public final void applyWiring(BlockPos modulePos, Map<Identifier, List<BlockPos>> componentMap, ServerWorld world)
	{
		BlockState state = world.getBlockState(modulePos);
		if(!(state.getBlock() instanceof IWireableBlock))
			return;
		
		wire(modulePos, (IWireableBlock)state.getBlock(), componentMap, world);
	}
	
	protected abstract void wire(BlockPos modulePos, final IWireableBlock wireable, Map<Identifier, List<BlockPos>> componentMap, ServerWorld world);
	
	public final JsonObject toJson(JsonOps ops)
	{
		JsonObject obj = new JsonObject();
		obj.add("style", WiringStyle.CODEC.encodeStart(ops, style()).getOrThrow());
		obj.add("data", write(ops));
		return obj;
	}
	
	@Nullable
	public static ModuleWiring fromJson(JsonOps ops, JsonObject obj)
	{
		final WiringStyle type = WiringStyle.CODEC.parse(ops, obj.get("style")).getOrThrow();
		return type == null ? null : type.parser.apply(ops, obj.get("data"));
	}
	
	protected abstract WiringStyle style();
	
	protected abstract JsonElement write(JsonOps ops);
	
	public static enum WiringStyle implements StringIdentifiable
	{
		SIMPLE(Simple::fromJson),
		COMPLEX(Complex::fromJson);
		
		public static final Codec<WiringStyle> CODEC = StringIdentifiable.createCodec(WiringStyle::values);
		
		public final BiFunction<JsonOps, JsonElement, ModuleWiring> parser;
		
		private WiringStyle(BiFunction<JsonOps, JsonElement, ModuleWiring> parserIn)
		{
			parser = parserIn;
		}
		
		public String asString() { return name().toLowerCase(); }
		
		@Nullable
		public static WiringStyle fromString(String nameIn)
		{
			for(WiringStyle style : values())
				if(style.asString().equalsIgnoreCase(nameIn))
					return style;
			return null;
		}
	}
	
	/** List of blocks providing output port to target module's input port */
	public static class Simple extends ModuleWiring
	{
		public static final Codec<Simple> CODEC	= Identifier.CODEC.listOf().comapFlatMap(set -> DataResult.success(Simple.of(set)), obj -> obj.inputs);
		private List<Identifier> inputs = Lists.newArrayList();
		
		public static Simple of(List<Identifier> setIn)
		{
			Simple wiring = new Simple();
			wiring.inputs.addAll(setIn);
			return wiring;
		}
		
		protected WiringStyle style() { return WiringStyle.SIMPLE; }
		
		protected JsonElement write(JsonOps ops)
		{
			return CODEC.encodeStart(ops, this).getOrThrow();
		}
		
		public static Simple fromJson(JsonOps ops, JsonElement ele)
		{
			return CODEC.parse(ops, ele).getOrThrow();
		}
		
		public boolean isEmpty() { return inputs.isEmpty(); }
		
		public boolean isWiringTarget(Module module)
		{
			return inputs.contains(module.name());
		}
		
		public void wire(BlockPos modulePos, final IWireableBlock wireable, Map<Identifier, List<BlockPos>> componentMap, ServerWorld world)
		{
			final Consumer<BlockPos> wireTo = p -> wireable.acceptWireFrom(CDLogicGates.INPUT, modulePos, WireMode.GLOBAL, new PortEntry(p, CDLogicGates.OUTPUT), world);
			inputs.forEach(id -> componentMap.getOrDefault(id, Lists.newArrayList()).forEach(wireTo::accept));
		}
	}
	
	/** Complex map of input ports to output ports of other blocks */
	public static class Complex extends ModuleWiring
	{
		public static final Codec<Complex> CODEC	= Wire.LIST_CODEC.comapFlatMap(set -> 
		{
			Complex obj = new Complex();
			set.forEach(obj::addWire);
			return DataResult.success(obj);
		}, obj -> obj.scheme.entrySet().stream().map(e -> new Wire(e.getKey(), e.getValue())).toList());
		private Map<Port, List<Output>> scheme = new HashMap<>();
		private List<Identifier> targets = Lists.newArrayList();
		
		protected void addWire(Wire wire)
		{
			final Port input = wire.input();
			for(Output output : wire.transmitters())
				attach(input, output);
		}
		
		public static Complex create() { return new Complex(); }
		
		protected WiringStyle style() { return WiringStyle.COMPLEX; }
		
		public boolean isEmpty() { return scheme.isEmpty() || targets.isEmpty(); }
		
		protected JsonElement write(JsonOps ops)
		{
			return CODEC.encodeStart(ops, this).getOrThrow();
		}
		
		public static Complex fromJson(JsonOps ops, JsonElement ele)
		{
			return CODEC.parse(ops, ele).getOrThrow();
		}
		
		public Complex attach(Port input, Output... output)
		{
			List<Output> set = scheme.getOrDefault(input, Lists.newArrayList());
			for(Output var : output)
			{
				if(!set.isEmpty() && set.removeIf(output::equals))
					tallyTargets();
				set.add(var);
				recordTargets(var.parts());
			}
			scheme.put(input, set);
			return this;
		}
		
		protected void tallyTargets()
		{
			targets.clear();
			for(List<Output> entry : scheme.values())
				entry.stream()
					.map(Output::parts)
					.forEach(this::recordTargets);
		}
		
		protected void recordTargets(List<Identifier> set)
		{
			set.stream()
				.filter(Predicates.not(targets::contains))
				.forEach(targets::add);
		}
		
		public boolean isWiringTarget(Module module)
		{
			return targets.contains(module.name());
		}
		
		public void wire(BlockPos modulePos, final IWireableBlock wireable, Map<Identifier, List<BlockPos>> componentMap, ServerWorld world)
		{
			for(Entry<Port, List<Output>> wire : scheme.entrySet())
			{
				final Port input = wire.getKey();
				final BiConsumer<Port, BlockPos> wireTo = (p,b) -> wireable.acceptWireFrom(input, modulePos, WireMode.GLOBAL, new PortEntry(b, p), world);
				for(Output set : wire.getValue())
				{
					final Port output = set.port();
					for(Identifier part : set.parts())
						componentMap.getOrDefault(part, List.of()).forEach(b -> wireTo.accept(output, b));
				}
			}
		}
		
		private static record Wire(Port input, List<Output> transmitters)
		{
			public static final Codec<Wire> CODEC	= RecordCodecBuilder.create(instance -> instance.group(
					Port.CODEC.fieldOf("input").forGetter(Wire::input),
					Output.CODEC.listOf().optionalFieldOf("outputs").forGetter(w -> CDUtils.listOrSolo(Optional.of(w.transmitters)).getLeft()),
					Output.CODEC.optionalFieldOf("output").forGetter(w -> CDUtils.listOrSolo(Optional.of(w.transmitters)).getRight()))
					.apply(instance, (port,outs,out) -> 
					{
						List<Output> outputs = outs.isPresent() ? outs.get() : out.isPresent() ? List.of(out.get()) : List.of();
						return new Wire(port, outputs);
					}));
			public static final Codec<List<Wire>> LIST_CODEC	= CODEC.listOf();
		}
		
		public static record Output(Port port, List<Identifier> parts)
		{
			public static final Codec<Output> CODEC	= RecordCodecBuilder.create(instance -> instance.group(
					Port.CODEC.fieldOf("port").forGetter(Output::port),
					Identifier.CODEC.listOf().optionalFieldOf("modules").forGetter(o -> CDUtils.listOrSolo(Optional.of(o.parts)).getLeft()),
					Identifier.CODEC.optionalFieldOf("module").forGetter(o -> CDUtils.listOrSolo(Optional.of(o.parts)).getRight()))
					.apply(instance, (port,mods,mod) -> 
					{
						List<Identifier> parts = mods.isPresent() ? mods.get() : mod.isPresent() ? List.of(mod.get()) : List.of();
						return new Output(port, parts);
					}));
			
			public static Output of(Port port, Identifier... partsIn)
			{
				List<Identifier> set = Lists.newArrayList();
				for(Identifier part : partsIn)
					if(!set.contains(part))
						set.add(part);
				return new Output(port, set);
			}
			
			public boolean equals(Object obj) { return obj instanceof Output && ((Output)obj).port().equals(port); }
		}
	}
}

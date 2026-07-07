package com.lying.grammar.content.trap;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.common.collect.Lists;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.lying.blueprint.BlueprintRoom;
import com.lying.grammar.RoomMetadata;
import com.lying.grammar.content.trap.modular.Module;
import com.lying.grammar.content.trap.modular.ModuleWiring;
import com.lying.init.CDTrapTypes;
import com.lying.reference.Reference;
import com.lying.worldgen.theme.Theme;
import com.mojang.serialization.JsonOps;

import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;

public class ModularTrap extends Trap
{
	public static final Identifier ID	= Reference.ModInfo.prefix("modular");
	
	protected List<Module> modules = Lists.newArrayList();
	
	public ModularTrap(Identifier name)
	{
		super(name);
	}
	
	public static ModularTrap create() { return (ModularTrap)CDTrapTypes.get(ID).get(); }
	
	public ModularTrap module(Module moduleIn)
	{
		modules.removeIf(m -> m.name().equals(moduleIn.name()));
		modules.add(moduleIn);
		return this;
	}
	
	public JsonObject toJson(JsonObject obj, JsonOps ops)
	{
		JsonArray set = new JsonArray();
		modules.forEach(module -> set.add(Module.CODEC.encodeStart(ops, module).getOrThrow()));
		obj.add("Modules", set);
		return obj;
	}
	
	public Trap fromJson(JsonOps ops, JsonObject obj)
	{
		JsonArray set = obj.getAsJsonArray("Modules");
		for(int i=0; i<set.size(); i++)
			module(Module.CODEC.parse(ops, set.get(i)).getOrThrow());
		return this;
	}
	
	public boolean isApplicableTo(BlueprintRoom room, RoomMetadata meta, Theme theme)
	{
		return super.isApplicableTo(room, meta, theme) && !modules.isEmpty() && modules.stream().map(Module::name).distinct().count() == modules.size();
	}
	
	public void apply(BlockPos min, BlockPos max, ServerWorld world, RoomMetadata meta, Random rand)
	{
		// Sort modules by necessary order of placement
		modules.sort(Module.BUILD_ORDER);
		
		// Identify viable positions within room for all components
		Map<Identifier, List<BlockPos>> viabilityMap = new HashMap<>();
		BlockPos.Mutable.iterate(min, max).forEach(p -> 
			modules.stream()
				.filter(m -> m.canExistAt(p, world))
				.map(Module::name)
				.forEach(m -> 
				{
					List<BlockPos> set = viabilityMap.getOrDefault(m, Lists.newArrayList());
					set.add(p.toImmutable());
					viabilityMap.put(m, set);
				})
		);
		
		// Build map of all components to be placed and where
		Map<Identifier, List<BlockPos>> componentMap = new HashMap<>();
		List<BlockPos> allComponents = Lists.newArrayList();
		for(Module component : modules)
		{
			final Identifier name = component.name();
			List<BlockPos> viable = viabilityMap.getOrDefault(name, Lists.newArrayList());
			
			viable.removeAll(allComponents);
			
			// Remove any positions that don't relational needs
			viable.removeIf(p -> !component.inValidRelations(p, componentMap));
			
			// Abort generation if a wiring component cannot be placed
			if(viable.isEmpty() && (component.isVitalComponent() || component.isWiringElement(modules)))
				return;
			
			List<BlockPos> set = Lists.newArrayList();
			for(int i=0; i<component.count(); i++)
			{
				if(viable.isEmpty())
					break;
				
				BlockPos pos = viable.size() == 1 ? viable.removeFirst() : viable.remove(rand.nextInt(viable.size()));
				set.add(pos);
				allComponents.add(pos);
			}
			componentMap.put(name, set);
		}
		
		// Place all components in the world
		for(Module component : modules)
		{
			final Identifier name = component.name();
			List<BlockPos> points = componentMap.get(name);
			for(int i=0; i<component.count(); i++)
			{
				if(i >= points.size())
					break;
				
				component.placeAt(points.get(i), world);
			}
		}
		
		// Conduct wiring of wireable blocks
		for(Module component : modules.stream().filter(Module::needsWiring).toList())
		{
			final List<BlockPos> instances = componentMap.get(component.name());
			final ModuleWiring wiringObj = component.inputs().get();
			instances.forEach(i -> wiringObj.applyWiring(i, componentMap, world));
		}
	}
}

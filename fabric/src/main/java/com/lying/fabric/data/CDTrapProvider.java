package com.lying.fabric.data;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import com.google.common.collect.Lists;
import com.lying.grammar.content.TrapRoomContent.TrapEntry;
import com.lying.grammar.content.trap.DefaultTraps;
import com.lying.reference.Reference;
import com.mojang.serialization.JsonOps;

import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.minecraft.data.DataOutput.OutputType;
import net.minecraft.data.DataOutput.PathResolver;
import net.minecraft.data.DataProvider;
import net.minecraft.data.DataWriter;
import net.minecraft.registry.RegistryWrapper.WrapperLookup;

public class CDTrapProvider implements DataProvider
{
	private final PathResolver path;
	private final CompletableFuture<WrapperLookup> wrapperLookup;
	
	public CDTrapProvider(FabricDataOutput generator, CompletableFuture<WrapperLookup> managerIn)
	{
		this.path = generator.getResolver(OutputType.DATA_PACK, "content/traps/");
		this.wrapperLookup = managerIn;
	}
	
	public CompletableFuture<?> run(DataWriter dataWriter)
	{
		return wrapperLookup.thenCompose(lookup -> {
			List<CompletableFuture<?>> futures = Lists.newArrayList();
			DefaultTraps.getAll().forEach(trap ->
				futures.add(DataProvider.writeToPath(dataWriter, TrapEntry.CODEC.encodeStart(JsonOps.INSTANCE, trap).getOrThrow(), this.path.resolveJson(trap.registryName()))));
			return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
		});
	}
	
	public String getName() { return Reference.ModInfo.MOD_NAME+" default trap entries"; }
}

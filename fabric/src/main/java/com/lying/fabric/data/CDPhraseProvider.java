package com.lying.fabric.data;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import com.google.common.collect.Lists;
import com.lying.reference.Reference;
import com.lying.worldgen.theme.DefaultPhrases;
import com.lying.worldgen.theme.InitialPhrase;
import com.mojang.serialization.JsonOps;

import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.minecraft.data.DataOutput.OutputType;
import net.minecraft.data.DataOutput.PathResolver;
import net.minecraft.data.DataProvider;
import net.minecraft.data.DataWriter;
import net.minecraft.registry.RegistryWrapper.WrapperLookup;

public class CDPhraseProvider implements DataProvider
{
	private final PathResolver path;
	private final CompletableFuture<WrapperLookup> wrapperLookup;
	
	public CDPhraseProvider(FabricDataOutput generator, CompletableFuture<WrapperLookup> managerIn)
	{
		this.path = generator.getResolver(OutputType.DATA_PACK, "grammar/phrases/");
		this.wrapperLookup = managerIn;
	}
	
	public CompletableFuture<?> run(DataWriter dataWriter)
	{
		return wrapperLookup.thenCompose(lookup -> {
			List<CompletableFuture<?>> futures = Lists.newArrayList();
			DefaultPhrases.getDefaults().forEach(phrase ->
				futures.add(DataProvider.writeToPath(dataWriter, InitialPhrase.CODEC.encodeStart(JsonOps.INSTANCE, phrase).getOrThrow(), this.path.resolveJson(phrase.registryName()))));
			return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
		});
	}
	
	public String getName() { return Reference.ModInfo.MOD_NAME+" default phrases"; }
}

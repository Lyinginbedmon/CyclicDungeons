package com.lying.fabric.data;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import com.google.common.collect.Lists;
import com.lying.grammar.content.battle.BattleEntry;
import com.lying.grammar.content.battle.DefaultBattles;
import com.lying.reference.Reference;
import com.mojang.serialization.JsonOps;

import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.minecraft.data.DataOutput.OutputType;
import net.minecraft.data.DataOutput.PathResolver;
import net.minecraft.data.DataProvider;
import net.minecraft.data.DataWriter;
import net.minecraft.registry.RegistryWrapper.WrapperLookup;

public class CDBattleProvider implements DataProvider
{
	private final PathResolver path;
	private final CompletableFuture<WrapperLookup> wrapperLookup;
	
	public CDBattleProvider(FabricDataOutput generator, CompletableFuture<WrapperLookup> managerIn)
	{
		this.path = generator.getResolver(OutputType.DATA_PACK, "encounters/");
		this.wrapperLookup = managerIn;
	}
	
	public CompletableFuture<?> run(DataWriter dataWriter)
	{
		return wrapperLookup.thenCompose(lookup -> {
			List<CompletableFuture<?>> futures = Lists.newArrayList();
			DefaultBattles.getAll().forEach(battle ->
				futures.add(DataProvider.writeToPath(dataWriter, BattleEntry.CODEC.encodeStart(JsonOps.INSTANCE, battle).getOrThrow(), this.path.resolveJson(battle.registryName()))));
			return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
		});
	}
	
	public String getName() { return Reference.ModInfo.MOD_NAME+" default encounter entries"; }
}

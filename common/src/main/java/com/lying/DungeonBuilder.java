package com.lying;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import com.google.common.collect.Lists;
import com.lying.blueprint.Blueprint;
import com.lying.grammar.CDGrammar;
import com.lying.grammar.GrammarPhrase;
import com.lying.grammar.RoomMetadata;
import com.lying.graph.GraphOrganiser;
import com.lying.graph.GraphScruncher;
import com.lying.init.CDLoggers;
import com.lying.init.CDThemes;
import com.lying.worldgen.theme.Theme;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;

public class DungeonBuilder
{
	public static final Logger LOGGER = CyclicDungeons.LOGGER;
	private static final DungeonBuilder INSTANCE	= new DungeonBuilder();
	private static final GraphOrganiser ORGANISER = GraphOrganiser.Poisson.create();
	private static final ExecutorService THREAD = new ThreadPoolExecutor(0, 16, 60L, TimeUnit.SECONDS, new LinkedBlockingDeque<>());
	private static List<BlueprintGeneration> SPOOL = Lists.newArrayList();
	
	protected DungeonBuilder() { }
	
	public static DungeonBuilder instance() { return INSTANCE; }
	
	public boolean generate(BlockPos position, ServerWorld world, Random rand)
	{
		final long startTime = System.currentTimeMillis();
		final Theme theme = CDThemes.instance().get(CDThemes.ID_GENERIC).get();
		final GrammarPhrase phrase = CDGrammar.initialPhrase(15, rand);	// FIXME Resolve stock grammar phrase passage issues
		if(phrase == null)
		{
			LOGGER.error(" ! Tried to generate a dungeon w/out providing an initial phrase");
			return false;
		}
		
		LOGGER.info("Starting dungeon calculation at {} in {}", position, world);
		SPOOL.add(new BlueprintGeneration(THREAD.submit(() -> 
		{
			CDGrammar.generate(phrase, rand);
			if(phrase == null || phrase.isEmpty())
			{
				LOGGER.error(" # Grammar generation failed");
				return Optional.empty();
			}
			long time;
			LOGGER.info(" # Grammar generation complete in {} ms, {} rooms across {} depths", (time = System.currentTimeMillis()) - startTime, phrase.size(), phrase.depth());
			
			Blueprint blueprint = Blueprint.fromGraph(phrase);
			blueprint.forEach(room -> 
			{
				final RoomMetadata meta = room.metadata();
				meta.type().prepare(meta, rand);
			});
			CDLoggers.GRAMMAR.info(" # Room sizes selected");
			
			int attempts = 50;
			do
			{
				CDLoggers.PLANAR.info(" - Attempt {} at organising graph", attempts);
				ORGANISER.organise(blueprint, rand);
			}
			while(blueprint.hasErrors() && attempts-- > 0);
			if(blueprint.hasErrors())
			{
				LOGGER.error(" # Graph organisation failed");
				return Optional.empty();
			}
			LOGGER.info(" # Graph organisation complete in {}ms", System.currentTimeMillis() - time);
			
			time = System.currentTimeMillis();
			GraphScruncher.collapse(blueprint);
			if(blueprint.hasErrors())
			{
				LOGGER.error(" # Passage optimisation failed");
				return Optional.empty();
			}
			LOGGER.info(" # Passage optimisation complete in {}ms", System.currentTimeMillis() - time);
			return Optional.of(blueprint);
		}), position, world, theme, rand, startTime));
		
		return true;
	}
	
	public static void onServerStart(MinecraftServer server)
	{
		SPOOL.clear();
	}
	
	public static void onServerTick(ServerWorld event)
	{
		SPOOL.stream().filter(BlueprintGeneration::isFailed).forEach(r -> LOGGER.error(" # Dungeon calculation failed at {}", r.position()));
		SPOOL.removeIf(BlueprintGeneration::isFailed);
		
		SPOOL.forEach(gen -> 
		{
			if(gen.isFinished())
			{
				final long time = System.currentTimeMillis();
				if(gen.build())
					LOGGER.info(" # Dungeon generation at {} complete in {}ms, {}ms total", gen.position(), System.currentTimeMillis() - time, System.currentTimeMillis() - gen.startTime);
				else
					LOGGER.error(" # Dungeon generation failed at {}", gen.position());
			}
		});
		SPOOL.removeIf(BlueprintGeneration::isFinished);
	}
	
	private record BlueprintGeneration(Future<Optional<Blueprint>> future, BlockPos position, ServerWorld world, Theme theme, Random rand, long startTime)
	{
		public boolean isFinished() { return future.isDone(); }
		
		public boolean isFailed()
		{
			return future.isCancelled() || future.isDone() && blueprint() == null;
		}
		
		@NotNull
		protected Optional<Blueprint> blueprint()
		{
			if(!future.isDone())
				return Optional.empty();
			Optional<Blueprint> blueprint = null;
			try
			{
				blueprint = future.get();
			}
			catch(Exception e) { }
			return blueprint;
		}
		
		public boolean build()
		{
			Optional<Blueprint> blueprint = blueprint();
			return blueprint.isPresent() && blueprint.get().build(position, world, rand);
		}
	}
}

package com.lying;

import org.slf4j.Logger;

import com.lying.blueprint.Blueprint;
import com.lying.blueprint.BlueprintOrganiser;
import com.lying.blueprint.BlueprintRoom;
import com.lying.blueprint.BlueprintScruncher;
import com.lying.grammar.CDGrammar;
import com.lying.grammar.GrammarPhrase;

import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;

public class DungeonBuilder
{
	public static final Logger LOGGER = CyclicDungeons.LOGGER;
	private static final DungeonBuilder INSTANCE	= new DungeonBuilder();
	private static final BlueprintOrganiser ORGANISER = BlueprintOrganiser.Poisson.create();
	
	private Random rand = Random.create();
	private GrammarPhrase phrase = null;
	
	protected DungeonBuilder() { }
	
	public static DungeonBuilder instance() { return INSTANCE; }
	
	public DungeonBuilder setPhrase(GrammarPhrase phraseIn)
	{
		phrase = phraseIn;
		return this;
	}
	
	public DungeonBuilder setPhrase(int size)
	{
		phrase = CDGrammar.initialPhrase(size, rand);
		return this;
	}
	
	public DungeonBuilder setRandom(Random randIn)
	{
		rand = randIn;
		return this;
	}
	
	public boolean generate(BlockPos position, ServerWorld world)
	{
		if(phrase == null)
		{
			LOGGER.error(" ! Tried to generate a dungeon w/out providing an initial phrase");
			return false;
		}
		
		LOGGER.info("Starting dungeon generation at {} in {}", position, world);
		final long startTime = System.currentTimeMillis();
		CDGrammar.generate(phrase, rand);
		if(phrase == null || phrase.isEmpty())
		{
			LOGGER.error(" # Grammar generation failed");
			return false;
		}
		long time;
		LOGGER.info(" # Grammar generation complete in {} ms, {} rooms across {} depths", (time = System.currentTimeMillis()) - startTime, phrase.size(), phrase.depth());
		
		Blueprint blueprint = Blueprint.fromGraph(phrase);
		blueprint.stream()
			.map(BlueprintRoom::metadata)
			.forEach(meta -> meta.type().prepare(meta, rand));
		
		int attempts = 50;
		do
		{
			ORGANISER.organise(blueprint, rand);
		}
		while(blueprint.hasErrors() && attempts-- > 0);
		if(blueprint.hasErrors())
		{
			LOGGER.error(" # Graph organisation failed");
			return false;
		}
		LOGGER.info(" # Graph organisation complete in {}ms", System.currentTimeMillis() - time);
		
		time = System.currentTimeMillis();
		BlueprintScruncher.collapse(blueprint);
		if(blueprint.hasErrors())
		{
			LOGGER.error(" # Passage optimisation failed");
			return false;
		}
		LOGGER.info(" # Passage optimisation complete in {}ms", System.currentTimeMillis() - time);
		
		time = System.currentTimeMillis();
		if(!blueprint.build(position, world, rand))
		{
			LOGGER.error(" # Dungeon generation failed");
			return false;
		}
		LOGGER.info(" # Dungeon generation complete in {}ms, {}ms total", System.currentTimeMillis() - time, System.currentTimeMillis() - startTime);
		return true;
	}
}

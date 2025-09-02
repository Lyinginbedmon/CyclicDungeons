package com.lying;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.lying.command.CDCommands;
import com.lying.config.ServerConfig;
import com.lying.grammar.CDGrammar;
import com.lying.init.CDBlocks;
import com.lying.init.CDEntityTypes;
import com.lying.init.CDItems;
import com.lying.init.CDParticleTypes;
import com.lying.init.CDScreenHandlerTypes;
import com.lying.init.CDSoundEvents;
import com.lying.init.CDTerms;
import com.lying.network.CDPacketHandler;
import com.lying.reference.Reference;

public final class CyclicDungeons
{
	/*
	 * Step 1: Sentence grammar
	 * Start -> Blank -> Blank -> Blank -> Exit
	 * Graph rewriting
	 * 
	 * Step 2: 2D space arrangement
	 * Organising sentence into world space
	 * 
	 * Step 3: Building/construction in-world
	 * Generation
	 * 
	 * Step 4: Content
	 * Stuff!
	 */
	
	public static Logger LOGGER = LoggerFactory.getLogger(Reference.ModInfo.MOD_ID);
	
	public static ServerConfig config;
	
	public static void init()
	{
		config = new ServerConfig("config/CyclicDungeonsServer.cfg");
		config.read();
		
		CDTerms.init();
		CDCommands.init();
		CDBlocks.init();
		CDEntityTypes.init();
		CDItems.init();
		CDSoundEvents.init();
		CDParticleTypes.init();
		CDScreenHandlerTypes.init();
		CDPacketHandler.initServer();
		registerServerEvents();
		
		CDGrammar.run();
	}
	
	private static void registerServerEvents()
	{
		
	}
}

package com.lying.config;

import java.io.FileWriter;
import java.util.Properties;
import java.util.function.BiFunction;

import org.slf4j.Logger;

import com.lying.CyclicDungeons;
import com.lying.init.CDLoggers;
import com.lying.init.CDLoggers.LogType;
import com.lying.utility.logging.DebugLogger;
import com.lying.worldgen.tile.Tile;

import net.minecraft.util.StringIdentifiable;
import net.minecraft.util.math.BlockPos;

public class ServerConfig extends Config
{
	private static final Properties DEFAULT_SETTINGS = new Properties();
	
	public static ShellType shellGenerationType = ShellType.LABYRINTH;
	
	public ServerConfig(String fileIn)
	{
		super(fileIn);
	}
	
	protected Properties getDefaults() { return DEFAULT_SETTINGS; }
	
	protected void readValues(Properties valuesIn)
	{
		Logger log = CyclicDungeons.LOGGER;
		log.info("Loading server config");
		
		shellGenerationType = ShellType.fromString(valuesIn.getProperty("ShellType", ShellType.MINIMUM.asString()));
		log.info(" # Dungeon exterior set to {}", shellGenerationType.asString());
		
		log.info(" # Console logs: ");
		for(LogType l : LogType.values())
		{
			DebugLogger log2 = CDLoggers.getLogger(l);
			if(log2 == null)
				continue;
			
			final boolean active = parseBoolOr(valuesIn.getProperty(l.asString()), false);
			log2.setActive(active);
			log.info(" ## {}: {}", l.asString(), active);
		}
	}
	
	protected void writeValues(FileWriter writer)
	{
		Config.writeString(writer, "ShellType", shellGenerationType.asString());
		for(LogType log : LogType.values())
			Config.writeBool(writer, log.asString(), CDLoggers.getLogger(log).isActive());
	}
	
	static
	{
		DEFAULT_SETTINGS.put("ShellType", ShellType.MINIMUM.asString());
		for(LogType log : LogType.values())
			DEFAULT_SETTINGS.put(log.asString(), "0");
	}
	
	public static enum ShellType implements StringIdentifiable
	{
		/** Full enclosure around dungeon */
		FULL((a,b) -> b),
		/** Bottom-most tile enclosure only, mainly for debugging */
		MINIMUM((a,b) -> b.withY(a.getY() + Tile.TILE_SIZE)),
		/** Full enclosure sans roof, like an architectural model */
		LABYRINTH((a,b) -> b.down()),
		/** No enclosure at all */
		NONE((a,b) -> a);
		
		public final BiFunction<BlockPos,BlockPos,BlockPos> adjustFunc;
		
		private ShellType(BiFunction<BlockPos,BlockPos,BlockPos> adjustIn)
		{
			adjustFunc = adjustIn;
		}
		
		public BlockPos adjust(BlockPos min, BlockPos max) { return adjustFunc.apply(min, max); }
		
		public String asString() { return name().toLowerCase(); }
		
		public static ShellType fromString(String name)
		{
			for(ShellType val : values())
				if(val.asString().equalsIgnoreCase(name))
					return val;
			return ShellType.FULL;
		}
	}
}

package com.lying.init;

import org.jetbrains.annotations.Nullable;

import com.lying.utility.logging.DebugLogger;

import net.minecraft.util.StringIdentifiable;

/** Centralised debug logger control class */
public class CDLoggers
{
	public static final DebugLogger GRAMMAR		= DebugLogger.of("grammar");
	public static final DebugLogger PLANAR		= DebugLogger.of("planar");
	public static final DebugLogger WFC			= DebugLogger.of("wfc");
	public static final DebugLogger WORLDGEN	= DebugLogger.of("worldgen");
	
	@Nullable
	public static DebugLogger getLogger(Logger logIn)
	{
		switch(logIn)
		{
			case GRAMMAR:
				return GRAMMAR;
			case PLANAR:
				return PLANAR;
			case WFC:
				return WFC;
			case WORLDGEN:
				return WORLDGEN;
		}
		return null;
	}
	
	public static enum Logger implements StringIdentifiable
	{
		GRAMMAR("LogGrammar"),
		PLANAR("LogPlanar"),
		WFC("LogTiles"),
		WORLDGEN("LogWorldgen");
		
		private final String name;
		
		private Logger(String nameIn)
		{
			name = nameIn;
		}
		
		public String asString() { return name; }
		
		public static Logger fromString(String name)
		{
			for(Logger val : values())
				if(val.asString().equalsIgnoreCase(name))
					return val;
			return Logger.GRAMMAR;
		}
	}
}

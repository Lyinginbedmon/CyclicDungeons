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
	public static DebugLogger getLogger(LogType logIn)
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
	
	public static enum LogType implements StringIdentifiable
	{
		GRAMMAR("LogGrammar"),
		PLANAR("LogPlanar"),
		WFC("LogTiles"),
		WORLDGEN("LogWorldgen");
		
		private final String name;
		
		private LogType(String nameIn)
		{
			name = nameIn;
		}
		
		public String asString() { return name; }
		
		public static LogType fromString(String name)
		{
			for(LogType val : values())
				if(val.asString().equalsIgnoreCase(name))
					return val;
			return LogType.GRAMMAR;
		}
	}
}

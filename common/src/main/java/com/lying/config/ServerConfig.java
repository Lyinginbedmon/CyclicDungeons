package com.lying.config;

import java.io.FileWriter;
import java.util.Properties;
import java.util.function.BiFunction;

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
		shellGenerationType = ShellType.fromString(valuesIn.getProperty("ShellType", ShellType.MINIMUM.asString()));
	}
	
	protected void writeValues(FileWriter writer)
	{
		Config.writeString(writer, "ShellType", shellGenerationType.asString());
	}
	
	static
	{
		DEFAULT_SETTINGS.put("ShellType", ShellType.MINIMUM.asString());
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

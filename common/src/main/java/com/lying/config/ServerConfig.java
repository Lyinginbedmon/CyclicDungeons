package com.lying.config;

import java.io.FileWriter;
import java.util.Properties;
import java.util.function.Consumer;
import java.util.function.Function;

import com.lying.worldgen.tile.Tile;

import net.minecraft.util.StringIdentifiable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;

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
		FULL(b -> 
			BlockPos.Mutable.iterate(
					new BlockPos((int)b.minX, (int)b.minY, (int)b.minZ), 
					new BlockPos((int)b.maxX, (int)b.maxY, (int)b.maxZ).add(-1, -1, -1))),
		/** Bottom-most tile enclosure only, mainly for debugging */
		MINIMUM(b -> 
			BlockPos.Mutable.iterate(
					new BlockPos((int)b.minX, (int)b.minY, (int)b.minZ), 
					new BlockPos((int)b.maxX, (int)b.minY + Tile.TILE_SIZE + 1, (int)b.maxZ).add(-1, -1, -1))),
		/** Full enclosure sans roof, like an architectural model */
		LABYRINTH(b ->
			BlockPos.Mutable.iterate(
					new BlockPos((int)b.minX, (int)b.minY, (int)b.minZ), 
					new BlockPos((int)b.maxX, (int)b.maxY, (int)b.maxZ).add(-1, -2, -1))),
		/** No enclosure at all */
		NONE(b -> null);
		
		public final Function<Box,Iterable<BlockPos>> buildFunc;
		
		private ShellType(Function<Box,Iterable<BlockPos>> funcIn)
		{
			buildFunc = funcIn;
		}
		
		public void build(Box box, Consumer<BlockPos> func)
		{
			buildFunc.apply(box).forEach(p -> func.accept(p));
		}
		
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

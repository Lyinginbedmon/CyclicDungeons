package com.lying.config;

import java.io.FileWriter;
import java.util.Properties;

public class ServerConfig extends Config
{
	private static final Properties DEFAULT_SETTINGS = new Properties();
	
	public ServerConfig(String fileIn)
	{
		super(fileIn);
	}
	
	protected Properties getDefaults() { return DEFAULT_SETTINGS; }
	
	protected void readValues(Properties valuesIn)
	{
		
	}
	
	protected void writeValues(FileWriter writer)
	{
		
	}
	
	static
	{
		
	}
}

package com.lying.command;

import static com.lying.reference.Reference.ModInfo.translate;

import com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;

import net.minecraft.text.Text;

@SuppressWarnings("unused")
public class CDCommands
{
	private static final Dynamic2CommandExceptionType TOO_BIG_EXCEPTION = new Dynamic2CommandExceptionType(
			(maxCount, count) -> Text.stringifiedTranslatable("commands.fill.toobig", maxCount, count)
		);
	private static SimpleCommandExceptionType make(String name)
	{
		return new SimpleCommandExceptionType(translate("command", "failed_"+name.toLowerCase()));
	}
	
	public static void init()
	{
		
	}
}

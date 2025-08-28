package com.lying.command;

import static com.lying.reference.Reference.ModInfo.translate;
import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

import java.util.List;

import com.google.common.collect.Lists;
import com.lying.grammar.Grammar;
import com.lying.grammar.Graph;
import com.lying.grammar.Room;
import com.lying.reference.Reference;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;

import dev.architectury.event.events.common.CommandRegistrationEvent;
import net.minecraft.command.argument.NbtCompoundArgumentType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

@SuppressWarnings("unused")
public class CDCommands
{
	private static final SimpleCommandExceptionType PHRASE_PARSE_FAILED_EXCEPTION = make("phrase_parse_failed");
	
	private static SimpleCommandExceptionType make(String name)
	{
		return new SimpleCommandExceptionType(translate("command", name.toLowerCase()));
	}
	
	public static void init()
	{
		CommandRegistrationEvent.EVENT.register((dispatcher, access, environment) -> 
		{
			dispatcher.register(literal(Reference.ModInfo.MOD_ID).requires(source -> source.hasPermissionLevel(2))
				.then(literal("parse")
					.then(argument("phrase", NbtCompoundArgumentType.nbtCompound())
						.executes(context -> tryParsePhrase(NbtCompoundArgumentType.getNbtCompound(context, "phrase"), context.getSource()))))
				.then(literal("generate")
					.then(argument("phrase", NbtCompoundArgumentType.nbtCompound())
						.executes(context -> tryGenerate(NbtCompoundArgumentType.getNbtCompound(context, "phrase"), context.getSource()))))
				);
		});
	}
	
	public static Graph parsePhrase(NbtCompound nbt) throws CommandSyntaxException
	{
		if(nbt.contains("Phrase", NbtElement.LIST_TYPE))
		{
			NbtList phrase = nbt.getList("Phrase", NbtElement.STRING_TYPE);
			if(phrase.isEmpty())
				throw PHRASE_PARSE_FAILED_EXCEPTION.create();
			
			List<String> terms = Lists.newArrayList();
			phrase.forEach(e -> terms.add(e.asString()));
			return Graph.parsePhrase(terms.toArray(new String[0]));
		}
		else
			throw PHRASE_PARSE_FAILED_EXCEPTION.create();
	}
	
	private static int tryParsePhrase(NbtCompound nbt, ServerCommandSource source) throws CommandSyntaxException
	{
		Graph graph = parsePhrase(nbt);
		if(graph.isEmpty())
			throw PHRASE_PARSE_FAILED_EXCEPTION.create();
		source.sendFeedback(() -> Text.literal("Parsed dungeon:"), false);
		graph.printAsTree(t -> source.sendFeedback(() -> t, false), CDCommands::roomToText);
		return graph.size();
	}
	
	private static int tryGenerate(NbtCompound nbt, ServerCommandSource source) throws CommandSyntaxException
	{
		Graph graph = parsePhrase(nbt);
		if(graph.isEmpty())
			throw PHRASE_PARSE_FAILED_EXCEPTION.create();
		source.sendFeedback(() -> Text.literal("Generated dungeon:"), false);
		Grammar.generate(graph).printAsTree(t -> source.sendFeedback(() -> t, false), CDCommands::roomToText);
		return graph.size();
	}
	
	private static Text roomToText(Room r)
	{
		return 
				Text.literal("  ".repeat(r.depth))
				.append(r.name());
	}
}

package com.lying.command;

import static com.lying.reference.Reference.ModInfo.translate;
import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

import java.util.List;

import com.google.common.collect.Lists;
import com.lying.blueprint.Blueprint;
import com.lying.blueprint.BlueprintOrganiser;
import com.lying.blueprint.BlueprintScruncher;
import com.lying.grammar.CDGrammar;
import com.lying.grammar.GrammarPhrase;
import com.lying.grammar.GrammarRoom;
import com.lying.network.ShowDungeonLayoutPacket;
import com.lying.reference.Reference;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;

import dev.architectury.event.events.common.CommandRegistrationEvent;
import net.minecraft.command.argument.BlockPosArgumentType;
import net.minecraft.command.argument.NbtCompoundArgumentType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;

@SuppressWarnings("unused")
public class CDCommands
{
	private static final SimpleCommandExceptionType PHRASE_PARSE_FAILED_EXCEPTION = make("phrase_parse_failed");
	private static final SimpleCommandExceptionType GRAPH_FAILED_EXCEPTION = make("graph_failed");
	private static final SimpleCommandExceptionType SCRUNCH_FAILED_EXCEPTION = make("scrunch_failed");
	private static final SimpleCommandExceptionType GENERATION_FAILED_EXCEPTION = make("generation_failed");
	
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
				.then(literal("preview")
					.then(argument("phrase", NbtCompoundArgumentType.nbtCompound())
						.executes(context -> tryPreview(NbtCompoundArgumentType.getNbtCompound(context, "phrase"), Random.create(), context.getSource()))
						.then(argument("seed", BlockPosArgumentType.blockPos())
							.executes(context -> tryPreview(NbtCompoundArgumentType.getNbtCompound(context, "phrase"), Random.create(getRandSeed(context, "seed")), context.getSource()))))
					.then(argument("size", IntegerArgumentType.integer(1))
						.executes(context -> tryPreview(IntegerArgumentType.getInteger(context, "size"), Random.create(), context.getSource()))
						.then(argument("seed", BlockPosArgumentType.blockPos())
							.executes(context -> tryPreview(IntegerArgumentType.getInteger(context, "size"), Random.create(getRandSeed(context, "seed")), context.getSource())))))
				.then(literal("generate")
					.then(argument("size", IntegerArgumentType.integer(1))
						.then(argument("position", BlockPosArgumentType.blockPos())
							.executes(context -> generateInWorld(IntegerArgumentType.getInteger(context, "size"), BlockPosArgumentType.getBlockPos(context, "position"), context.getSource())))))
				);
		});
	}
	
	public static GrammarPhrase parsePhrase(NbtCompound nbt) throws CommandSyntaxException
	{
		if(nbt.contains("Phrase", NbtElement.LIST_TYPE))
		{
			NbtList phrase = nbt.getList("Phrase", NbtElement.STRING_TYPE);
			if(phrase.isEmpty())
				throw PHRASE_PARSE_FAILED_EXCEPTION.create();
			
			List<String> terms = Lists.newArrayList();
			phrase.forEach(e -> terms.add(e.asString()));
			return GrammarPhrase.parsePhrase(terms.toArray(new String[0]));
		}
		else
			throw PHRASE_PARSE_FAILED_EXCEPTION.create();
	}
	
	private static int tryParsePhrase(NbtCompound nbt, ServerCommandSource source) throws CommandSyntaxException
	{
		GrammarPhrase graph = parsePhrase(nbt);
		if(graph.isEmpty())
			throw PHRASE_PARSE_FAILED_EXCEPTION.create();
		if(source.getPlayer() != null)
			ShowDungeonLayoutPacket.sendTo(source.getPlayer(), graph, false);
		return graph.size();
	}
	
	private static int tryPreview(NbtCompound nbt, Random rand, ServerCommandSource source) throws CommandSyntaxException
	{
		GrammarPhrase graph = parsePhrase(nbt);
		if(graph.isEmpty())
			throw PHRASE_PARSE_FAILED_EXCEPTION.create();
		CDGrammar.generate(graph, rand);
		if(source.getPlayer() != null)
			ShowDungeonLayoutPacket.sendTo(source.getPlayer(), graph, true);
		return graph.size();
	}
	
	private static int tryPreview(int size, Random rand, ServerCommandSource source)
	{
		GrammarPhrase graph = CDGrammar.initialGraph(size, rand);
		CDGrammar.generate(graph, rand);
		if(source.getPlayer() != null)
			ShowDungeonLayoutPacket.sendTo(source.getPlayer(), graph, true);
		return graph.size();
	}
	
	private static Text roomToText(GrammarRoom r)
	{
		return 
				Text.literal("  ".repeat(r.metadata().depth()))
				.append(r.name());
	}
	
	private static int getRandSeed(CommandContext<ServerCommandSource> context, String name)
	{
		BlockPos position = BlockPosArgumentType.getBlockPos(context, name);
		return position.getX() * position.getX() + position.getZ() * position.getZ();
	}
	
	private static int generateInWorld(int size, BlockPos position, ServerCommandSource source) throws CommandSyntaxException
	{
		Random rand = Random.create(position.getX() * position.getX() + position.getZ() * position.getZ());
		GrammarPhrase graph = CDGrammar.initialGraph(size, rand);
		CDGrammar.generate(graph, rand);
		
		Blueprint blueprint = Blueprint.fromGraph(graph);
		blueprint.forEach(node -> node.metadata().setSize(node.metadata().type().size(rand)));
		BlueprintOrganiser.Circular.create().organise(blueprint, rand);
		if(blueprint.hasErrors())
			throw GRAPH_FAILED_EXCEPTION.create();
		
		BlueprintScruncher.collapse(blueprint, false);
//		BlueprintScruncher.collapse(blueprint, true);
		if(blueprint.hasErrors())
			throw SCRUNCH_FAILED_EXCEPTION.create();
		
		ServerWorld world = source.getWorld();
		if(!blueprint.build(position, world))
			throw GENERATION_FAILED_EXCEPTION.create();
		
		return 15;
	}
}

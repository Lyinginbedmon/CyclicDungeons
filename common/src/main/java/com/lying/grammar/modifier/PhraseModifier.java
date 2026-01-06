package com.lying.grammar.modifier;

import static com.lying.reference.Reference.ModInfo.prefix;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

import com.lying.grammar.GrammarPhrase;
import com.lying.grammar.GrammarRoom;
import com.lying.grammar.GrammarTerm;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;

import net.minecraft.util.Identifier;

public abstract class PhraseModifier
{
	// TODO Add support for configurable phrase modifiers
	private static final Map<Identifier, Supplier<PhraseModifier>> REGISTRY = new HashMap<>();
	public static final Codec<PhraseModifier> CODEC = Identifier.CODEC.comapFlatMap(id -> DataResult.success(PhraseModifier.get(id)), PhraseModifier::registryName);
	
	public static final Identifier ID_NOOP	= prefix("none");
	public static final Supplier<PhraseModifier> NOOP	= register(ID_NOOP, id -> new PhraseModifier(id)
	{
		public void apply(GrammarTerm term, GrammarRoom room, GrammarPhrase graph) { }
	});
	public static final Supplier<PhraseModifier> INJECT_BRANCH	= register("inject_branch", InjectBranch::new);
	public static final Supplier<PhraseModifier> INJECT_ROOM	= register("inject_room", InjectRoom::new);
	public static final Supplier<PhraseModifier> INJECT_TREASURE	= register("inject_treasure", InjectRoom.Treasure::new);
	
	private static Supplier<PhraseModifier> register(String nameIn, Function<Identifier,PhraseModifier> factoryIn)
	{
		return register(prefix(nameIn), factoryIn);
	}
	
	public static Supplier<PhraseModifier> register(Identifier idIn, Function<Identifier,PhraseModifier> factoryIn)
	{
		Supplier<PhraseModifier> supplier = () -> factoryIn.apply(idIn);
		REGISTRY.put(idIn, supplier);
		return supplier;
	}
	
	public static PhraseModifier get(Identifier id)
	{
		return REGISTRY.getOrDefault(id, NOOP).get();
	}
	
	private final Identifier id;
	
	protected PhraseModifier(Identifier idIn)
	{
		id = idIn;
	}
	
	public final Identifier registryName() { return id; }
	
	public final boolean isBlank() { return id.equals(ID_NOOP); }
	
	public abstract void apply(GrammarTerm term, GrammarRoom room, GrammarPhrase graph);
	
	public boolean isBranchInjector() { return false; }
}
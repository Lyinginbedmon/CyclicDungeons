package com.lying.grammar;

import static com.lying.reference.Reference.ModInfo.prefix;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import org.joml.Vector2i;

import com.lying.grammar.content.RoomContent;
import com.lying.grammar.modifier.PhraseModifier;
import com.lying.init.CDTerms;

import net.minecraft.util.Identifier;

public class DefaultTerms
{
	private static final Map<Identifier, GrammarTerm> TERMS = new HashMap<>();
	
	public static final Identifier
		ID_BATTLE	= prefix("battle"),
		ID_BOSS		= prefix("boss"),
		ID_TRAP		= prefix("trap"),
		ID_BIG_PUZZLE	= prefix("big_puzzle"),
		ID_SML_PUZZLE	= prefix("small_puzzle");
	
	// Initial building blocks
	public static final GrammarTerm START		= register(CDTerms.ID_START, () -> GrammarTerm.Builder.create(0xFFFFFF)
			.size(6, 8, 6, 8)
			.unplaceable());
	public static final GrammarTerm END		= register(CDTerms.ID_END, () -> GrammarTerm.Builder.create(0xFFFFFF)
			.size(6, 8, 6, 8)
			.unplaceable());
	
	// Functional rooms
	public static final GrammarTerm EMPTY			= register(CDTerms.ID_EMPTY, () -> GrammarTerm.Builder.create(0xA6A6A6)
			.withCondition(TermConditions.create()
				.consecutive(false)
				.allowDeadEnds(false)
				.neverAfter(List.of(CDTerms.ID_START))
				.neverBefore(List.of(CDTerms.ID_END)))
			.size(7, 10, 7, 10));
	public static final GrammarTerm BATTLE		= register(ID_BATTLE, () -> GrammarTerm.Builder.create(0xC80707)
			.withCondition(TermConditions.create()
				.consecutive(false))
			.size(8, 14, 8, 14)
			.setContent(RoomContent.BATTLE.get())
			.weight(3));
	public static final GrammarTerm TRAP			= register(ID_TRAP, () -> GrammarTerm.Builder.create(0xAE31DE)
			.withCondition(TermConditions.create()
				.consecutive(false))
			.size(6, 8, 6, 8)
			.setContent(RoomContent.TRAP.get())
			.weight(2));
	public static final GrammarTerm BIG_PUZZLE	= register(ID_BIG_PUZZLE, () -> GrammarTerm.Builder.create(0x3136DE)
			.withCondition(TermConditions.create()
					.popCap(2)
					.consecutive(false))
			.size(10, 16, 10, 16)
			.onApply(PhraseModifier.INJECT_TREASURE.get()));
	public static final GrammarTerm SML_PUZZLE	= register(ID_SML_PUZZLE, () -> GrammarTerm.Builder.create(0x2768CA)
			.withCondition(TermConditions.create()
				.popCap(4)
				.consecutive(false))
			.size(5, 8, 5, 8));
	public static final GrammarTerm BOSS			= register(ID_BOSS, () -> GrammarTerm.Builder.create(0x7D1D1D)
			.withCondition(TermConditions.create()
				.popCap(1)
				.neverAfter(List.of(CDTerms.ID_START))
				.onlyBefore(List.of(CDTerms.ID_END)))
			.size(new Vector2i(16, 16))
			.weight(10)
			.onApply(PhraseModifier.INJECT_TREASURE.get()));
	public static final GrammarTerm TREASURE		= register(CDTerms.ID_TREASURE, () -> GrammarTerm.Builder.create(0xFFDC40)
			.withCondition(TermConditions.create()
				.popCap(3)
				.onlyAfter(List.of(DefaultTerms.ID_BATTLE, DefaultTerms.ID_SML_PUZZLE, CDTerms.ID_EMPTY)))
			.size(new Vector2i(5, 5))
			.weight(2));
	
	public static GrammarTerm register(Identifier id, Supplier<GrammarTerm.Builder> funcIn)
	{
		GrammarTerm sup = funcIn.get().build(id);
		TERMS.put(id, sup);
		return sup;
	}
	
	public static List<GrammarTerm> getDefaults()
	{
		return TERMS.values().stream().toList();
	}
}

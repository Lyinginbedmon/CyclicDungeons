package com.lying.init;

import static com.lying.reference.Reference.ModInfo.prefix;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import com.lying.CyclicDungeons;
import com.lying.grammar.Graph;
import com.lying.grammar.Room;
import com.lying.grammar.Term;

import net.minecraft.util.Identifier;

@SuppressWarnings("unchecked")
public class CDTerms
{
	private static final Map<Identifier, Supplier<Term>> TERMS = new HashMap<>();
	private static int tally = 0;
	
	// Initial building blocks
	public static final Supplier<Term> START		= register("start", () -> Term.Builder.create().unplaceable());
	public static final Supplier<Term> BLANK		= register("blank", () -> Term.Builder.create().unplaceable().replaceable());
	public static final Supplier<Term> END			= register("end", () -> Term.Builder.create().unplaceable());
	
	/** Completely blank, only used to mark errors in generation */
	public static final Supplier<Term> VOID			= register("void", () -> Term.Builder.create().unplaceable());
	
	// Functional rooms
	public static final Supplier<Term> EMPTY		= register("empty", () -> Term.Builder.create().nonconsecutive().allowDeadEnds(false).neverAfter(CDTerms.START).neverBefore(CDTerms.END));
	public static final Supplier<Term> BATTLE		= register("battle", () -> Term.Builder.create().nonconsecutive());
	public static final Supplier<Term> TRAP			= register("trap", () -> Term.Builder.create().nonconsecutive());
	public static final Supplier<Term> BIG_PUZZLE	= register("big_puzzle", () -> Term.Builder.create().nonconsecutive().popCap(2).neverAfter(CDTerms.SML_PUZZLE).onApply(CDTerms::injectTreasure));
	public static final Supplier<Term> SML_PUZZLE	= register("small_puzzle", () -> Term.Builder.create().nonconsecutive().popCap(4).neverAfter(CDTerms.BIG_PUZZLE).neverBefore(CDTerms.BIG_PUZZLE));
	public static final Supplier<Term> BOSS			= register("boss", () -> Term.Builder.create().popCap(1).onlyBefore(CDTerms.END).onApply(CDTerms::injectTreasure));
	public static final Supplier<Term> TREASURE		= register("treasure", () -> Term.Builder.create().popCap(3).onlyAfter(CDTerms.BATTLE, CDTerms.SML_PUZZLE, CDTerms.EMPTY));
	public static final Supplier<Term> ADD			= register("add_room", () -> Term.Builder.create().replaceable().sizeCap(10).onApply((t,r,g) -> Term.injectRoom(r, g)));
	public static final Supplier<Term> ADD_BRANCH	= register("add_branch", () -> Term.Builder.create().replaceable().sizeCap(10).onApply((t,r,g) -> Term.injectBranch(r, g)));
	
	private static void injectTreasure(Term t, Room r, Graph g)
	{
		Term.injectRoom(r, g).applyTerm(CDTerms.TREASURE.get(), g);
	}
	
	private static Supplier<Term> register(String name, Supplier<Term.Builder> funcIn)
	{
		final Identifier id = prefix(name);
		final Term term = funcIn.get().build(id);
		if(term.isPlaceable())
			tally++;
		Supplier<Term> sup = () -> term;
		TERMS.put(id, sup);
		return sup;
	}
	
	public static Optional<Term> get(String name) { return get(name.contains(":") ? Identifier.of(name) : prefix(name)); }
	
	public static Optional<Term> get(Identifier id) { return TERMS.containsKey(id) ? Optional.of(TERMS.get(id).get()) : Optional.empty(); }
	
	public static List<Term> placeables() { return TERMS.values().stream().map(Supplier::get).filter(Term::isPlaceable).toList(); }
	
	public static void init()
	{
		CyclicDungeons.LOGGER.info("# Initialised {} grammar terms ({} placeable)", TERMS.size(), tally);
	}
}

package com.lying.init;

import static com.lying.reference.Reference.ModInfo.prefix;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

import org.joml.Vector2i;

import com.lying.CyclicDungeons;
import com.lying.grammar.GrammarPhrase;
import com.lying.grammar.GrammarRoom;
import com.lying.grammar.GrammarTerm;

import net.minecraft.util.DyeColor;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.random.Random;

@SuppressWarnings("unchecked")
public class CDTerms
{
	private static final Map<Identifier, Supplier<GrammarTerm>> TERMS = new HashMap<>();
	private static int tally = 0;
	
	// Initial building blocks
	public static final Supplier<GrammarTerm> START		= register("start", () -> GrammarTerm.Builder.create(0xFFFFFF, DyeColor.WHITE)
			.size(size(6, 6, 8, 8))
			.unplaceable());
	public static final Supplier<GrammarTerm> END		= register("end", () -> GrammarTerm.Builder.create(0xFFFFFF, DyeColor.WHITE)
			.size(size(6, 6, 8, 8))
			.unplaceable());
	
	/** Completely blank, only used to mark errors in generation */
	public static final Supplier<GrammarTerm> VOID			= register("void", () -> GrammarTerm.Builder.create(0x000000, DyeColor.BLACK)
			.unplaceable());
	
	// Replaceable rooms, only occur during generation
	public static final Supplier<GrammarTerm> BLANK		= register("blank", () -> GrammarTerm.Builder.create(0x080808, DyeColor.LIGHT_GRAY)
			.unplaceable()
			.replaceable());
	public static final Supplier<GrammarTerm> ADD			= register("add_room", () -> GrammarTerm.Builder.create(0xD2D2D2, DyeColor.WHITE)
			.replaceable()
			.sizeCap(6)
			.weight(3)
			.onApply((t,r,g) -> GrammarTerm.injectRoom(r, g)));
	public static final Supplier<GrammarTerm> ADD_BRANCH	= register("add_branch", () -> GrammarTerm.Builder.create(0xB9B9B9, DyeColor.WHITE)
			.replaceable()
			.injectsBranches()
			.sizeCap(6)
			.weight(4)
			.onApply((t,r,g) -> GrammarTerm.injectBranch(r, g)));
	
	// Functional rooms
	public static final Supplier<GrammarTerm> EMPTY			= register("empty", () -> GrammarTerm.Builder.create(0xA6A6A6, DyeColor.GRAY)
			.size(size(7, 7, 10, 10))
			.withTileSet(CDRoomTileSets.EMPTY_ROOM_TILESET)
			.nonconsecutive()
			.allowDeadEnds(false)
			.neverAfter(CDTerms.START)
			.neverBefore(CDTerms.END));
	public static final Supplier<GrammarTerm> BATTLE		= register("battle", () -> GrammarTerm.Builder.create(0xC80707, DyeColor.ORANGE)
			.size(size(8, 8, 14, 14))
			.withTileSet(CDRoomTileSets.BATTLE_ROOM_TILESET)
			.nonconsecutive()
			.weight(3));
	public static final Supplier<GrammarTerm> TRAP			= register("trap", () -> GrammarTerm.Builder.create(0xAE31DE, DyeColor.MAGENTA)
			.size(size(6, 6, 8, 8))
			.nonconsecutive()
			.weight(2));
	public static final Supplier<GrammarTerm> BIG_PUZZLE	= register("big_puzzle", () -> GrammarTerm.Builder.create(0x3136DE, DyeColor.BLUE)
			.size(size(10, 10, 16, 16))
			.withTileSet(CDRoomTileSets.PUZZLE_ROOM_TILESET)
			.nonconsecutive()
			.popCap(2)
			.onApply(CDTerms::injectTreasure));
	public static final Supplier<GrammarTerm> SML_PUZZLE	= register("small_puzzle", () -> GrammarTerm.Builder.create(0x2768CA, DyeColor.LIGHT_BLUE)
			.size(size(5, 5, 8, 8))
			.withTileSet(CDRoomTileSets.PUZZLE_ROOM_TILESET)
			.nonconsecutive()
			.popCap(4));
	public static final Supplier<GrammarTerm> BOSS			= register("boss", () -> GrammarTerm.Builder.create(0x7D1D1D, DyeColor.RED)
			.size(size(16, 16))
			.withTileSet(CDRoomTileSets.BOSS_ROOM_TILESET)
			.popCap(1)
			.weight(10)
			.neverAfter(CDTerms.START)
			.onlyBefore(CDTerms.END)
			.onApply(CDTerms::injectTreasure));
	public static final Supplier<GrammarTerm> TREASURE		= register("treasure", () -> GrammarTerm.Builder.create(0xFFDC40, DyeColor.YELLOW)
			.size(size(5, 5))
			.withTileSet(CDRoomTileSets.TREASURE_ROOM_TILESET)
			.popCap(3)
			.weight(2)
			.onlyAfter(CDTerms.BATTLE, CDTerms.SML_PUZZLE, CDTerms.EMPTY));
	
	private static void injectTreasure(GrammarTerm t, GrammarRoom r, GrammarPhrase g)
	{
		GrammarTerm.injectRoom(r, g).applyTerm(CDTerms.TREASURE.get(), g);
	}
	
	private static Function<Random, Vector2i> size(int x, int y)
	{
		return r -> new Vector2i(x, y);
	}
	
	private static Function<Random, Vector2i> size(int minX, int minY, int maxX, int maxY)
	{
		Function<Random, Integer> x = minX == maxX ? r -> minX : r -> minX + r.nextInt(maxX - minX);
		Function<Random, Integer> y = minY == maxY ? r -> minY : r -> minY + r.nextInt(maxY - minY);
		return r -> new Vector2i(x.apply(r), y.apply(r));
	}
	
	private static Supplier<GrammarTerm> register(String name, Supplier<GrammarTerm.Builder> funcIn)
	{
		final Identifier id = prefix(name);
		final GrammarTerm term = funcIn.get().build(id);
		if(term.isPlaceable())
			tally++;
		Supplier<GrammarTerm> sup = () -> term;
		TERMS.put(id, sup);
		return sup;
	}
	
	public static Optional<GrammarTerm> get(String name) { return get(name.contains(":") ? Identifier.of(name) : prefix(name)); }
	
	public static Optional<GrammarTerm> get(Identifier id) { return TERMS.containsKey(id) ? Optional.of(TERMS.get(id).get()) : Optional.empty(); }
	
	public static List<GrammarTerm> placeables() { return TERMS.values().stream().map(Supplier::get).filter(GrammarTerm::isPlaceable).toList(); }
	
	public static void init()
	{
		CyclicDungeons.LOGGER.info("# Initialised {} grammar terms ({} placeable)", TERMS.size(), tally);
	}
}

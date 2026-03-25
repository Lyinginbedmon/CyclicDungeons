package com.lying.worldgen.theme;

import static com.lying.reference.Reference.ModInfo.prefix;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

import com.google.common.collect.Lists;
import com.lying.grammar.DefaultTerms;
import com.lying.init.CDTerms;
import com.lying.worldgen.theme.InitialPhrase.PhraseTerm;

import net.minecraft.util.Identifier;

public class DefaultPhrases
{
	private static final List<Supplier<InitialPhrase>> PHRASES = Lists.newArrayList();
	
	public static final Identifier
		ID_LINEAR			= prefix("linear"),
		ID_MILD_BRANCHING	= prefix("mild_branching"),
		ID_SIMPLE			= prefix("simple");
	
	protected static final Supplier<InitialPhrase> LINEAR	= register(ID_LINEAR, id -> new InitialPhrase(id)
			.add(new PhraseTerm("start", Optional.of(CDTerms.ID_START), Optional.of(List.of("a"))))
			.add(new PhraseTerm("a", Optional.of(CDTerms.ID_START), Optional.of(List.of("b"))))
			.add(new PhraseTerm("b", Optional.of(CDTerms.ID_START), Optional.of(List.of("c"))))
			.add(new PhraseTerm("c", Optional.of(CDTerms.ID_START), Optional.of(List.of("d"))))
			.add(new PhraseTerm("d", Optional.of(CDTerms.ID_START), Optional.of(List.of("e"))))
			.add(new PhraseTerm("e", Optional.of(CDTerms.ID_START), Optional.of(List.of("terminus"))))
			.add(new PhraseTerm("terminus", Optional.of(CDTerms.ID_END), Optional.empty())));
	
	protected static final Supplier<InitialPhrase> MILD_BRANCHING	= register(ID_MILD_BRANCHING, id -> new InitialPhrase(id)
			.add(new PhraseTerm("start", Optional.of(CDTerms.ID_START), Optional.of(List.of("r0", "r1"))))
			.add(new PhraseTerm("r1", Optional.empty(), Optional.of(List.of("r2", "r3", "r4"))))
			.add(new PhraseTerm("r3", Optional.empty(), Optional.of(List.of("r5", "r6"))))
			.add(new PhraseTerm("r5", Optional.empty(), Optional.of(List.of("r7", "r8", "r9"))))
			.add(new PhraseTerm("r8", Optional.empty(), Optional.of(List.of("r10"))))
			.add(new PhraseTerm("r10", Optional.empty(), Optional.of(List.of("r11", "r12", "r13"))))
			.add(new PhraseTerm("r12", Optional.empty(), Optional.of(List.of("r14", "r15", "r16"))))
			.add(new PhraseTerm("r14", Optional.of(CDTerms.ID_END), Optional.empty())));
	
	protected static final Supplier<InitialPhrase> SIMPLE	= register(ID_SIMPLE, id -> new InitialPhrase(id)
			.add(new PhraseTerm("entrance", Optional.of(CDTerms.ID_START), Optional.of(List.of("atrium"))))
			.add(new PhraseTerm("atrium", Optional.empty(), Optional.of(List.of("armory", "barracks", "kitchen"))))
			.add(new PhraseTerm("kitchen", Optional.empty(), Optional.of(List.of("pantry"))))
			.add(new PhraseTerm("armory", Optional.empty(), Optional.of(List.of("vault"))))
			.add(new PhraseTerm("vault", Optional.of(DefaultTerms.ID_TREASURE), Optional.empty()))
			.add(new PhraseTerm("barracks", Optional.of(CDTerms.ID_END), Optional.empty())));
	
	public static Supplier<InitialPhrase> register(Identifier id, Function<Identifier, InitialPhrase> func)
	{
		final Supplier<InitialPhrase> entry = () -> func.apply(id);
		PHRASES.add(entry);
		return entry;
	}
	
	public static List<InitialPhrase> getDefaults() { return PHRASES.stream().map(Supplier::get).toList(); }
}

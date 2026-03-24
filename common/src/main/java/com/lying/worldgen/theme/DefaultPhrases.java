package com.lying.worldgen.theme;

import static com.lying.reference.Reference.ModInfo.prefix;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

import com.google.common.collect.Lists;
import com.lying.init.CDTerms;
import com.lying.worldgen.theme.InitialPhrase.PhraseTerm;

import net.minecraft.util.Identifier;

public class DefaultPhrases
{
	private static final List<Supplier<InitialPhrase>> PHRASES = Lists.newArrayList();
	
	public static final Identifier
		ID_LINEAR	= prefix("linear");
	
	protected static final Supplier<InitialPhrase> LINEAR	= register(ID_LINEAR, id -> new InitialPhrase(id)
			.add(new PhraseTerm("start", Optional.of(CDTerms.ID_START), Optional.of(List.of("r0", "r1"))))
			.add(new PhraseTerm("r1", Optional.empty(), Optional.of(List.of("r2", "r3", "r4"))))
			.add(new PhraseTerm("r3", Optional.empty(), Optional.of(List.of("r5", "r6"))))
			.add(new PhraseTerm("r5", Optional.empty(), Optional.of(List.of("r7", "r8", "r9"))))
			.add(new PhraseTerm("r8", Optional.empty(), Optional.of(List.of("r10"))))
			.add(new PhraseTerm("r10", Optional.empty(), Optional.of(List.of("r11", "r12", "r13"))))
			.add(new PhraseTerm("r12", Optional.empty(), Optional.of(List.of("r14", "r15", "r16"))))
			.add(new PhraseTerm("r14", Optional.of(CDTerms.ID_END), Optional.empty())));
	
	public static Supplier<InitialPhrase> register(Identifier id, Function<Identifier, InitialPhrase> func)
	{
		final Supplier<InitialPhrase> entry = () -> func.apply(id);
		PHRASES.add(entry);
		return entry;
	}
	
	public static List<InitialPhrase> getDefaults() { return PHRASES.stream().map(Supplier::get).toList(); }
}

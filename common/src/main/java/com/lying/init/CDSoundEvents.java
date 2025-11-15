package com.lying.init;

import static com.lying.reference.Reference.ModInfo.prefix;

import java.util.function.Supplier;

import com.lying.CyclicDungeons;
import com.lying.reference.Reference;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;

@SuppressWarnings("unused")
public class CDSoundEvents
{
	public static final DeferredRegister<SoundEvent> SOUND_EVENTS 	= DeferredRegister.create(Reference.ModInfo.MOD_ID, RegistryKeys.SOUND_EVENT);
	private static int tally;
	
	public static final RegistrySupplier<SoundEvent> WIRING_GUN	= register(prefix("wiring_gun"));
	
	private static RegistrySupplier<SoundEvent> register(Identifier name)
	{
		tally++;
		return SOUND_EVENTS.register(name, () -> SoundEvent.of(name));
	}
	
	public static void init()
	{
		SOUND_EVENTS.register();
		CyclicDungeons.LOGGER.info("# Registered {} sound events", tally);
	}
	
	private static class SupplierSoundGroup extends BlockSoundGroup
	{
		private final Supplier<SoundEvent> breakSound;
		private final Supplier<SoundEvent> stepSound;
		private final Supplier<SoundEvent> placeSound;
		private final Supplier<SoundEvent> hitSound;
		private final Supplier<SoundEvent> fallSound;
		
		public SupplierSoundGroup(
				float volume, 
				float pitch, 
				Supplier<SoundEvent> breakSound, 
				Supplier<SoundEvent> stepSound,
				Supplier<SoundEvent> placeSound, 
				Supplier<SoundEvent> hitSound, 
				Supplier<SoundEvent> fallSound)
		{
			super(volume, pitch, null, null, null, null, null);
			this.breakSound = breakSound;
			this.stepSound = stepSound;
			this.placeSound = placeSound;
			this.hitSound = hitSound;
			this.fallSound = fallSound;
		}
		
		public SoundEvent getBreakSound() { return this.breakSound.get(); }
		
		public SoundEvent getStepSound() { return this.stepSound.get(); }
		
		public SoundEvent getPlaceSound() { return this.placeSound.get(); }
		
		public SoundEvent getHitSound() { return this.hitSound.get(); }
		
		public SoundEvent getFallSound() { return this.fallSound.get(); }
	}
}

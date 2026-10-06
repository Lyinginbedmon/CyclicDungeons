package com.lying.init;

import static com.lying.reference.Reference.ModInfo.prefix;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import org.joml.Vector2i;

import com.lying.item.component.CircuitComponent;
import com.lying.item.component.CircuitComponent.CircuitPart;

import net.minecraft.util.Identifier;

public class CDLogicCircuits
{
	private static final Map<Identifier, Supplier<CircuitComponent>> REGISTRY = new HashMap<>();
	
	public static final Identifier ID_BIT3_COMBO_LOCK	= prefix("3bit_combo_lock");
	
	// TODO Implement more standardised logic circuits
	
	public static final Supplier<CircuitComponent> BIT3_COMBO_LOCK	= register(ID_BIT3_COMBO_LOCK, List.of(
			CircuitPart.of(CDLogicGates.TRUE.get().create()
					.addOutput(CDLogicGates.OUTPUT, "set"), new Vector2i(-1,3)),
			
			CircuitPart.of(CDLogicGates.ENTRY.get().create("in1").addOutput(CDLogicGates.OUTPUT, "aI"), new Vector2i(0,0)),
			CircuitPart.of(CDLogicGates.RAND.get().create("bit1").addInput("set").addOutput(CDLogicGates.OUTPUT, "aR"), new Vector2i(0,1)),
			CircuitPart.of(CDLogicGates.XNOR.get().create("mask1")
					.addInput(CDLogicGates.makeInput(0), "aI")
					.addInput(CDLogicGates.makeInput(1), "aR")
					.addOutput(CDLogicGates.OUTPUT, "out1"), new Vector2i(2,1)),
			
			CircuitPart.of(CDLogicGates.ENTRY.get().create("in2").addOutput(CDLogicGates.OUTPUT, "bI"), new Vector2i(0,2)),
			CircuitPart.of(CDLogicGates.RAND.get().create("bit2").addInput("set").addOutput(CDLogicGates.OUTPUT, "bR"), new Vector2i(0,3)),
			CircuitPart.of(CDLogicGates.XNOR.get().create("mask2")
					.addInput(CDLogicGates.makeInput(0), "bI")
					.addInput(CDLogicGates.makeInput(1), "bR")
					.addOutput(CDLogicGates.OUTPUT, "out2"), new Vector2i(2,3)),
			
			CircuitPart.of(CDLogicGates.ENTRY.get().create("in3").addOutput(CDLogicGates.OUTPUT, "cI"), new Vector2i(0,4)),
			CircuitPart.of(CDLogicGates.RAND.get().create("bit3").addInput("set").addOutput(CDLogicGates.OUTPUT, "cR"), new Vector2i(0,5)),
			CircuitPart.of(CDLogicGates.XNOR.get().create("mask3")
					.addInput(CDLogicGates.makeInput(0), "cI")
					.addInput(CDLogicGates.makeInput(1), "cR")
					.addOutput(CDLogicGates.OUTPUT, "out3"), new Vector2i(2,5)),
			
			CircuitPart.of(CDLogicGates.AND.get().create()
					.addInput(CDLogicGates.makeInput(0), "out1")
					.addInput(CDLogicGates.makeInput(1), "out2")
					.addInput(CDLogicGates.makeInput(2), "out3")
					.addOutput(CDLogicGates.OUTPUT, "out"), new Vector2i(4,3)),
			CircuitPart.of(CDLogicGates.EXIT.get().create("result")
					.addInput(CDLogicGates.INPUT, "out"), new Vector2i(5,3))
			));
	
	public static Supplier<CircuitComponent> register(Identifier id, List<CircuitPart> gatesIn)
	{
		final Supplier<CircuitComponent> sup = () -> CircuitComponent.of(gatesIn);
		REGISTRY.put(id, sup);
		return sup;
	}
	
	public static Optional<CircuitComponent> get(Identifier idIn)
	{
		return REGISTRY.containsKey(idIn) ? Optional.of(REGISTRY.get(idIn).get()) : Optional.empty();
	}
}

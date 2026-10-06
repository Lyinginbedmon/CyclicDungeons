package com.lying.grammar.content.defaults;

import static com.lying.reference.Reference.ModInfo.prefix;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

import com.lying.block.Port;
import com.lying.grammar.content.Content;
import com.lying.grammar.content.generation.ModularContent;
import com.lying.grammar.content.generation.modular.Module;
import com.lying.grammar.content.generation.modular.ModuleWiring;
import com.lying.grammar.content.generation.modular.ModuleWiring.Complex.Output;
import com.lying.init.CDBlocks;
import com.lying.init.CDContentTypes.ContentEntry;
import com.lying.init.CDDataComponentTypes;
import com.lying.init.CDItems;
import com.lying.init.CDLogicCircuits;
import com.lying.init.CDLogicGates;
import com.lying.item.component.CircuitComponent;
import com.lying.item.component.CircuitComponent.CircuitPart;
import com.lying.utility.BlockPredicate;
import com.lying.utility.BlockPredicate.BlockFlags;
import com.lying.utility.BlockPredicate.SubPredicate;
import com.lying.worldgen.tile.DefaultTiles;
import com.lying.worldgen.tileset.DoorWaySet;

import net.minecraft.block.Blocks;
import net.minecraft.block.LeverBlock;
import net.minecraft.block.enums.BlockFace;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtOps;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

public class DefaultPuzzles
{
	private static final Map<Identifier, Supplier<ContentEntry>> PUZZLE = new HashMap<>();
	
	public static final Identifier ID_COMBO_LOCK = prefix("combo_lock");
	
	protected static final BlockPredicate ON_SOLID_GROUND = BlockPredicate.Builder.create()
			.addFlag(BlockFlags.AIR)
			.child(new SubPredicate(BlockPos.ORIGIN.down(), BlockPredicate.Builder.create()
					.addFlag(BlockFlags.SOLID)
					.build()))
			.build();
	protected static final BlockPredicate IN_FLOOR = BlockPredicate.Builder.create()
			.addFlag(BlockFlags.SOLID)
			.child(new SubPredicate(BlockPos.ORIGIN.up(), BlockPredicate.Builder.create()
					.addFlag(BlockFlags.AIR)
					.build()))
			.build();
	
	public static final Supplier<ContentEntry> COMBO_LOCK	= register(ID_COMBO_LOCK, () -> ModularContent.create()
			.module(Module.Builder
					.of(prefix("emitter"))
					.positioned(ON_SOLID_GROUND)
					.block(CDBlocks.PUZZLE_TRIGGER.get())
					.wiring(ModuleWiring.Complex.create()
							.attach(CDLogicGates.INPUT, Output.of(Port.of("result"), prefix("logic")))
						)
					.markVital()
					.build())
			
			.module(Module.Builder
					.of(prefix("logic"))
					.positioned(IN_FLOOR)
					.block(CDBlocks.MODULAR_LOGIC.get())
					.logicModule(CDLogicCircuits.ID_BIT3_COMBO_LOCK)
					.wiring(ModuleWiring.Complex.create()
							.attach(Port.of("in1"), Output.of(CDLogicGates.OUTPUT, prefix("sensor_1")))
							.attach(Port.of("in2"), Output.of(CDLogicGates.OUTPUT, prefix("sensor_2")))
							.attach(Port.of("in3"), Output.of(CDLogicGates.OUTPUT, prefix("sensor_3")))
						)
					.markVital()
					.build())
			
			.module(Module.Builder
					.of(prefix("sensor_1"))
					.positioned(ON_SOLID_GROUND)
					.block(CDBlocks.SENSOR_REDSTONE.get())
					.markVital()
					.build())
			.module(Module.Builder
					.of(prefix("switch_1"))
					.blockState(Blocks.LEVER.getDefaultState().with(LeverBlock.FACE, BlockFace.FLOOR))
					.relation(prefix("sensor_1"), BlockPos.ORIGIN.down())
					.markVital()
					.build())
			
			.module(Module.Builder
					.of(prefix("sensor_2"))
					.positioned(ON_SOLID_GROUND)
					.block(CDBlocks.SENSOR_REDSTONE.get())
					.markVital()
					.build())
			.module(Module.Builder
					.of(prefix("switch_2"))
					.blockState(Blocks.LEVER.getDefaultState().with(LeverBlock.FACE, BlockFace.FLOOR))
					.relation(prefix("sensor_2"), BlockPos.ORIGIN.down())
					.markVital()
					.build())
			
			.module(Module.Builder
					.of(prefix("sensor_3"))
					.positioned(ON_SOLID_GROUND)
					.block(CDBlocks.SENSOR_REDSTONE.get())
					.markVital()
					.build())
			.module(Module.Builder
					.of(prefix("switch_3"))
					.blockState(Blocks.LEVER.getDefaultState().with(LeverBlock.FACE, BlockFace.FLOOR))
					.relation(prefix("sensor_3"), BlockPos.ORIGIN.down())
					.markVital()
					.build())
			.setExitDoorTiles(DoorWaySet.of(DefaultTiles.ID_PUZZLE_DOORWAY))
			);
	
	private static Supplier<ContentEntry> register(final Identifier id, Supplier<Content> func)
	{
		Supplier<ContentEntry> sup = () -> new ContentEntry(id, func.get());
		PUZZLE.put(id, sup);
		return sup;
	}
	
	public static List<ContentEntry> getAll()
	{
		return PUZZLE.values().stream().map(Supplier::get).toList();
	}
	
	public static Consumer<NbtCompound> encodeCircuit(final List<CircuitPart> parts)
	{
		return nbt -> 
		{
			NbtCompound card = new NbtCompound();
				card.putInt("count", 1);
				card.putString("id", CDItems.LOGIC_CARD.getIdAsString());
				NbtCompound comps = new NbtCompound();
					comps.put(CDDataComponentTypes.CIRCUIT.getIdAsString(), CircuitComponent.CODEC.encodeStart(NbtOps.INSTANCE, CircuitComponent.of(parts)).getOrThrow());
				card.put("components", comps);
			nbt.put("Card", card);
		};
	}
}

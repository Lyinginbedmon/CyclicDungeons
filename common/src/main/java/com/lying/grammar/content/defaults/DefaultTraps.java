package com.lying.grammar.content.defaults;

import static com.lying.reference.Reference.ModInfo.prefix;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import com.lying.block.actors.FlameJetBlock;
import com.lying.data.CDTags;
import com.lying.grammar.content.Content;
import com.lying.grammar.content.RoomNumberProvider;
import com.lying.grammar.content.generation.ModularContent;
import com.lying.grammar.content.generation.SimpleJumpingContent;
import com.lying.grammar.content.generation.StructurePlacerContent;
import com.lying.grammar.content.generation.TileContent;
import com.lying.grammar.content.generation.TileSetContent;
import com.lying.grammar.content.generation.TileToBlockContent;
import com.lying.grammar.content.generation.modular.Module;
import com.lying.grammar.content.generation.modular.ModuleWiring;
import com.lying.grammar.content.generation.modular.ModuleWiring.Complex.Output;
import com.lying.init.CDBlocks;
import com.lying.init.CDContentTypes.ContentEntry;
import com.lying.init.CDLogicGates;
import com.lying.utility.BlockPredicate;
import com.lying.utility.BlockPredicate.BlockFlags;
import com.lying.utility.BlockPredicate.ChildLogic;
import com.lying.utility.BlockPredicate.SubPredicate;
import com.lying.worldgen.tile.DefaultTiles;
import com.lying.worldgen.tileset.DefaultTileSets;

import net.minecraft.block.Blocks;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

public class DefaultTraps
{
	private static final Map<Identifier, Supplier<ContentEntry>> TRAPS = new HashMap<>();
	
	public static final Identifier
		ID_PITFALL			= prefix("pitfall"),
		ID_LAVA_RIVER		= prefix("lava_river"),
		ID_PIT_JUMPING		= prefix("pit_jumping"),
		ID_LAVA_JUMPING		= prefix("lava_jumping"),
		ID_MINEFIELD		= prefix("minefield"),
		ID_BEARTRAPS		= prefix("beartraps"),
		ID_BEAR_TRAPS		= prefix("bear_traps"),
		ID_HATCH_PITFALL	= prefix("hatch_pitfall"),
		ID_MODULE_TEST		= prefix("module_test");
	
	public static final Supplier<ContentEntry> PITFALL			= register(ID_PITFALL, () -> TileSetContent.of(DefaultTileSets.ID_PITFALL_TRAP));
	public static final Supplier<ContentEntry> LAVA_RIVER		= register(ID_LAVA_RIVER, () -> TileContent.of(DefaultTiles.ID_LAVA_RIVER, new RoomNumberProvider.Unlimited(), false));
	public static final Supplier<ContentEntry> PIT_JUMPING		= register(ID_PIT_JUMPING, () -> SimpleJumpingContent.of(DefaultTiles.ID_PIT));
	public static final Supplier<ContentEntry> LAVA_JUMPING	= register(ID_LAVA_JUMPING, () -> SimpleJumpingContent.of(DefaultTiles.ID_LAVA));
	public static final Supplier<ContentEntry> MINEFIELD		= register(ID_MINEFIELD, () -> StructurePlacerContent.of(
			prefix("trap/landmine"), 
			new BlockPos(0, -2, 0), 
			4, 
			3, 
			new RoomNumberProvider.SizeRatio(1, 1, 1D/8D), 
			BlockPredicate.Builder.create().addFlag(BlockFlags.AIR)
				.child(new SubPredicate(BlockPos.ORIGIN.down(1), BlockPredicate.Builder.create().addFlag(BlockFlags.SOLID).build()))
				.build()
				));
	public static final Supplier<ContentEntry> BEARTRAPS		= register(ID_BEARTRAPS, () -> StructurePlacerContent.of(
			prefix("trap/beartrap"), 
			new BlockPos(0, -2, 0), 
			4, 
			3, 
			new RoomNumberProvider.SizeRatio(1, 1, 1D/8D), 
			BlockPredicate.Builder.create().addFlag(BlockFlags.AIR)
				.child(new SubPredicate(BlockPos.ORIGIN.down(1), BlockPredicate.Builder.create().addFlag(BlockFlags.SOLID).build()))
				.child(new SubPredicate(BlockPos.ORIGIN.down(2), BlockPredicate.Builder.create().addFlag(BlockFlags.AIR).invert().build()))
				.build()
				));
	public static final Supplier<ContentEntry> BEAR_TRAPS		= register(ID_BEAR_TRAPS, () -> StructurePlacerContent.of(
			prefix("trap/bear_trap"), 
			new BlockPos(0, -2, 0), 
			4, 
			3, 
			new RoomNumberProvider.SizeRatio(1, 1, 1D/8D), 
			BlockPredicate.Builder.create().addFlag(BlockFlags.AIR)
				.child(new SubPredicate(BlockPos.ORIGIN.down(1), BlockPredicate.Builder.create().addFlag(BlockFlags.SOLID).build()))
				.child(new SubPredicate(BlockPos.ORIGIN.down(2), BlockPredicate.Builder.create().addFlag(BlockFlags.AIR).invert().build()))
				.build()
				));
	public static final Supplier<ContentEntry> HATCH_PITFALL	= register(ID_HATCH_PITFALL, () -> TileToBlockContent.of(
			DefaultTiles.ID_HATCH, 
			new RoomNumberProvider.SizeRatio(1, 1, 0.5),
			prefix("trap/pressure_plate"), 
			new RoomNumberProvider.RandBetween(1, 5, 2),
			BlockPredicate.Builder.create().addFlag(BlockFlags.AIR)
				.child(new SubPredicate(BlockPos.ORIGIN.down(1), BlockPredicate.Builder.create().addFlag(BlockFlags.SOLID).build()))
				.child(new SubPredicate(BlockPos.ORIGIN.down(1), BlockPredicate.Builder.create()
						.childLogic(ChildLogic.OR)
						.child(new SubPredicate(BlockPos.ORIGIN.north(), BlockPredicate.Builder.create().addBlockTag(CDTags.TRAP_HATCHES).build()))
						.child(new SubPredicate(BlockPos.ORIGIN.east(), BlockPredicate.Builder.create().addBlockTag(CDTags.TRAP_HATCHES).build()))
						.child(new SubPredicate(BlockPos.ORIGIN.south(), BlockPredicate.Builder.create().addBlockTag(CDTags.TRAP_HATCHES).build()))
						.child(new SubPredicate(BlockPos.ORIGIN.west(), BlockPredicate.Builder.create().addBlockTag(CDTags.TRAP_HATCHES).build()))
					.build()))
			.build(), BlockPos.ORIGIN)
			);
	public static final Supplier<ContentEntry> MODULE_TEST	= register(ID_MODULE_TEST, () -> ModularContent.create()
			.module(Module.Builder
					.of(prefix("chest"))
					.positioned(BlockPredicate.Builder.create().addFlag(BlockFlags.AIR).child(new SubPredicate(BlockPos.ORIGIN.down(), BlockPredicate.Builder.create().addFlag(BlockFlags.SOLID).build())).build())
					.blockState(Blocks.TRAPPED_CHEST.getDefaultState())
					.markVital()
					.build())
			.module(Module.Builder
					.of(prefix("sensor"))
					.positioned(BlockPredicate.Builder.create().addFlag(BlockFlags.SOLID).build())
					.relation(prefix("chest"), BlockPos.ORIGIN.up())
					.blockState(CDBlocks.SENSOR_REDSTONE.get().getDefaultState())
					.markVital()
					.build())
			.module(Module.Builder
					.of(prefix("jet"))
					.positioned(BlockPredicate.Builder.create().addFlag(BlockFlags.SOLID).build())
					.blockState(CDBlocks.FLAME_JET.get().getDefaultState().with(FlameJetBlock.FACING, Direction.UP))
					.relation(prefix("sensor"), BlockPos.ORIGIN.up())
//					.wiring(ModuleWiring.Simple.of(List.of(prefix("sensor"))))
					.wiring(ModuleWiring.Complex.create().attach(CDLogicGates.INPUT, Output.of(CDLogicGates.OUTPUT, prefix("sensor"))))
					.markVital()
					.build())
			);
	
	private static Supplier<ContentEntry> register(final Identifier id, Supplier<Content> func)
	{
		Supplier<ContentEntry> sup = () -> new ContentEntry(id, func.get());
		TRAPS.put(id, sup);
		return sup;
	}
	
	public static List<ContentEntry> getAll()
	{
		return TRAPS.values().stream().map(Supplier::get).toList();
	}
}

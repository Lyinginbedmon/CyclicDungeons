package com.lying.grammar.content.trap;

import static com.lying.reference.Reference.ModInfo.prefix;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import com.lying.block.FlameJetBlock;
import com.lying.data.CDTags;
import com.lying.grammar.content.RoomNumberProvider;
import com.lying.grammar.content.TrapRoomContent.TrapEntry;
import com.lying.init.CDBlocks;
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
	private static final Map<Identifier, Supplier<TrapEntry>> TRAPS = new HashMap<>();
	
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
	
	public static final Supplier<TrapEntry> PITFALL			= register(ID_PITFALL, () -> TileSetTrap.of(DefaultTileSets.ID_PITFALL_TRAP));
	public static final Supplier<TrapEntry> LAVA_RIVER		= register(ID_LAVA_RIVER, () -> TileTrap.of(DefaultTiles.ID_LAVA_RIVER, new RoomNumberProvider.Unlimited(), false));
	public static final Supplier<TrapEntry> PIT_JUMPING		= register(ID_PIT_JUMPING, () -> SimpleJumpingTrap.of(DefaultTiles.ID_PIT));
	public static final Supplier<TrapEntry> LAVA_JUMPING	= register(ID_LAVA_JUMPING, () -> SimpleJumpingTrap.of(DefaultTiles.ID_LAVA));
	public static final Supplier<TrapEntry> MINEFIELD		= register(ID_MINEFIELD, () -> StructurePlacerTrap.of(
			prefix("trap/landmine"), 
			new BlockPos(0, -2, 0), 
			4, 
			3, 
			new RoomNumberProvider.SizeRatio(1, 1, 1/8), 
			BlockPredicate.Builder.create().addFlag(BlockFlags.AIR)
				.child(new SubPredicate(BlockPos.ORIGIN.down(1), BlockPredicate.Builder.create().addFlag(BlockFlags.SOLID).build()))
				.child(new SubPredicate(BlockPos.ORIGIN.down(2), BlockPredicate.Builder.create().addFlag(BlockFlags.AIR).invert().build()))
				.build()
				));
	public static final Supplier<TrapEntry> BEARTRAPS		= register(ID_BEARTRAPS, () -> StructurePlacerTrap.of(
			prefix("trap/beartrap"), 
			new BlockPos(0, -2, 0), 
			4, 
			3, 
			new RoomNumberProvider.SizeRatio(1, 1, 1/8), 
			BlockPredicate.Builder.create().addFlag(BlockFlags.AIR)
				.child(new SubPredicate(BlockPos.ORIGIN.down(1), BlockPredicate.Builder.create().addFlag(BlockFlags.SOLID).build()))
				.child(new SubPredicate(BlockPos.ORIGIN.down(2), BlockPredicate.Builder.create().addFlag(BlockFlags.AIR).invert().build()))
				.build()
				));
	public static final Supplier<TrapEntry> BEAR_TRAPS		= register(ID_BEAR_TRAPS, () -> StructurePlacerTrap.of(
			prefix("trap/bear_trap"), 
			new BlockPos(0, -2, 0), 
			4, 
			3, 
			new RoomNumberProvider.SizeRatio(1, 1, 1/8), 
			BlockPredicate.Builder.create().addFlag(BlockFlags.AIR)
				.child(new SubPredicate(BlockPos.ORIGIN.down(1), BlockPredicate.Builder.create().addFlag(BlockFlags.SOLID).build()))
				.child(new SubPredicate(BlockPos.ORIGIN.down(2), BlockPredicate.Builder.create().addFlag(BlockFlags.AIR).invert().build()))
				.build()
				));
	public static final Supplier<TrapEntry> HATCH_PITFALL	= register(ID_HATCH_PITFALL, () -> TileToBlockTrap.of(
			DefaultTiles.ID_HATCH, 
			new RoomNumberProvider.SizeRatio(1, 1, 0.3),
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
	public static final Supplier<TrapEntry> MODULE_TEST	= register(ID_MODULE_TEST, () -> ModularTrap.create()
			.module(ModularTrap.Module.Builder
					.of(prefix("chest"))
					.positioned(BlockPredicate.Builder.create().addFlag(BlockFlags.AIR).child(new SubPredicate(BlockPos.ORIGIN.down(), BlockPredicate.Builder.create().addFlag(BlockFlags.SOLID).build())).build())
					.blockState(Blocks.TRAPPED_CHEST.getDefaultState())
					.markVital()
					.build())
			.module(ModularTrap.Module.Builder
					.of(prefix("sensor"))
					.positioned(BlockPredicate.Builder.create().addFlag(BlockFlags.SOLID).build())
					.relation(prefix("chest"), BlockPos.ORIGIN.up())
					.blockState(CDBlocks.SENSOR_REDSTONE.get().getDefaultState())
					.markVital()
					.build())
			.module(ModularTrap.Module.Builder
					.of(prefix("jet"))
					.positioned(BlockPredicate.Builder.create().addFlag(BlockFlags.SOLID).build())
					.blockState(CDBlocks.FLAME_JET.get().getDefaultState().with(FlameJetBlock.FACING, Direction.UP))
					.relation(prefix("sensor"), BlockPos.ORIGIN.up())
					.connection(prefix("sensor"))
					.markVital()
					.build())
			);
	
	private static Supplier<TrapEntry> register(final Identifier id, Supplier<Trap> func)
	{
		Supplier<TrapEntry> sup = () -> new TrapEntry(id, func.get());
		TRAPS.put(id, sup);
		return sup;
	}
	
	public static List<TrapEntry> getAll()
	{
		return TRAPS.values().stream().map(Supplier::get).toList();
	}
}

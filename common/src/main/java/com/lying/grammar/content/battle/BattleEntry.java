package com.lying.grammar.content.battle;

import java.util.List;
import java.util.Optional;

import com.google.common.collect.Lists;
import com.lying.block.entity.EncounterSpawnerBlockEntity;
import com.lying.grammar.RoomMetadata;
import com.lying.grammar.content.IContentEntry;
import com.lying.init.CDBlockEntityTypes;
import com.lying.init.CDBlocks;
import com.lying.utility.BlockPredicate;
import com.lying.utility.BlockPredicate.BlockFlags;
import com.lying.utility.BlockPredicate.ChildLogic;
import com.lying.utility.BlockPredicate.SubPredicate;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

public record BattleEntry(Identifier registryName, Battle encounter) implements IContentEntry
{
	public static final Codec<BattleEntry> CODEC	= RecordCodecBuilder.create(instance -> instance.group(
			Identifier.CODEC.fieldOf("Name").forGetter(BattleEntry::registryName),
			Battle.CODEC.fieldOf("Encounter").forGetter(BattleEntry::encounter)
			).apply(instance, BattleEntry::new));
	private static final BlockPredicate PREDICATE	= BlockPredicate.Builder.create()
			.addFlag(BlockFlags.AIR)
			.childLogic(ChildLogic.OR)
				.child(new SubPredicate(BlockPos.ORIGIN.north(), BlockPredicate.Builder.create().addFlag(BlockFlags.SOLID).build()))
				.child(new SubPredicate(BlockPos.ORIGIN.east(), BlockPredicate.Builder.create().addFlag(BlockFlags.SOLID).build()))
				.child(new SubPredicate(BlockPos.ORIGIN.south(), BlockPredicate.Builder.create().addFlag(BlockFlags.SOLID).build()))
				.child(new SubPredicate(BlockPos.ORIGIN.west(), BlockPredicate.Builder.create().addFlag(BlockFlags.SOLID).build()))
				.child(new SubPredicate(BlockPos.ORIGIN.up(), BlockPredicate.Builder.create().addFlag(BlockFlags.SOLID).build()))
				.child(new SubPredicate(BlockPos.ORIGIN.down(), BlockPredicate.Builder.create().addFlag(BlockFlags.SOLID).build()))
			.build();
	
	public void apply(BlockPos min, BlockPos max, ServerWorld world, RoomMetadata meta)
	{
		List<BlockPos> positions = Lists.newArrayList();
		BlockPos.Mutable.iterate(min, max).forEach(p -> 
		{
			if(PREDICATE.applyTo(p, world))
				positions.add(p.toImmutable());
		});
		
		final BlockPos pos = positions.isEmpty() ? min : positions.get(world.random.nextInt(positions.size()));
		world.setBlockState(pos, CDBlocks.ENCOUNTER.get().getDefaultState());
		
		Optional<EncounterSpawnerBlockEntity> tile = world.getBlockEntity(pos, CDBlockEntityTypes.ENCOUNTER.get());
		tile.ifPresent(entity -> 
		{
			entity.setRoomArea(min, max);
			entity.setBattle(encounter);
		});
	}
	
	public Text describe() { return encounter.describe(); }
}
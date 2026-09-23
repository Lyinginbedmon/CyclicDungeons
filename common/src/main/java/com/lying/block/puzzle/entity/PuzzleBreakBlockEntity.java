package com.lying.block.puzzle.entity;

import java.util.Optional;
import java.util.UUID;

import org.jetbrains.annotations.Nullable;

import com.lying.DungeonBuilder;
import com.lying.block.entity.IRoomTaggedBlock;
import com.lying.block.puzzle.PuzzleBreakBlock;
import com.lying.init.CDBlockEntityTypes;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class PuzzleBreakBlockEntity extends BlockEntity implements IRoomTaggedBlock
{
	private Optional<UUID> room = Optional.empty();
	private Optional<String> channel = Optional.empty();
	
	protected <T extends PuzzleBreakBlockEntity> PuzzleBreakBlockEntity(BlockEntityType<T> type, BlockPos pos, BlockState state)
	{
		super(type, pos, state);
	}
	
	public PuzzleBreakBlockEntity(BlockPos pos, BlockState state)
	{
		this(CDBlockEntityTypes.PUZZLE_BREAK.get(), pos, state);
	}
	
	protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup)
	{
		super.writeNbt(nbt, registryLookup);
		
		room.ifPresent(p -> nbt.putUuid("Room", p));
		channel.ifPresent(c -> nbt.putString("Channel", c));
	}
	
	protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup)
	{
		super.readNbt(nbt, registryLookup);
		
		room = nbt.contains("Room") ? Optional.of(nbt.getUuid("Room")) : Optional.empty();
		channel = nbt.contains("Channel", NbtElement.STRING_TYPE) ? Optional.of(nbt.getString("Channel")) : Optional.empty();
	}
	
	public void setRoom(UUID id)
	{
		room = room == null ? Optional.empty() : Optional.of(id);
	}
	
	@Nullable
	public UUID getRoom() { return room.orElse(null); }
	
	@SuppressWarnings("unchecked")
	@Nullable
	public static <E extends BlockEntity, A extends BlockEntity> BlockEntityTicker<A> validateTicker(BlockEntityType<A> given, BlockEntityType<E> expected, BlockEntityTicker<? super E> ticker)
	{
		return expected == given ? (BlockEntityTicker<A>)ticker : null;
	}
	
	public static <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type)
	{
		return type != CDBlockEntityTypes.PUZZLE_BREAK.get() ? 
				null : 
				PuzzleBreakBlock.validateTicker(type, CDBlockEntityTypes.PUZZLE_BREAK.get(), 
					world.isClient() ? 
						PuzzleBreakBlockEntity::tickClient : 
						PuzzleBreakBlockEntity::tickServer);
	}
	
	public static <T extends BlockEntity> void tickClient(World world, BlockPos pos, BlockState state, PuzzleBreakBlockEntity tile) { }
	
	public static <T extends BlockEntity> void tickServer(World world, BlockPos pos, BlockState state, PuzzleBreakBlockEntity tile)
	{
		tile.room.ifPresent(id -> 
		{
			if(DungeonBuilder.instance().hasBeenTriggered(id, tile.channel))
				tile.onTrigger();
		});
	}
	
	/** Called when the server registers a matching room event trigger */
	protected void onTrigger()
	{
		world.breakBlock(pos, false);
	}
}

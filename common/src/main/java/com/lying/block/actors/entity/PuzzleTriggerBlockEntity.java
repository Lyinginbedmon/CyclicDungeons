package com.lying.block.actors.entity;

import java.util.Optional;
import java.util.UUID;

import org.jetbrains.annotations.Nullable;

import com.lying.block.IWireableBlock;
import com.lying.block.actors.PuzzleTriggerBlock;
import com.lying.block.entity.IRoomTaggedBlock;
import com.lying.event.PuzzleTriggerEvent;
import com.lying.init.CDBlockEntityTypes;
import com.lying.reference.Reference;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtOps;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.text.Text;
import net.minecraft.text.TextCodecs;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class PuzzleTriggerBlockEntity extends TrapActorBlockEntity<PuzzleTriggerBlock> implements IRoomTaggedBlock
{
	private Optional<UUID> roomOrigin = Optional.empty();
	private Optional<String> triggerChannel = Optional.empty();
	
	private Optional<Text> message = Optional.of(Reference.ModInfo.translate("gui", "puzzle_trigger.activated"));
	// TODO Add optional command execution
	
	public PuzzleTriggerBlockEntity(BlockPos pos, BlockState state)
	{
		super(CDBlockEntityTypes.PUZZLE_TRIGGER.get(), pos, state);
	}
	
	public void setRoom(UUID room)
	{
		roomOrigin = room == null ? Optional.empty() : Optional.of(room);
	}
	
	@Nullable
	public UUID getRoom() { return roomOrigin.orElse(null); }
	
	protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup)
	{
		super.writeNbt(nbt, registryLookup);
		
		roomOrigin.ifPresent(p -> nbt.putUuid("Room", p));
		triggerChannel.ifPresent(c -> nbt.putString("Channel", c));
		message.ifPresent(t -> nbt.put("Message", TextCodecs.STRINGIFIED_CODEC.encodeStart(NbtOps.INSTANCE, t).getOrThrow()));
	}
	
	protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup)
	{
		super.readNbt(nbt, registryLookup);
		
		roomOrigin = nbt.contains("Room") ? Optional.of(nbt.getUuid("Room")) : Optional.empty();
		triggerChannel = nbt.contains("Channel", NbtElement.STRING_TYPE) ? Optional.of(nbt.getString("Channel")) : Optional.empty();
		message = nbt.contains("Message") ? Optional.of(TextCodecs.STRINGIFIED_CODEC.parse(NbtOps.INSTANCE, nbt.get("Message")).getOrThrow()) : Optional.empty();
	}
	
	@SuppressWarnings("unchecked")
	@Nullable
	public static <E extends BlockEntity, A extends BlockEntity> BlockEntityTicker<A> validateTicker(BlockEntityType<A> given, BlockEntityType<E> expected, BlockEntityTicker<? super E> ticker)
	{
		return expected == given ? (BlockEntityTicker<A>)ticker : null;
	}
	
	public static <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type)
	{
		return type != CDBlockEntityTypes.PUZZLE_TRIGGER.get() ? 
				null : 
				IWireableBlock.validateTicker(type, CDBlockEntityTypes.PUZZLE_TRIGGER.get(), 
					world.isClient() ? 
						TrapActorBlockEntity::tickClient : 
						TrapActorBlockEntity::tickServer);
	}
	
	public void onTrigger()
	{
		// Fire puzzle trigger event
		roomOrigin.ifPresent(r -> PuzzleTriggerEvent.EVENT.invoker().onPuzzleTriggered(r, triggerChannel));
		
		// Destroy self to prevent repeat firing
		getWorld().breakBlock(getPos(), false);
		
		message.ifPresent(text -> getWorld().getPlayers().stream()
				.filter(p -> p.getBlockPos().getManhattanDistance(getPos()) < 16)
				.forEach(p -> p.sendMessage(text, true)));
	}
}

package com.lying.block.actors.entity;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

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
import net.minecraft.nbt.NbtHelper;
import net.minecraft.nbt.NbtOps;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.text.TextCodecs;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.CommandBlockExecutor;
import net.minecraft.world.World;

public class PuzzleTriggerBlockEntity extends TrapActorBlockEntity<PuzzleTriggerBlock> implements IRoomTaggedBlock
{
	private Optional<UUID> roomOrigin = Optional.empty();
	private Optional<String> triggerChannel = Optional.empty();
	private Optional<Integer> triggerCount = Optional.empty();
	
	private Optional<Text> message = Optional.of(Reference.ModInfo.translate("gui", "puzzle_trigger.activated"));
	
	private Optional<String> command = Optional.empty();
	private final CommandBlockExecutor executor = new CommandBlockExecutor()
			{
				public ServerWorld getWorld() { return (ServerWorld)PuzzleTriggerBlockEntity.this.world; }
				
				public void markDirty()
				{
					BlockState blockState = PuzzleTriggerBlockEntity.this.world.getBlockState(PuzzleTriggerBlockEntity.this.pos);
					this.getWorld().updateListeners(PuzzleTriggerBlockEntity.this.pos, blockState, blockState, 3);
				}
				
				public Vec3d getPos() { return Vec3d.ofCenter(PuzzleTriggerBlockEntity.this.pos); }
				
				public ServerCommandSource getSource()
				{
					return new ServerCommandSource(
						this,
						Vec3d.ofCenter(PuzzleTriggerBlockEntity.this.pos),
						new Vec2f(0.0F, Direction.UP.getPositiveHorizontalDegrees()),
						this.getWorld(),
						2,
						this.getName().getString(),
						this.getName(),
						this.getWorld().getServer(),
						null
					);
				}
				
				public boolean isEditable() { return false; }
			};
	
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
		triggerCount.ifPresent(i -> nbt.putInt("Triggers", i));
		
		message.ifPresent(t -> nbt.put("Message", TextCodecs.STRINGIFIED_CODEC.encodeStart(NbtOps.INSTANCE, t).getOrThrow()));
		command.ifPresent(t -> nbt.putString("Command", t));
	}
	
	protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup)
	{
		super.readNbt(nbt, registryLookup);
		
		roomOrigin = tryRetrieve(nbt, "Room", NbtHelper::toUuid);
		triggerChannel = tryRetrieve(nbt, "Channel");
		triggerCount = tryRetrieve(nbt, "Triggers");
		
		message = tryRetrieve(nbt, "Message", e -> TextCodecs.STRINGIFIED_CODEC.parse(NbtOps.INSTANCE, e).getOrThrow());
		command = tryRetrieve(nbt, "Command");
	}
	
	protected static <T extends Object> Optional<T> tryRetrieve(NbtCompound nbt, String name)
	{
		return tryRetrieve(nbt, name, null);
	}
	
	@SuppressWarnings("unchecked")
	protected static <T extends Object> Optional<T> tryRetrieve(NbtCompound nbt, String name, Function<NbtElement,T> converter)
	{
		if(nbt.contains(name))
		{
			final NbtElement ele = nbt.get(name);
			if(converter != null)
				return Optional.of(converter.apply(ele));
			else
			{
				Object val = null;
				switch(ele.getType())
				{
					case NbtElement.STRING_TYPE:
						val = nbt.getString(name);
						break;
					case NbtElement.INT_TYPE:
						val = nbt.getInt(name);
						break;
					case NbtElement.INT_ARRAY_TYPE:
						val = nbt.getIntArray(name);
						break;
				}
				
				if(val != null)
					return Optional.of((T)val);
			}
		}
		return Optional.empty();
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
		
		// Decrement trigger count and destroy, as appropriate
		int count = triggerCount.orElse(0);
		if(count >= 0)
			if(--count < 0)
				getWorld().breakBlock(getPos(), false);
			else
				triggerCount = Optional.of(count);
		
		command.ifPresent(str -> 
		{
			executor.setCommand(str);
			executor.execute(getWorld());
		});
		
		message.ifPresent(text -> getWorld().getPlayers().stream()
				.filter(p -> p.getBlockPos().getManhattanDistance(getPos()) < 16)
				.forEach(p -> p.sendMessage(text, true)));
	}
}

package com.lying.grammar.content;

import static com.lying.reference.Reference.ModInfo.prefix;

import java.util.Optional;

import com.lying.grammar.content.TrapRoomContent.TrapEntry;
import com.lying.worldgen.theme.Theme;

import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.BlockStateRaycastContext;

public class TrapRoomContent extends RegistryRoomContent<TrapEntry>
{
	public static final Identifier ID	= prefix("trap");
	
	public TrapRoomContent()
	{
		super(ID);
	}
	
	public void buildRegistry(Theme theme)
	{
		theme.traps().forEach(trap -> register(trap.registryName(), trap));
	}
	
	public static abstract class TrapEntry implements IContentEntry
	{
		private final Identifier id;
		
		protected TrapEntry(Identifier idIn)
		{
			id = idIn;
		}
		
		public Identifier registryName() { return id; }
		
		// FIXME Ensure raytrace actually succeeds
		
		public static Optional<BlockPos> getCeilingAbove(BlockPos pos, ServerWorld world) { return getCeilingAbove(pos, world, 10); }
		
		public static Optional<BlockPos> getCeilingAbove(BlockPos pos, ServerWorld world, int maxRange)
		{
			BlockPos top = pos.offset(Direction.UP, maxRange);
			BlockStateRaycastContext context = new BlockStateRaycastContext(
					new Vec3d(pos.getX(), pos.getY(), pos.getZ()).add(0.5D),
					new Vec3d(top.getX(), top.getY(), top.getZ()).add(0.5D),
					s -> s.isAir());
			
			BlockHitResult trace = world.raycast(context);
			return trace.getType() != Type.BLOCK ? Optional.empty() : Optional.of(trace.getBlockPos());
		}
	}
}

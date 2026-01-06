package com.lying.grammar;

import java.util.List;
import java.util.Optional;

import org.jetbrains.annotations.NotNull;
import org.joml.Vector2i;

import com.google.gson.JsonElement;
import com.lying.blueprint.Blueprint;
import com.lying.blueprint.BlueprintPassage;
import com.lying.blueprint.BlueprintRoom;
import com.lying.grammar.content.RoomContent;
import com.lying.grammar.modifier.PhraseModifier;
import com.lying.grid.BlueprintTileGrid;
import com.lying.grid.GraphTileGrid;
import com.lying.grid.GridTile;
import com.lying.init.CDTerms;
import com.lying.init.CDTiles;
import com.lying.worldgen.TileGenerator;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;

public class GrammarTerm
{
	public static final Codec<GrammarTerm> CODEC	= RecordCodecBuilder.create(instance -> instance.group(
			Identifier.CODEC.fieldOf("id").forGetter(GrammarTerm::registryName),
			Codec.INT.fieldOf("weight").forGetter(GrammarTerm::weight),
			Codec.INT.fieldOf("colour").forGetter(GrammarTerm::colour),
			RoomContent.CODEC.fieldOf("content").forGetter(t -> t.contentBuilder),
			PhraseModifier.CODEC.optionalFieldOf("phrase_modifier").forGetter(t -> t.modifier.isBlank() ? Optional.empty() : Optional.of(t.modifier)),
			SizeFunction.CODEC.fieldOf("size").forGetter(t -> t.sizeFunc),
			Codec.BOOL.optionalFieldOf("is_placeable").forGetter(t -> t.isPlaceable() ? Optional.of(true) : Optional.empty()),
			Codec.BOOL.optionalFieldOf("is_replaceable").forGetter(t -> t.isReplaceable() ? Optional.of(true) : Optional.empty()),
			TermConditions.CODEC.fieldOf("conditions").forGetter(t -> t.conditions)
			).apply(instance, (id, weight, colour, content, modifier, sizeFunc, placeable, replaceable, condition) -> 
			{
				return new GrammarTerm(
						id, 
						weight, 
						colour, 
						content, 
						modifier.orElse(PhraseModifier.NOOP.get()), 
						sizeFunc, 
						placeable.orElse(false), 
						replaceable.orElse(false), 
						condition);
			}));
	
	private final Identifier registryName;
	private final int colour;
	private final int weight;
	private final boolean isReplaceable, isPlaceable;
	private final TermConditions conditions;
	
	private final RoomContent contentBuilder;
	private final SizeFunction sizeFunc;
	private final PhraseModifier modifier;
	
	private GrammarTerm(
			Identifier idIn, 
			int weightIn, 
			int colourIn, 
			RoomContent processorIn,
			PhraseModifier applyFuncIn,
			SizeFunction sizeFuncIn, 
			boolean placeable, 
			boolean replaceable, 
			TermConditions conditionsIn)
	{
		registryName = idIn;
		weight = weightIn;
		colour = colourIn;
		contentBuilder = processorIn;
		modifier = applyFuncIn;
		sizeFunc = sizeFuncIn;
		conditions = conditionsIn;
		isPlaceable = placeable;
		isReplaceable = replaceable;
	}
	
	public final Identifier registryName() { return registryName; }
	
	public final JsonElement writeToJson(JsonOps ops)
	{
		return CODEC.encodeStart(ops, this).getOrThrow();
	}
	
	public static GrammarTerm readFromJson(JsonOps ops, JsonElement ele)
	{
		return CODEC.parse(ops, ele).getOrThrow();
	}
	
	public final int colour() { return colour; }
	
	public final int weight() { return weight; }
	
	public MutableText name() { return Text.literal(registryName.getPath()); }
	
	public boolean matches(GrammarTerm b) { return registryName.equals(b.registryName); }
	
	/** Returns true if generation should replace rooms with this Term */
	public boolean isReplaceable() { return isReplaceable || registryName.equals(CDTerms.ID_BLANK); }
	
	/** Returns true if generation can place this kind of room */
	public boolean isPlaceable() { return isPlaceable && !(registryName.equals(CDTerms.ID_START) || registryName.equals(CDTerms.ID_END)); }
	
	/** Returns true if this Term can exist in the given room */
	public final boolean canBePlaced(GrammarRoom inRoom, @NotNull List<GrammarRoom> previous, @NotNull List<GrammarRoom> next, GrammarPhrase graph)
	{
		return (!modifier.isBranchInjector() || inRoom.canAddLink()) && conditions.test(this, inRoom, previous, next, graph);
	}
	
	public boolean generate(BlockPos position, ServerWorld world, BlueprintRoom node, List<BlueprintPassage> passages)
	{
		BlueprintTileGrid map = BlueprintTileGrid.fromGraphGrid(node.tileGrid(), Blueprint.ROOM_TILE_HEIGHT);
		RoomMetadata meta = node.metadata();
		
		try
		{
			// Pre-seed doorways to connecting rooms
			preseedDoorways(node, map, passages);
			
			contentBuilder.applyPreProcessing(node, meta, map, world);
			
			// Fill rest of tileset with WFC generation
			TileGenerator.generate(map, node.metadata().theme().getTileSet(this), world.getRandom());
		}
		catch(Exception e) { }
		
		map.finalise(meta.theme());
		
		if(map.generate(position, world))
		{
			Box box = node.worldBox().offset(position);
			BlockPos min = new BlockPos((int)box.minX, (int)box.minY, (int)box.minZ);
			BlockPos max = new BlockPos((int)box.maxX, (int)box.maxY, (int)box.maxZ);
			contentBuilder.applyPostProcessing(min, max, world, node, meta);
			return true;
		}
		return false;
	}
	
	protected static void preseedDoorways(BlueprintRoom node, BlueprintTileGrid map, List<BlueprintPassage> passages)
	{
		/**
		 * Find all passages that connect to the given room
		 * Convert those passages to tile grids
		 * Mark any tile in the given room adjacent to a tile in the passages as PASSAGE
		 * 
		 * This improves room navigability by reducing occlusion of doorways
		 */
		final List<GraphTileGrid> connectingPassages = passages.stream().filter(p -> p.isTerminus(node)).map(BlueprintPassage::asTiles).toList();
		map.getBoundaries(Direction.Type.HORIZONTAL.stream().toList()).stream()
			.filter(t -> 
			{
				GridTile tile = new GridTile(t.getX(), t.getZ());
				return connectingPassages.stream().anyMatch(g -> g.containsAdjacent(tile));
			})
			.forEach(t -> map.put(t.withY(1), CDTiles.instance().get(CDTiles.ID_PASSAGE_FLAG).orElse(CDTiles.AIR.get())));
	}
	
	public void applyTo(GrammarRoom room, GrammarPhrase graph)
	{
		room.metadata().setType(this);
		onApply(room, graph);
	}
	
	public void onApply(GrammarRoom room, GrammarPhrase graph)
	{
		modifier.apply(this, room, graph);
	}
	
	public void prepare(RoomMetadata metadata, Random rand)
	{
		metadata.setSize(sizeFunc.apply(rand));
	}
	
	public static class Builder
	{
		private final int colour;
		private int weight = 1;
		private boolean replaceable = false;
		private boolean placeable = true;
		private TermConditions condition = TermConditions.create();
		private PhraseModifier applyFunc = PhraseModifier.NOOP.get();
		
		private RoomContent processor = RoomContent.NOOP.get();
		private SizeFunction sizeFunc = new SizeFunction(3, 6, 3, 6);
		
		private Builder(int colourIn)
		{
			colour = colourIn;
		}
		
		public static Builder create(int colour)
		{
			return new Builder(colour);
		}
		
		public Builder unplaceable()
		{
			placeable = false;
			return this;
		}
		
		public Builder replaceable()
		{
			replaceable = true;
			return this;
		}
		
		public Builder weight(int val)
		{
			weight = val;
			return this;
		}
		
		public Builder size(int minX, int maxX, int minY, int maxY)
		{
			sizeFunc = new SizeFunction(minX, maxX, minY, maxY);
			return this;
		}
		
		public Builder size(Vector2i vec)
		{
			sizeFunc = new SizeFunction(vec.x, vec.x, vec.y, vec.y);
			return this;
		}
		
		public Builder withCondition(TermConditions conditionIn)
		{
			condition = conditionIn;
			return this;
		}
		
		public Builder onApply(PhraseModifier funcIn)
		{
			applyFunc = funcIn;
			return this;
		}
		
		public Builder setContent(RoomContent processorIn)
		{
			processor = processorIn;
			return this;
		}
		
		public GrammarTerm build(Identifier registryName)
		{
			return new GrammarTerm(registryName, weight, colour, processor, applyFunc, sizeFunc, placeable, replaceable, condition);
		}
	}
}
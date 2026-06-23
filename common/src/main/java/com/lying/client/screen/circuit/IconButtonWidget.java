package com.lying.client.screen.circuit;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class IconButtonWidget extends ButtonWidget
{
	private static final Identifier[] TEXTURES = new Identifier[]
			{
				Identifier.ofVanilla("widget/button"), 
				Identifier.ofVanilla("widget/button_disabled"), 
				Identifier.ofVanilla("widget/button_highlighted")
			};
	private final Identifier texture;
	private int color = -1;
	
	protected IconButtonWidget(int x, int y, Text message, Identifier texture, ButtonWidget.PressAction onPress)
	{
		super(x, y, 22, 22, message, onPress, ButtonWidget.DEFAULT_NARRATION_SUPPLIER);
		this.texture = texture;
	}
	
	protected IconButtonWidget(int x, int y, Identifier texture, ButtonWidget.PressAction onPress)
	{
		this(x, y, ScreenTexts.EMPTY, texture, onPress);
	}
	
	public IconButtonWidget color(int colorIn)
	{
		color = colorIn;
		return this;
	}
	
	public int iconColor() { return color; }
	
	public void renderWidget(DrawContext context, int mouseX, int mouseY, float delta)
	{
		Identifier identifier = TEXTURES[!this.active ? 1 : this.isSelected() ? 2 : 0];
		context.drawGuiTexture(RenderLayer::getGuiTextured, identifier, this.getX(), this.getY(), this.width, this.height);
		context.drawTexture(
				RenderLayer::getGuiTextured, 
				this.texture, 
				this.getX() + (width / 2) - 8, 
				this.getY() + (width / 2) - 8, 
				0, 
				0, 
				16, 
				16, 
				16, 
				16, 
				iconColor());
	}
}
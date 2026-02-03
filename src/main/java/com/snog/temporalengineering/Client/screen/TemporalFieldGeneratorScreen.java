package com.snog.temporalengineering.client.screen;

import com.mojang.blaze3d.vertex.PoseStack;
import com.snog.temporalengineering.common.menu.TemporalFieldGeneratorMenu;
import com.snog.temporalengineering.common.network.ModNetwork;
import com.snog.temporalengineering.common.network.ToggleFieldAreaPacket;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class TemporalFieldGeneratorScreen extends net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<TemporalFieldGeneratorMenu>
{
    private Button toggleButton;

    public TemporalFieldGeneratorScreen(TemporalFieldGeneratorMenu menu, Inventory inv, Component title)
    {
        super(menu, inv, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
    }

    @Override
    protected void init()
    {
        super.init();

        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;

        this.toggleButton = new Button(
            x + 20,
            y + 60,
            136,
            20,
            getToggleLabel(),
            btn ->
            {
                boolean next = !menu.getShowArea();
                ModNetwork.CHANNEL.sendToServer(new ToggleFieldAreaPacket(menu.getBlockEntity().getBlockPos(), next));
            }
        );

        this.addRenderableWidget(this.toggleButton);
    }

    private Component getToggleLabel()
    {
        return Component.literal(menu.getShowArea() ? "Hide Area" : "Show Area");
    }

    @Override
    public void containerTick()
    {
        super.containerTick();

        if (this.toggleButton != null)
        {
            this.toggleButton.setMessage(getToggleLabel());
        }
    }

    @Override
    protected void renderBg(PoseStack ms, float partialTicks, int mouseX, int mouseY)
    {
        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;

        GuiComponent.fill(ms, x, y, x + imageWidth, y + imageHeight, 0xFF202020);

        font.draw(ms, "Volatile EM Fuel Slot", x + 20, y + 20, 0xCCCCCC);
        font.draw(ms, "Toggle renders a cosmetic boundary outline.", x + 20, y + 90, 0x888888);
    }

    @Override
    public void render(PoseStack ms, int mouseX, int mouseY, float partialTicks)
    {
        this.renderBackground(ms);
        super.render(ms, mouseX, mouseY, partialTicks);
        this.renderTooltip(ms, mouseX, mouseY);
    }
}
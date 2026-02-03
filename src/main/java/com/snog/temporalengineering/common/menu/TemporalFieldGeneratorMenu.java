package com.snog.temporalengineering.common.menu;

import com.snog.temporalengineering.common.blockentity.TemporalFieldGeneratorBlockEntity;
import com.snog.temporalengineering.common.registry.ModBlocks;
import com.snog.temporalengineering.common.registry.ModMenuTypes;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.SlotItemHandler;

import java.util.Objects;

public class TemporalFieldGeneratorMenu extends AbstractContainerMenu
{
    private final TemporalFieldGeneratorBlockEntity blockEntity;
    private final ContainerData data;

    public TemporalFieldGeneratorMenu(int id, Inventory playerInv, FriendlyByteBuf buf)
    {
        this(id, playerInv,
            (TemporalFieldGeneratorBlockEntity) Objects.requireNonNull(
                playerInv.player.level.getBlockEntity(buf.readBlockPos()),
                "BlockEntity not found for TemporalFieldGeneratorMenu"
            )
        );
    }

    public TemporalFieldGeneratorMenu(int id, Inventory playerInv, TemporalFieldGeneratorBlockEntity be)
    {
        super(ModMenuTypes.TEMPORAL_FIELD_GENERATOR_MENU.get(), id);
        this.blockEntity = be;

        IItemHandler items = be.getItemHandlerView();

        // Slot 0: fuel slot
        this.addSlot(new SlotItemHandler(items, 0, 80, 35));

        // Player inventory
        int leftCol = 8;
        int topRow = 84;

        for (int row = 0; row < 3; row++)
        {
            for (int col = 0; col < 9; col++)
            {
                this.addSlot(new Slot(playerInv, col + row * 9 + 9, leftCol + col * 18, topRow + row * 18));
            }
        }

        // Hotbar
        for (int col = 0; col < 9; col++)
        {
            this.addSlot(new Slot(playerInv, col, 8 + col * 18, 142));
        }

        // Data: 0 = showArea (0/1)
        this.data = new ContainerData()
        {
            @Override
            public int get(int index)
            {
                if (index == 0)
                {
                    return blockEntity.getShowArea() ? 1 : 0;
                }
                return 0;
            }

            @Override
            public void set(int index, int value)
            {
                // Client-side data sink; server is authoritative
            }

            @Override
            public int getCount()
            {
                return 1;
            }
        };

        this.addDataSlots(this.data);
    }

    @Override
    public boolean stillValid(Player player)
    {
        if (this.blockEntity == null || player == null)
        {
            return false;
        }

        var level = this.blockEntity.getLevel();
        if (level == null)
        {
            return false;
        }

        var pos = this.blockEntity.getBlockPos();
        boolean inRange = player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= 64.0;
        boolean isSameBlock = level.getBlockState(pos).is(ModBlocks.TEMPORAL_FIELD_GENERATOR.get());
        return inRange && isSameBlock;
    }

    public boolean getShowArea()
    {
        return this.data.get(0) != 0;
    }

    public TemporalFieldGeneratorBlockEntity getBlockEntity()
    {
        return blockEntity;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index)
    {
        // MVP: implement later; safe default
        return ItemStack.EMPTY;
    }
}
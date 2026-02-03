package com.snog.temporalengineering.common.blockentity;

import com.snog.temporalengineering.api.TemporalAdapterRegistry;
import com.snog.temporalengineering.common.config.TemporalConfig;
import com.snog.temporalengineering.common.menu.TemporalFieldGeneratorMenu;
import com.snog.temporalengineering.common.registry.ModBlockEntities;
import com.snog.temporalengineering.common.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class TemporalFieldGeneratorBlockEntity extends BlockEntity implements MenuProvider
{
    // inventory: one slot for Volatile Exotic Matter ONLY
    private final ItemStackHandler itemHandler = new ItemStackHandler(1)
    {
        @Override
        public boolean isItemValid(int slot, @Nonnull ItemStack stack)
        {
            return stack.getItem() == ModItems.VOLATILE_EXOTIC_MATTER.get();
        }

        @Override
        protected void onContentsChanged(int slot)
        {
            setChanged();
        }
    };

    private final LazyOptional<IItemHandler> itemHandlerCap = LazyOptional.of(() -> itemHandler);

    // UI toggle: always show faint outline when enabled (Choice B)
    private boolean showArea = false;

    public TemporalFieldGeneratorBlockEntity(BlockPos pos, BlockState state)
    {
        super(ModBlockEntities.TEMPORAL_FIELD_GENERATOR.get(), pos, state);
    }

    // ======== TICK ========

    public static void serverTick(Level level, BlockPos pos, BlockState state, TemporalFieldGeneratorBlockEntity be)
    {
        if (level.isClientSide)
        {
            return;
        }

        int consumeInterval = TemporalConfig.GENERATOR_CONSUME_INTERVAL_TICKS.get();
        if (consumeInterval <= 0)
        {
            return;
        }

        if (level.getGameTime() % consumeInterval != 0)
        {
            return;
        }

        ItemStack stack = be.itemHandler.getStackInSlot(0);
        if (!stack.isEmpty() && stack.getItem() == ModItems.VOLATILE_EXOTIC_MATTER.get())
        {
            ItemStack extracted = be.itemHandler.extractItem(0, 1, false);
            if (!extracted.isEmpty())
            {
                int radius = TemporalConfig.FIELD_RADIUS.get();
                float speed = TemporalConfig.GENERATOR_SPEED_MULTIPLIER.get().floatValue();
                int duration = TemporalConfig.GENERATOR_EFFECT_DURATION_TICKS.get();

                applySpeedToNearby(level, pos, radius, speed, duration);

                // FX pulse (config-gated)
                if (TemporalConfig.FX_GENERATOR_PARTICLES_ENABLED.get())
                {
                    spawnFieldPulseParticles(level, pos, radius);
                }

                if (TemporalConfig.FX_GENERATOR_SOUND_ENABLED.get())
                {
                    level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 0.6f, 1.2f);
                }

                be.setChanged();
                be.syncToClient();
            }
        }
    }

    private static void applySpeedToNearby(Level level, BlockPos center, int radius, float speed, int duration)
    {
        int r = Math.max(0, radius);

        for (int dx = -r; dx <= r; dx++)
        {
            for (int dy = -r; dy <= r; dy++)
            {
                for (int dz = -r; dz <= r; dz++)
                {
                    BlockPos check = center.offset(dx, dy, dz);

                    if (!level.isLoaded(check))
                    {
                        continue;
                    }

                    BlockEntity target = level.getBlockEntity(check);
                    if (target == null)
                    {
                        continue;
                    }

                    // Centralized application via registry (covers native + adapters)
                    TemporalAdapterRegistry.tryApplyWithSource(target, speed, duration, "Field");
                }
            }
        }
    }

    private static void spawnFieldPulseParticles(Level level, BlockPos pos, int radius)
    {
        // TEMP: random sparks near center (we'll replace with bubble ring + meridians next)
        for (int i = 0; i < 16; i++)
        {
            double ox = pos.getX() + 0.5 + (level.random.nextDouble() - 0.5) * (radius / 2.0);
            double oy = pos.getY() + 1.0 + level.random.nextDouble() * 0.25;
            double oz = pos.getZ() + 0.5 + (level.random.nextDouble() - 0.5) * (radius / 2.0);

            level.addParticle(ParticleTypes.ELECTRIC_SPARK, ox, oy, oz, 0.0, 0.02, 0.0);
        }
    }

    // ======== UI TOGGLE ========

    public boolean getShowArea()
    {
        return showArea;
    }

    public void setShowArea(boolean value)
    {
        this.showArea = value;
        setChanged();
        syncToClient();
    }

    private void syncToClient()
    {
        if (this.level != null && !this.level.isClientSide)
        {
            this.level.sendBlockUpdated(this.worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    // ======== MENU PROVIDER ========

    @Override
    public Component getDisplayName()
    {
        return Component.literal("Temporal Field Generator");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory playerInventory, Player player)
    {
        return new TemporalFieldGeneratorMenu(id, playerInventory, this);
    }

    // ======== NBT / NETWORK SYNC ========

    @Override
    public void load(CompoundTag tag)
    {
        super.load(tag);

        if (tag.contains("Inventory"))
        {
            itemHandler.deserializeNBT(tag.getCompound("Inventory"));
        }

        this.showArea = tag.getBoolean("ShowArea");
    }

    @Override
    protected void saveAdditional(CompoundTag tag)
    {
        super.saveAdditional(tag);

        tag.put("Inventory", itemHandler.serializeNBT());
        tag.putBoolean("ShowArea", this.showArea);
    }

    @Override
    public CompoundTag getUpdateTag()
    {
        return this.saveWithoutMetadata();
    }

    @Nullable
    @Override
    public Packet<?> getUpdatePacket()
    {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void onDataPacket(net.minecraft.network.Connection net, ClientboundBlockEntityDataPacket pkt)
    {
        CompoundTag tag = pkt.getTag();
        if (tag != null)
        {
            this.load(tag);
        }
    }

    // ======== CAPABILITIES ========

    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability(@Nonnull net.minecraftforge.common.capabilities.Capability<T> cap, @Nullable Direction side)
    {
        if (cap == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY)
        {
            return itemHandlerCap.cast();
        }

        return super.getCapability(cap, side);
    }

    @Override
    public void setRemoved()
    {
        super.setRemoved();
        itemHandlerCap.invalidate();
    }

    public IItemHandler getItemHandlerView()
    {
        return itemHandler;
    }
}
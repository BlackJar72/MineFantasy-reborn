package minefantasy.mfr.container;

import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import minefantasy.mfr.block.tile.TileEntityResearch;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

public class ContainerResearch extends Container {
    private TileEntityResearch tile;
    private boolean isGuiContainer = false;
    private int lastProgress;
    private int lastProgressMax;

    public ContainerResearch(InventoryPlayer user, TileEntityResearch tile) {
        isGuiContainer = true;
        this.tile = tile;

        this.addSlotToContainer(new Slot(tile, 0, 83, 40));

        int i;

        for (i = 0; i < 3; ++i) {
            for (int j = 0; j < 9; ++j) {
                this.addSlotToContainer(new Slot(user, j + i * 9 + 9, 10 + j * 18, 76 + i * 18));
            }
        }

        for (i = 0; i < 9; ++i) {
            this.addSlotToContainer(new Slot(user, i, 10 + i * 18, 134));
        }
    }

    @Override
    public void detectAndSendChanges() {
        for (int i = 0; i < this.listeners.size(); ++i) {
            Container icrafting = (Container) this.listeners.get(i);

            if (this.lastProgress != tile.progress) {
                icrafting.updateProgressBar( 0, (int) tile.progress);
            }
            if (this.lastProgressMax != tile.maxProgress) {
                icrafting.updateProgressBar( 1, (int) tile.maxProgress);
            }
        }
        this.lastProgress = (int) tile.progress;
        this.lastProgressMax = (int) tile.maxProgress;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void updateProgressBar(int id, int value) {
        if (id == 0) {
            tile.progress = value;
        }
        if (id == 1) {
            tile.maxProgress = value;
        }
    }

    @Override
    public boolean canInteractWith(EntityPlayer player) {
        return this.tile.isUsableByPlayer(player);
    }

    @Override
    public ItemStack transferStackInSlot(EntityPlayer user, int clicked) {
        ItemStack itemstack = null;
        Slot slot = (Slot) this.inventorySlots.get(clicked);

        if (slot != null && slot.getHasStack()) {
            ItemStack itemstack1 = slot.getStack();
            itemstack = itemstack1.copy();

            if (clicked > 0)// INVENTORY
            {
                if (TileEntityResearch.canAccept(itemstack1)) {
                    if (!this.mergeItemStack(itemstack1, 0, 1, false)) {
                        return null;
                    }
                } else if (clicked >= 1 && clicked < 28)// INVENTORY
                {
                    if (!this.mergeItemStack(itemstack1, 28, 37, false)) {
                        return null;
                    }
                }
                // BAR
                else if (clicked >= 28 && clicked < 37 && !this.mergeItemStack(itemstack1, 1, 28, false)) {
                    return null;
                }
            } else if (!this.mergeItemStack(itemstack1, 1, 37, false)) {
                return null;
            }

            if (itemstack1.getCount() == 0) {
                slot.putStack((ItemStack) null);
            } else {
                slot.onSlotChanged();
            }

            if (itemstack1.getCount() == itemstack.getCount()) {
                return null;
            }

            slot.onTake(user, itemstack1);
        }

        return itemstack;
    }
}
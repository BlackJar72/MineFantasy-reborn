package minefantasy.mfr.block.tile;

import minefantasy.mfr.api.crafting.IBasicMetre;
import minefantasy.mfr.api.knowledge.IArtefact;
import minefantasy.mfr.api.knowledge.InformationBase;
import minefantasy.mfr.api.knowledge.ResearchArtefacts;
import minefantasy.mfr.api.knowledge.ResearchLogic;
import minefantasy.mfr.init.ComponentListMFR;
import minefantasy.mfr.init.SoundsMFR;
import minefantasy.mfr.proxy.NetworkUtils;
import minefantasy.mfr.packet.ResearchTablePacket;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.WorldServer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

public class TileEntityResearch extends TileEntity implements IInventory, IBasicMetre {
    public float progress;
    public float maxProgress;
    public int researchID = -1;
    private ItemStack[] items = new ItemStack[1];
    private Random rand = new Random();
    private int ticksExisted;

    public static ArrayList<String> getInfo(ItemStack item) {
        if (item == null) {
            return null;
        }

        return ResearchArtefacts.getResearchNames(item);
    }

    public static boolean canAccept(ItemStack item) {
        ArrayList<String> info = getInfo(item);
        return info != null && info.size() > 0;
    }

    public boolean interact(EntityPlayer user) {
        if (world.isRemote) {
            return true;
        }
        ArrayList<String> research = this.getInfo(items[0]);
        int result = canResearch(user, research);

        if (research != null && research.size() > 0 && result != 0) {
            if (result == -1) {
                if (!user.world.isRemote)
                    user.sendMessage(new TextComponentString("research.noskill" + Arrays.toString(new Object[0])));
                return true;
            }
            maxProgress = getMaxTime();
            if (maxProgress > 0) {
                addProgress(user);

                if (progress >= maxProgress) {
                    addResearch(research, user);
                    progress = 0;
                }

                return true;
            }
        } else {
            if (result == 0) {
                if (!user.world.isRemote)
                    user.sendMessage(new TextComponentString("research.null" + Arrays.toString(new Object[0])));
            }
            progress = 0;
        }

        return items[0] != null;
    }

    private void addResearch(ArrayList<String> research, EntityPlayer user) {
        for (int id = 0; id < research.size(); id++) {
            InformationBase base = ResearchLogic.getResearch(research.get(id));
            if (base != null && !ResearchLogic.alreadyUsedArtefact(user, base, items[0])
                    && ResearchLogic.canPurchase(user, base) && base.hasSkillsUnlocked(user)) {
                int artefacts = ResearchArtefacts.useArtefact(items[0], base, user);
                world.playSound(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, SoundsMFR.UPDATE_RESEARCH, SoundCategory.NEUTRAL, 1.0F, 1.0F, true);
                if (!user.world.isRemote) {
                    Object name = new TextComponentString("knowledge." + base.getUnlocalisedName());
                    if (artefacts == -1) {
                        user.sendMessage(new TextComponentString("research.finishResearch" + name));
                    } else {
                        user.sendMessage(new TextComponentString("research.addArtefact"+ name + artefacts + base.getArtefactCount()));
                    }
                }
                return;
            }

        }
    }

    // 0 nothing, -1 for no skill, 1 for yes
    private int canResearch(EntityPlayer user, ArrayList<String> research) {
        if (research == null) {
            return 0;
        }
        int result = 0;

        for (int id = 0; id < research.size(); id++) {
            InformationBase base = ResearchLogic.getResearch(research.get(id));
            if (base != null && !ResearchLogic.alreadyUsedArtefact(user, base, items[0])) {
                if (ResearchLogic.canPurchase(user, base) && base.hasSkillsUnlocked(user)) {
                    return 1;
                } else if (!ResearchLogic.hasInfoUnlocked(user, base)) {
                    result = -1;
                }
            }
        }
        return result;
    }

    private float getMaxTime() {
        int t = 10;
        if (items[0] != null && items[0].getItem() instanceof IArtefact) {
            return ((IArtefact) items[0].getItem()).getStudyTime(items[0]);
        }
        return this.getInfo(items[0]) != null ? t : 0;
    }

    private void addProgress(EntityPlayer user) {
        ItemStack held = user.getHeldItemMainhand();
        if (held != null && (held.getItem() == ComponentListMFR.talisman_lesser
                || held.getItem() == ComponentListMFR.talisman_greater)) {
            progress = maxProgress;
            if (!user.capabilities.isCreativeMode && held.getItem() == ComponentListMFR.talisman_lesser) {
                held.shrink(1);
                if (held.getCount() <= 0) {
                    user.setItemStackToSlot(EntityEquipmentSlot.MAINHAND, null);
                }
            }
            return;
        }
        float efficiency = 1.0F;
        if (user.swingProgress > 0) {
            efficiency *= Math.max(0F, 1.0F - user.swingProgress);
        }
        world.playSound(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, SoundsMFR.FLIP_PAGE, SoundCategory.NEUTRAL, 1.0F, rand.nextFloat() * 0.4F + 0.8F, true);
        efficiency *= getEnvironmentBoost();
        progress += efficiency;
    }

    private float getEnvironmentBoost() {
        int books = 0;
        for (int x = -8; x <= 8; x++) {
            for (int y = -8; y <= 8; y++) {
                for (int z = -8; z <= 8; z++) {
                    if (world.getBlockState(pos.add(x,y,z)) == Blocks.BOOKSHELF) {
                        ++books;
                    }
                }
            }
        }
        return 1.0F + (0.1F * books);
    }

    @Override
    public void markDirty() {
        syncData();
    }

    @Override
    public boolean isUsableByPlayer(EntityPlayer user) {
        return user.getDistance(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D) < 8D;
    }

    @Override
    public void openInventory(EntityPlayer player) {

    }

    @Override
    public void closeInventory(EntityPlayer player) {

    }

    public void syncData() {
        if (world.isRemote)
            return;

        NetworkUtils.sendToWatchers(new ResearchTablePacket(this).generatePacket(), (WorldServer) world, this.pos);

		/*
        List<EntityPlayer> players = ((WorldServer) worldObj).playerEntities;
		for (int i = 0; i < players.size(); i++) {
			EntityPlayer player = players.get(i);
			((WorldServer) worldObj).getEntityTracker().func_151248_b(player,
					new ResearchTablePacket(this).generatePacket());
		}
		*/
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
        super.writeToNBT(nbt);

        nbt.setInteger("researchID", researchID);
        nbt.setInteger("ticksExisted", ticksExisted);
        nbt.setFloat("progress", progress);
        nbt.setFloat("maxProgress", maxProgress);
        NBTTagList savedItems = new NBTTagList();

        for (int i = 0; i < this.items.length; ++i) {
            if (this.items[i] != null) {
                NBTTagCompound savedSlot = new NBTTagCompound();
                savedSlot.setByte("Slot", (byte) i);
                this.items[i].writeToNBT(savedSlot);
                savedItems.appendTag(savedSlot);
            }
        }

        nbt.setTag("Items", savedItems);
        return nbt;
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        super.readFromNBT(nbt);

        ticksExisted = nbt.getInteger("ticksExisted");
        researchID = nbt.getInteger("researchID");
        progress = nbt.getFloat("progress");
        maxProgress = nbt.getFloat("maxProgress");
        NBTTagList savedItems = nbt.getTagList("Items", 10);
        this.items = new ItemStack[this.getSizeInventory()];

        for (int i = 0; i < savedItems.tagCount(); ++i) {
            NBTTagCompound savedSlot = savedItems.getCompoundTagAt(i);
            byte slotNum = savedSlot.getByte("Slot");

            if (slotNum >= 0 && slotNum < this.items.length) {
                this.items[slotNum] = new ItemStack(savedSlot);
            }
        }
    }

    @Override
    public int getSizeInventory() {
        return items.length;
    }

    @Override
    public boolean isEmpty() {
        return false;
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        return items[slot];
    }

    @Override
    public ItemStack decrStackSize(int slot, int num) {
        if (this.items[slot] != null) {
            ItemStack itemstack;

            if (this.items[slot].getCount() <= num) {
                itemstack = this.items[slot];
                this.items[slot] = null;
                return itemstack;
            } else {
                itemstack = this.items[slot].splitStack(num);

                if (this.items[slot].getCount() == 0) {
                    this.items[slot] = null;
                }

                return itemstack;
            }
        } else {
            return null;
        }
    }

    @Override
    public ItemStack removeStackFromSlot(int slot) {
        return items[slot];
    }

    @Override
    public void setInventorySlotContents(int slot, ItemStack item) {
        items[slot] = item;
    }

    @Override
    public int getInventoryStackLimit() {
        return 64;
    }


    @Override
    public boolean isItemValidForSlot(int slot, ItemStack item) {
        return canAccept(item);
    }

    @Override
    public int getField(int id) {
        return 0;
    }

    @Override
    public void setField(int id, int value) {

    }

    @Override
    public int getFieldCount() {
        return 0;
    }

    @Override
    public void clear() {

    }

    @Override
    public int getMetreScale(int size) {
        if (maxProgress <= 0) {
            return 0;
        }
        return (int) Math.min(size, Math.ceil(size / maxProgress * progress));
    }

    @Override
    public boolean shouldShowMetre() {
        return maxProgress > 0;
    }

    @Override
    public String getLocalisedName() {
        return "";
    }

    @Override
    public String getName() {
        return null;
    }

    @Override
    public boolean hasCustomName() {
        return false;
    }
}

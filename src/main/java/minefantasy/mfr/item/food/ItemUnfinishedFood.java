package minefantasy.mfr.item.food;

import minefantasy.mfr.init.CreativeTabMFR;
import minefantasy.mfr.init.ToolListMFR;
import minefantasy.mfr.item.ItemBaseMFR;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.EnumRarity;
import net.minecraft.item.ItemStack;

/**
 * @author Anonymous Productions
 */
public class ItemUnfinishedFood extends ItemBaseMFR {
    private int itemRarity;

    public ItemUnfinishedFood(String name) {
        this(name, 0);
    }

    public ItemUnfinishedFood(String name, int rarity) {
        super(name);
        setMaxStackSize(1);
        itemRarity = rarity;

        this.setCreativeTab(CreativeTabMFR.tabFood);
    }

    @Override
    public EnumRarity getRarity(ItemStack item) {
        int lvl = itemRarity + 1;

        if (item.isItemEnchanted()) {
            if (lvl == 0) {
                lvl++;
            }
            lvl++;
        }
        if (lvl >= ToolListMFR.RARITY.length) {
            lvl = ToolListMFR.RARITY.length - 1;
        }
        return ToolListMFR.RARITY[lvl];
    }

    public void onCrafted(EntityPlayer user, ItemStack item) {
    }
}

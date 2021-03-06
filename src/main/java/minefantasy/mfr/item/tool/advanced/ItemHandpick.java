package minefantasy.mfr.item.tool.advanced;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import minefantasy.mfr.MineFantasyReborn;
import minefantasy.mfr.api.helpers.CustomToolHelper;
import minefantasy.mfr.api.material.CustomMaterial;
import minefantasy.mfr.api.mining.RandomOre;
import minefantasy.mfr.api.tier.IToolMaterial;
import minefantasy.mfr.config.ConfigTools;
import minefantasy.mfr.init.CreativeTabMFR;
import minefantasy.mfr.proxy.IClientRegister;
import minefantasy.mfr.util.ModelLoaderHelper;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.EnumRarity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemPickaxe;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.oredict.OreDictionary;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

/**
 * @author Anonymous Productions
 */
public class ItemHandpick extends ItemPickaxe implements IToolMaterial, IClientRegister {
    protected int itemRarity;
    private String name;
    private float baseDamage = 1F;
    // ===================================================== CUSTOM START
    // =============================================================\\
    private boolean isCustom = false;
    private float efficiencyMod = 1.0F;

    public ItemHandpick(String name, ToolMaterial material, int rarity) {
        super(material);
        itemRarity = rarity;
        setCreativeTab(CreativeTabMFR.tabOldTools);
        this.name = name;
        setRegistryName(name);
        setUnlocalizedName(name);

        this.setUnlocalizedName(name);
        setMaxDamage(material.getMaxUses());

        MineFantasyReborn.proxy.addClientRegister(this);
    }

    @Override
    public boolean onBlockDestroyed(ItemStack item, World world, IBlockState blockState, BlockPos pos, EntityLivingBase user) {
        if (!world.isRemote) {
            IBlockState state = world.getBlockState(pos);
            int harvestlvl = this.getMaterial().getHarvestLevel();
            int fortune = EnchantmentHelper.getEnchantmentLevel( Enchantment.getEnchantmentByID(35), item);
            boolean silk = user.getHeldItemMainhand().canHarvestBlock(blockState);

            //double drop logic
            List<ItemStack> drops = blockState.getBlock().getDrops(world, pos, state, ConfigTools.handpickFortune ? fortune : 0);

            if (!silk && drops != null && !drops.isEmpty()) {
                Iterator<ItemStack> list = drops.iterator();
                while (list.hasNext()) {
                    ItemStack drop = list.next();
                    if (isOre(blockState.getBlock(), state) && !drop.isItemEqual(new ItemStack(blockState.getBlock(),1))
                            && !(drop.getItem() instanceof ItemBlock) && world.rand.nextFloat() < getDoubleDropChance()) {
                        dropItem(world, pos, drop.copy());
                    }
                }
            }

            //special drop logic
            ArrayList<ItemStack> specialdrops = RandomOre.getDroppedItems(user, blockState.getBlock(), state.getBlock().getMetaFromState(state), harvestlvl, fortune, silk, pos.getY());

            if (specialdrops != null && !specialdrops.isEmpty()) {
                Iterator list = specialdrops.iterator();

                while (list.hasNext()) {
                    ItemStack newdrop = (ItemStack) list.next();

                    if (newdrop != null) {
                        if (newdrop.getCount() < 1)
                            newdrop.setCount(1);

                        dropItem(world, pos, newdrop);
                    }
                }
            }
        }
        return super.onBlockDestroyed(item, world, blockState, pos, user);
    }

    private boolean isOre(Block block, IBlockState state) {
        if (!block.isToolEffective("pickaxe", state)) {
            return false;
        }
        for (int i : OreDictionary.getOreIDs(new ItemStack(block, 1))) {
            String orename = OreDictionary.getOreName(i);
            if (orename != null && orename.startsWith("ore")) {
                return true;
            }
        }
        return false;
    }

    private void dropItem(World world, BlockPos pos, ItemStack drop) {
        if (world.isRemote)
            return;

        EntityItem dropItem = new EntityItem(world, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, drop);
        dropItem.setPickupDelay(10);
        world.spawnEntity(dropItem);
    }

    private float getDoubleDropChance() {
        return ConfigTools.handpickBonus / 100F;
    }

    @Override
    public ToolMaterial getMaterial() {
        return toolMaterial;
    }

    public ItemHandpick setCustom(String s) {
        canRepair = false;
        isCustom = true;
        return this;
    }

    public ItemHandpick setBaseDamage(float baseDamage) {
        this.baseDamage = baseDamage;
        return this;
    }

    public ItemHandpick setEfficiencyMod(float efficiencyMod) {
        this.efficiencyMod = efficiencyMod;
        return this;
    }

    @Override
    public Multimap<String, AttributeModifier> getAttributeModifiers(EntityEquipmentSlot slot, ItemStack stack) {
        if (slot != EntityEquipmentSlot.MAINHAND) {
            return super.getAttributeModifiers(slot, stack);
        }
        Multimap map = HashMultimap.create();
        map.put(SharedMonsterAttributes.ATTACK_DAMAGE.getName(),
                new AttributeModifier(ATTACK_DAMAGE_MODIFIER, "Weapon modifier", getMeleeDamage(stack), 0));

        return map;
    }

    /**
     * Gets a stack-sensitive value for the melee dmg
     */
    protected float getMeleeDamage(ItemStack item) {
        return baseDamage + CustomToolHelper.getMeleeDamage(item, toolMaterial.getAttackDamage());
    }

    protected float getWeightModifier(ItemStack stack) {
        return CustomToolHelper.getWeightModifier(stack, 1.0F);
    }

    @Override
    public int getMaxDamage(ItemStack stack) {
        return CustomToolHelper.getMaxDamage(stack, super.getMaxDamage(stack)) / 2;
    }

    public ItemStack construct(String main, String haft) {
        return CustomToolHelper.construct(this, main, haft);
    }

    @Override
    public EnumRarity getRarity(ItemStack item) {
        return CustomToolHelper.getRarity(item, itemRarity);
    }

    public float getDigSpeed(ItemStack stack, Block block, World world, BlockPos pos, EntityPlayer player) {
        if (!ForgeHooks.isToolEffective(world, pos, stack)) {
            return this.getDestroySpeed(stack, block);
        }
        float digSpeed = player.getDigSpeed(block.getDefaultState(), pos);
        return CustomToolHelper.getEfficiency(stack, digSpeed, efficiencyMod / 10);
    }

    public float getDestroySpeed(ItemStack stack, Block block) {
        return block.getMaterial(block.getDefaultState()) != Material.IRON && block.getMaterial(block.getDefaultState()) != Material.ANVIL
                && block.getMaterial(block.getDefaultState()) != Material.ROCK ? super.getDestroySpeed(stack, block.getDefaultState())
                : CustomToolHelper.getEfficiency(stack, this.efficiency, efficiencyMod / 2);
    }

    @Override
    public int getHarvestLevel(ItemStack stack, String toolClass, @Nullable EntityPlayer player, @Nullable IBlockState blockState) {
        return CustomToolHelper.getHarvestLevel(stack, super.getHarvestLevel(stack, toolClass, player, blockState));
    }

    @Override
    public void getSubItems(CreativeTabs tab, NonNullList<ItemStack> items) {
        if (!isInCreativeTab(tab)) {
            return;
        }
        if (isCustom) {
            ArrayList<CustomMaterial> metal = CustomMaterial.getList("metal");
            Iterator iteratorMetal = metal.iterator();
            while (iteratorMetal.hasNext()) {
                CustomMaterial customMat = (CustomMaterial) iteratorMetal.next();
                if (MineFantasyReborn.isDebug() || customMat.getItem() != null) {
                    items.add(this.construct(customMat.name, "OakWood"));
                }
            }
        } else {
            super.getSubItems(tab, items);
        }
    }

    @Override
    public void addInformation(ItemStack item, World world, List list, ITooltipFlag flag) {
        if (isCustom) {
            CustomToolHelper.addInformation(item, list);
        }
        super.addInformation(item, world, list, flag);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public String getItemStackDisplayName(ItemStack item) {
        String unlocalName = this.getUnlocalizedNameInefficiently(item) + ".name";
        return CustomToolHelper.getLocalisedName(item, unlocalName);
    }

    @Override
    public void registerClient() {
        ModelLoaderHelper.registerItem(this);
    }

    // ====================================================== CUSTOM END
    // ==============================================================\\
}

package minefantasy.mfr.item.tool.advanced;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import minefantasy.mfr.MineFantasyReborn;
import minefantasy.mfr.api.helpers.CustomToolHelper;
import minefantasy.mfr.api.material.CustomMaterial;
import minefantasy.mfr.api.tier.IToolMaterial;
import minefantasy.mfr.config.ConfigTools;
import minefantasy.mfr.init.CreativeTabMFR;
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
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.EnumRarity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemSpade;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

/**
 * @author Anonymous Productions
 */
public class ItemHvyShovel extends ItemSpade implements IToolMaterial {
    protected int itemRarity;
    private String name;
    private float baseDamage = 2F;
    private Random rand = new Random();
    // ===================================================== CUSTOM START
    // =============================================================\\
    private boolean isCustom = false;
    private float efficiencyMod = 1.0F;

    public ItemHvyShovel(String name, ToolMaterial material, int rarity) {
        super(material);
        itemRarity = rarity;
        setCreativeTab(CreativeTabMFR.tabOldTools);
        this.name = name;
        setRegistryName(name);
        setUnlocalizedName(MineFantasyReborn.MOD_ID + "." + name);
        GameRegistry.findRegistry(Item.class).register(this);
        setMaxDamage(material.getMaxUses());
    }

    @Override
    public boolean onBlockDestroyed(ItemStack item, World world, IBlockState state, BlockPos pos, EntityLivingBase user) {
        if (!world.isRemote && ForgeHooks.isToolEffective(world, pos, item)
                && ItemLumberAxe.canAcceptCost(user)) {
            int range = 2;
            for (int x1 = -range; x1 <= range; x1++) {
                // for(int y1 = -1; y1 <= 1; y1 ++)
                {
                    for (int z1 = -range; z1 <= range; z1++) {
                        if (getDistance(pos.getX() + x1, pos.getY(), pos.getZ() + z1, pos.getX() ,pos.getY() ,pos.getZ() ) <= range * 1 + 0.5D) {
                            EnumFacing facing = getFacingFor(user, pos);
                            BlockPos blockPos = pos.add(x1 + facing.getFrontOffsetX(), facing.getFrontOffsetY(), z1 + facing.getFrontOffsetZ());

                            if (!(x1 + facing.getFrontOffsetX() == 0 && facing.getFrontOffsetY() == 0 && z1 + facing.getFrontOffsetZ() == 0)) {
                                IBlockState newblock = world.getBlockState(pos);
                                IBlockState above = world.getBlockState(pos.add(0,1,0));

                                if ((above == null || !above.getMaterial().isSolid())
                                        && newblock != null && user instanceof EntityPlayer
                                        && ForgeHooks.canHarvestBlock(newblock.getBlock(), (EntityPlayer) user, world, pos)
                                        && ForgeHooks.isToolEffective(world, pos, item)) {

                                    if (rand.nextFloat() * 100F < (100F - ConfigTools.hvyDropChance)) {
                                        newblock.getBlock().dropBlockAsItem(world, blockPos, state, EnchantmentHelper.getEnchantmentLevel(Enchantment.getEnchantmentByID(35), item));
                                    }
                                    world.setBlockToAir(pos);
                                    item.damageItem(1, user);
                                    ItemLumberAxe.tirePlayer(user, 1F);
                                }
                            }
                        }
                    }
                }
            }
        }
        return super.onBlockDestroyed(item, world,state, pos, user);
    }

    public double getDistance(double x, double y, double z, int posX, int posY, int posZ) {
        double var7 = posX - x;
        double var9 = posY - y;
        double var11 = posZ - z;
        return MathHelper.sqrt(var7 * var7 + var9 * var9 + var11 * var11);
    }

    private EnumFacing getFacingFor(EntityLivingBase user, BlockPos pos) {
        return EnumFacing.getDirectionFromEntityLiving(pos, user);// TODO: FD
    }

    @Override
    public ToolMaterial getMaterial() {
        return toolMaterial;
    }

    public ItemHvyShovel setCustom(String s) {
        canRepair = false;
        isCustom = true;
        return this;
    }

    public ItemHvyShovel setBaseDamage(float baseDamage) {
        this.baseDamage = baseDamage;
        return this;
    }

    public ItemHvyShovel setEfficiencyMod(float efficiencyMod) {
        this.efficiencyMod = efficiencyMod;
        return this;
    }

    @Override
    public Multimap<String, AttributeModifier> getAttributeModifiers(EntityEquipmentSlot slot, ItemStack item) {
        Multimap map = HashMultimap.create();
        map.put(SharedMonsterAttributes.ATTACK_DAMAGE.getName(),
                new AttributeModifier(UUID.fromString(slot.getName()), "Weapon modifier", getMeleeDamage(item), 0));

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

//    @Override
//    @SideOnly(Side.CLIENT)
//    public int getColorFromItemStack(ItemStack item, int layer) {
//        return CustomToolHelper.getColourFromItemStack(item, layer, super.getColorFromItemStack(item, layer));
//    }

    @Override
    public int getMaxDamage(ItemStack stack) {
        return CustomToolHelper.getMaxDamage(stack, super.getMaxDamage(stack)) * 2;
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
    public void getSubItems(CreativeTabs tab, NonNullList list) {
        if (isCustom) {
            ArrayList<CustomMaterial> metal = CustomMaterial.getList("metal");
            for (CustomMaterial customMat : metal) {
                if (MineFantasyReborn.isDebug() || customMat.getItem() != null) {
                    list.add(this.construct(customMat.name, "OakWood"));
                }
            }
        } else {
            super.getSubItems(tab, list);
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
    // ====================================================== CUSTOM END
    // ==============================================================\\
}

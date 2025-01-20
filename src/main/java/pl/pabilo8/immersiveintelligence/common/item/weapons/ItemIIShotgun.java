package pl.pabilo8.immersiveintelligence.common.item.weapons;

import blusunrize.immersiveengineering.common.util.ItemNBTHelper;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import pl.pabilo8.immersiveintelligence.ImmersiveIntelligence;
import pl.pabilo8.immersiveintelligence.api.utils.tools.IAdvancedZoomTool;
import pl.pabilo8.immersiveintelligence.common.IIConfigHandler.IIConfig.Weapons.Shotgun;
import pl.pabilo8.immersiveintelligence.common.IIContent;
import pl.pabilo8.immersiveintelligence.common.IISounds;
import pl.pabilo8.immersiveintelligence.common.item.ammo.ItemIIBulletMagazine.Magazines;
import pl.pabilo8.immersiveintelligence.common.item.weapons.ItemIIWeaponUpgrade.WeaponUpgrade;
import pl.pabilo8.immersiveintelligence.common.item.weapons.ammohandler.AmmoHandler;
import pl.pabilo8.immersiveintelligence.common.item.weapons.ammohandler.ModifiedAmmoHandlerList;
import pl.pabilo8.immersiveintelligence.common.item.weapons.ammohandler.AmmoHandlerMagazine;
import pl.pabilo8.immersiveintelligence.common.util.AdvancedSounds.RangedSound;
import pl.pabilo8.immersiveintelligence.common.util.easynbt.EasyNBT;
import pl.pabilo8.immersiveintelligence.common.util.item.IICategory;
import pl.pabilo8.immersiveintelligence.common.util.item.IIItemEnum.IIItemProperties;

import javax.annotation.Nullable;

@IIItemProperties(category = IICategory.WARFARE)
public class ItemIIShotgun extends ItemIIGunBase implements IAdvancedZoomTool
{
	//--- NBT Values Reference ---//
	public static final String HANDMADE = "handmade";

	//--- Scope Overlay Textures ---//
	public static final ResourceLocation OVERLAY_SCOPE = new ResourceLocation(ImmersiveIntelligence.MODID,
			"textures/gui/item/machinegun/scope.png");

	//--- Ammunition Handler ---//
	public static final int MAG_SIZE = Shotgun.clipSize;
	public static final int MAG_SIZE_EXTENDED = Shotgun.extendedClipSize + MAG_SIZE;
	private final ModifiedAmmoHandlerList ammoHandler, ammoHandlerExtenededMagazine;
	private final AmmoHandlerMagazine ammoHandlerSemiAuto;

	public ItemIIShotgun()
	{
		super("shotgun");
		ammoHandler = new ModifiedAmmoHandlerList(this, BULLETS, IIContent.itemAmmoShotgun, MAG_SIZE)
		{
			@Nullable
			@Override
			protected SoundEvent getStartLoadingSound(ItemStack weapon, EasyNBT nbt)
			{
				//return IISounds.shotgunLoadStart;
				return null;
			}

			@Nullable
			@Override
			protected SoundEvent getReloadSound(ItemStack weapon, EasyNBT nbt)
			{
				return IISounds.shotgunLoad;
			}

			@Nullable
			@Override
			protected SoundEvent getFinishLoadingSound(ItemStack weapon, EasyNBT nbt)
			{
				return IISounds.shotgunLoadEnd;
			}
		};
		ammoHandlerExtenededMagazine = new ModifiedAmmoHandlerList(this, BULLETS, IIContent.itemAmmoShotgun, MAG_SIZE_EXTENDED)
		{
			@Nullable
			@Override
			protected SoundEvent getStartLoadingSound(ItemStack weapon, EasyNBT nbt)
			{
				//return IISounds.shotgunLoadStart;
				return null;
			}

			@Nullable
			@Override
			protected SoundEvent getReloadSound(ItemStack weapon, EasyNBT nbt)
			{
				return IISounds.shotgunLoad;
			}

			@Nullable
			@Override
			protected SoundEvent getFinishLoadingSound(ItemStack weapon, EasyNBT nbt)
			{
				return IISounds.shotgunLoadEnd;
			}
		};
		ammoHandlerSemiAuto = new AmmoHandlerMagazine(this, MAGAZINE, IIContent.itemAmmoShotgun)
		{
			@Override
			protected boolean isValidType(ItemStack weapon, Magazines magazine)
			{
				return magazine==Magazines.SHOTGUN;
			}

			@Nullable
			@Override
			protected SoundEvent getUnloadSound(ItemStack weapon, EasyNBT nbt)
			{
				return IISounds.rifleUnloadMagazine;
			}

			@Nullable
			@Override
			protected SoundEvent getReloadSound(ItemStack weapon, EasyNBT nbt)
			{
				return IISounds.shotgunLoad;
			}
		};
	}

	@Override
	public int getSlotCount(ItemStack stack)
	{
		return 2;
	}

	@Override
	public void removeFromWorkbench(EntityPlayer player, ItemStack stack)
	{
		//NBTTagCompound upgrades = getUpgrades(stack);
		// TODO: 31.01.2023 advancements
	}

	@Override
	public AmmoHandler getAmmoHandler(ItemStack weapon)
	{
		if (hasIIUpgrade(weapon, WeaponUpgrade.SHOTGUN_REVOLVER_DRUM_MAGAZINE))
			return ammoHandlerSemiAuto;
		if (hasIIUpgrade(weapon, WeaponUpgrade.SHOTGUN_EXTENDED_MAGAZINE))
			return ammoHandlerExtenededMagazine;
		else
			return ammoHandler;
	}

	@Override
	protected FireModeType getFireMode(ItemStack weapon)
	{
		return hasIIUpgrade(weapon, WeaponUpgrade.SHOTGUN_REVOLVER_DRUM_MAGAZINE)?FireModeType.AUTOMATIC: FireModeType.SINGULAR;
		//return FireModeType.SINGULAR;
	}

	@Override
	protected double getEquipSpeed(ItemStack weapon, EasyNBT nbt)
	{
		return hasIIUpgrade(weapon, WeaponUpgrade.SHOTGUN_REVOLVER_DRUM_MAGAZINE) ? 1.0625: 0.9;
		//return 0.9;
	}

	@Override
	public int getFireDelay(ItemStack weapon, EasyNBT nbt)
	{
		return hasIIUpgrade(weapon, WeaponUpgrade.SHOTGUN_REVOLVER_DRUM_MAGAZINE)?Shotgun.bulletFireTimeSemiAuto: Shotgun.bulletFireTime;
		//return Shotgun.bulletFireTime;
	}

	@SideOnly(Side.CLIENT)
	@Override
	public void getSubItems(CreativeTabs tab, NonNullList<ItemStack> list)
	{
		if(this.isInCreativeTab(tab))
		{
			list.add(new ItemStack(this, 1));

			ItemStack handmade = new ItemStack(this, 1, 0);
			handmade.setTagCompound(EasyNBT.newNBT().withBoolean(HANDMADE, true).unwrap());
			list.add(handmade);
		}
	}

	@Override
	public String getUnlocalizedName(ItemStack stack)
	{
		if(ItemNBTHelper.hasKey(stack, HANDMADE))
			return "item.immersiveintelligence.shotgun_handmade";
		else
			return super.getUnlocalizedName(stack);
	}

	@Nullable
	@Override
	protected SoundEvent getDryfireSound(ItemStack weapon, EasyNBT easyNBT)
	{
		return IISounds.shotgunShotDry;
	}

	@Nullable
	@Override
	protected RangedSound getFireSound(ItemStack weapon, EasyNBT easyNBT)
	{
		//return hasIIUpgrade(weapon, WeaponUpgrade.SHOTGUN_REVOLVER_DRUM_MAGAZINE)?IISounds.shotgunShot: IISounds.shotgunShot;
		return IISounds.shotgunShot;
	}

	@Override
	protected int getEnemyAttractRange(ItemStack weapon, EasyNBT nbt)
	{
		return Shotgun.enemyAttractRange;
	}

	@Override
	public int getAimingTime(ItemStack weapon, EasyNBT nbt)
	{
		return Shotgun.aimTime;
	}

	@Override
	public int getReloadTime(ItemStack weapon, ItemStack loaded, EasyNBT nbt)
	{
		return hasIIUpgrades(weapon, WeaponUpgrade.SHOTGUN_REVOLVER_DRUM_MAGAZINE)?Shotgun.magazineReloadTime: Shotgun.bulletReloadTime;
		//return Shotgun.bulletReloadTime;
	}

	@Override
	public float getHorizontalRecoil(ItemStack weapon, EasyNBT nbt, boolean isAimed)
	{
		return (isAimed?0.5f: 1f)*Shotgun.recoilHorizontal;
	}

	@Override
	public float getVerticalRecoil(ItemStack weapon, EasyNBT nbt, boolean isAimed)
	{
		if(nbt.hasKey(WeaponUpgrade.SHOTGUN_SAWED_OFF_BARREL))
			return (isAimed?0.75f: 1f)*1.55f;
		return (isAimed?0.5f: 1f)*Shotgun.recoilVertical;
	}

	@Override
	public float getMaxHorizontalRecoil(ItemStack weapon, EasyNBT nbt)
	{
		return Shotgun.maxRecoilHorizontal;
	}

	@Override
	public float getMaxVerticalRecoil(ItemStack weapon, EasyNBT nbt)
	{
		return Shotgun.maxRecoilVertical;
	}

	@Override
	protected float getGunfireParticleSize(ItemStack weapon, EasyNBT nbt)
	{
		if(nbt.hasKey(WeaponUpgrade.SHOTGUN_SAWED_OFF_BARREL))
			return 2.5f;
		return 1.5f;
	}

	@Override
	protected float getVelocityModifier(ItemStack weapon, EasyNBT nbt, ItemStack ammo)
	{
		if(nbt.hasKey(WeaponUpgrade.SHOTGUN_SAWED_OFF_BARREL))
			return 2.5f;
		return 1.75f;
	}

	//--- IAdvancedZoomTool ---//

	@Override
	public boolean shouldZoom(ItemStack stack, EntityPlayer player)
	{
		boolean isAimed = ItemNBTHelper.getInt(stack, AIMING) > getAimingTime(stack, EasyNBT.wrapNBT(getUpgrades(stack)))*0.75;
		return isAimed&&hasIIUpgrade(stack, WeaponUpgrade.SCOPE);
	}

	@Override
	public float getZoomProgress(ItemStack stack, EntityPlayer player)
	{
		int aiming = ItemNBTHelper.getInt(stack, AIMING);
		int fullTime = getAimingTime(stack, EasyNBT.wrapNBT(getUpgrades(stack)));

		return MathHelper.clamp(((aiming/(float)fullTime)-0.75f), 0, 0.25f)/0.25f;
	}

	@Override
	public float[] getZoomSteps(ItemStack stack, EntityPlayer player)
	{
		return new float[]{0.125f, 0.25f};
	}

	@SideOnly(Side.CLIENT)
	@Override
	public ResourceLocation getZoomOverlayTexture(ItemStack stack, EntityPlayer player)
	{
		return OVERLAY_SCOPE;
	}
}

package com.hbm.tileentity.machine;

import api.hbm.energy.IBatteryItem;
import api.hbm.energy.IEnergyUser;
import com.hbm.blocks.machine.Charger;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.INBTPacketReceiver;
import com.hbm.tileentity.TileEntityLoadedBase;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.ITickable;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class TileEntityCharger extends TileEntityLoadedBase implements IEnergyUser, INBTPacketReceiver, ITickable {
	private List<EntityPlayer> players = new ArrayList<>();

	private long charge = 0;

	private int lastOp = 0;

	boolean particles = false;

	public int usingTicks;

	public int lastUsingTicks;

	public static final int delay = 20;

	@Override
	public void update() {
		IBlockState state = world.getBlockState(pos);
		ForgeDirection dir = ForgeDirection.getOrientation(state.getValue(Charger.FACING).getOpposite().getIndex());

		if (!world.isRemote) {
			this.trySubscribe(world, pos.add(dir.offsetX, 0, dir.offsetZ), dir);

			AxisAlignedBB bb = new AxisAlignedBB(pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1, pos.getY() + 0.5, pos.getZ() + 1);
			players = world.getEntitiesWithinAABB(EntityPlayer.class, bb);
			charge = 0;

			for (EntityPlayer player : players) {
				for (ItemStack stack : player.getEquipmentAndArmor()) {
					if (stack.getItem() instanceof IBatteryItem) {
						IBatteryItem battery = (IBatteryItem) stack.getItem();
						charge += Math.min(battery.getMaxCharge() - battery.getCharge(stack), battery.getChargeRate());
					}
				}
			}

			particles = lastOp > 0;
			if (particles) {
				lastOp--;
				if (world.getTotalWorldTime() % 20 == 0) {
					world.playSound(null, pos.add(0.5, 0.5, 0.5), SoundEvents.BLOCK_LAVA_EXTINGUISH, SoundCategory.BLOCKS, 0.2F, 0.5F);
				}
			}

			NBTTagCompound data = new NBTTagCompound();
			data.setLong("c", charge);
			data.setBoolean("p", particles);
			INBTPacketReceiver.networkPack(this, data, 25);
		}

		lastUsingTicks = usingTicks;

		if ((charge > 0 || particles) && usingTicks < delay) {
			usingTicks++;
			if (usingTicks == 2)
				world.playSound(null, pos.add(0.5, 0.5, 0.5), SoundEvents.BLOCK_PISTON_EXTEND, SoundCategory.BLOCKS, 0.5F, 0.5F);
		}
		if ((charge <= 0 && !particles) && usingTicks > 0) {
			usingTicks--;
			if (usingTicks == 4)
				world.playSound(null, pos.add(0.5, 0.5, 0.5), SoundEvents.BLOCK_PISTON_CONTRACT, SoundCategory.BLOCKS, 0.5F, 0.5F);
		}

		if (particles) {
			Random rand = world.rand;
			world.spawnParticle(EnumParticleTypes.CRIT_MAGIC, pos.getX() + 0.5 + rand.nextDouble() * 0.0625 + dir.offsetX * 0.75, pos.getY() + 0.1, pos.getZ() + 0.5 + rand.nextDouble() * 0.0625 + dir.offsetZ * 0.75, -dir.offsetX + rand.nextGaussian() * 0.1, 0, -dir.offsetZ + rand.nextGaussian() * 0.1);
		}

	}

	@Override
	public long getPower() {
		return 0;
	}

	@Override
	public long getMaxPower() {
		return charge;
	}

	@Override
	public void setPower(long power) {
	}

	@Override
	public void networkUnpack(NBTTagCompound nbt) {
		this.charge = nbt.getLong("c");
		this.particles = nbt.getBoolean("p");
	}

	@Override
	public long transferPower(long power) {
		if (this.usingTicks < delay || power == 0) return power;

		for (EntityPlayer player : players) {
			for (ItemStack stack : player.getEquipmentAndArmor()) {
				if (stack.getItem() instanceof IBatteryItem) {
					IBatteryItem battery = (IBatteryItem) stack.getItem();

					long toCharge = Math.min(battery.getMaxCharge() - battery.getCharge(stack), battery.getChargeRate());
					toCharge = Math.min(toCharge, power / 5);
					battery.chargeBattery(stack, toCharge);
					power -= toCharge;

					lastOp = 4;
				}

			}
		}

		return power;
	}
}
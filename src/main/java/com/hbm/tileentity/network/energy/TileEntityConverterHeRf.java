package com.hbm.tileentity.network.energy;

import com.hbm.tileentity.TileEntityLoadedBase;
import com.hbm.config.GeneralConfig;
import com.hbm.lib.ForgeDirection;

import api.hbm.energy.IEnergyConnector;
import cofh.redstoneflux.api.IEnergyProvider;
import cofh.redstoneflux.api.IEnergyReceiver;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;

public class TileEntityConverterHeRf extends TileEntityLoadedBase implements ITickable, IEnergyConnector, IEnergyProvider {

	//Thanks to the great people of Fusion Warfare for helping me with the original implementation of the RF energy API
	
	public TileEntityConverterHeRf() {
		super();
	}
	
	@Override
	public void update() {
		if (!world.isRemote) {
			this.updateStandardConnections(world, pos);
		}
	}

	@Override
	public boolean canConnectEnergy(EnumFacing from) {
		return true;
	}

	@Override
	public int extractEnergy(EnumFacing from, int maxExtract, boolean simulate) {
		return 0;
	}

	@Override
	public int getEnergyStored(EnumFacing from) {
		return 0;
	}

	@Override
	public int getMaxEnergyStored(EnumFacing from) {
		return 0;
	}

	private boolean recursionBrake = false;

	//NTM
	@Override
	public long transferPower(long power) {
		
		if(recursionBrake)
			return power;
		
		recursionBrake = true;
		
		// we have to limit the transfer amount because otherwise FEnSUs would overflow the RF output, twice
		int toRF = (int) Math.min(Integer.MAX_VALUE, power);
		int rfTransferred = 0;
		int totalTransferred = 0;

		for(ForgeDirection dir : ForgeDirection.VALID_DIRECTIONS) {

			TileEntity entity = world.getTileEntity(pos.add(dir.offsetX, dir.offsetY, dir.offsetZ));

			if(entity != null && entity instanceof IEnergyReceiver) {
				
				IEnergyReceiver receiver = (IEnergyReceiver) entity;
				rfTransferred = receiver.receiveEnergy(dir.getOpposite().toEnumFacing(), toRF, false);
				totalTransferred += rfTransferred;
				
				toRF -= rfTransferred; //to prevent energy duping
			}
		}

		recursionBrake = false;
		lastTransfer = totalTransferred / GeneralConfig.rfConversionRate;
		
		return power - (totalTransferred / GeneralConfig.rfConversionRate);
	}
	
	@Override
	public long getPower() {
		return 0;
	}

	@Override
	public long getMaxPower() {
		return Integer.MAX_VALUE / GeneralConfig.rfConversionRate;
	}

	private long lastTransfer = 0;
	
	@Override
	public long getTransferWeight() {
		
		if(lastTransfer > 0) {
			return lastTransfer * 2;
		} else {
			return getMaxPower();
		}
	}
}

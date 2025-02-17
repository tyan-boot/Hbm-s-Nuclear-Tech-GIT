package com.hbm.tileentity.machine;

import com.hbm.blocks.BlockDummyable;
import com.hbm.forgefluid.FFUtils;
import com.hbm.forgefluid.ModForgeFluids;
import api.hbm.tile.IHeatSource;
import com.hbm.inventory.BoilerRecipes;
import com.hbm.lib.ForgeDirection;
import com.hbm.lib.Library;
import com.hbm.tileentity.INBTPacketReceiver;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidTank;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidTankProperties;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class TileEntityHeatBoiler extends TileEntity implements INBTPacketReceiver, ITickable, IFluidHandler {

    public FluidTank[] tanks;
    public Fluid[] types = new Fluid[2];
    public int heat;
    public static final int maxHeat = 100_000;
    public static final double diffusion = 0.05D;

    public TileEntityHeatBoiler() {
        super();
        tanks = new FluidTank[2];

        tanks[0] = new FluidTank(FluidRegistry.WATER, 0, 64000);
        types[0] = FluidRegistry.WATER;

        tanks[1] = new FluidTank(ModForgeFluids.steam, 0, 64000);
        types[1] = ModForgeFluids.steam;

    }

    public void setTankType(int idx, Fluid type){
        if(types[idx] != type){
            types[idx] = type;
            if(type != null){
                tanks[idx].setFluid(new FluidStack(type, 0));
            }else {
                tanks[idx].setFluid(null);
            }
        }
    }

    @Override
    public void update() {

        if(!world.isRemote) {
            setupTanks();
            tryPullHeat();
            tryConvert();
            networkPack();

            fillFluidInit(tanks[0]);
            fillFluidInit(tanks[1]);

            this.heat -= (heat - TileEntityHeatBoiler.maxHeat / 3) / 10;
        }
    }

    public void fillFluidInit(FluidTank type) {

        ForgeDirection dir = ForgeDirection.getOrientation(this.getBlockMetadata() - BlockDummyable.offset);
        dir = dir.getRotation(ForgeDirection.UP);
        this.fillFluid(pos.getX(), pos.getY() + 4, pos.getZ(), type);
        this. fillFluid(pos.getX() + dir.offsetX, pos.getY(), pos.getZ() + dir.offsetZ * 2, type);
        this.fillFluid(pos.getX() - dir.offsetX * 2, pos.getY(), pos.getZ() - dir.offsetZ * 2, type);
        System.out.print((pos.getX() + dir.offsetX * 2) + " and " + (pos.getZ() + dir.offsetZ * 2 ) + " ");
        System.out.print((pos.getX() - dir.offsetX * 2) + " and " + (pos.getZ() - dir.offsetZ * 2 ) + " ");
        System.out.print(pos.getY());
    }

    public void fillFluid(int x, int y, int z, FluidTank type) {
        FFUtils.fillFluid(this, type, world, new BlockPos(x, y, z), 64000);
    }

    @Override
    public <T> T getCapability(Capability<T> capability, EnumFacing facing){
        if(capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY)
            return CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY.cast(this);
        return super.getCapability(capability, facing);
    }

    @Override
    public boolean hasCapability(Capability<?> capability, EnumFacing facing){
        return capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY || super.hasCapability(capability, facing);
    }

    @Override
    public IFluidTankProperties[] getTankProperties() {
        return new IFluidTankProperties[] {tanks[0].getTankProperties()[0], tanks[1].getTankProperties()[0]};
    }

    @Override
    public int fill(FluidStack resource, boolean doFill){
        if(resource != null && resource.getFluid() == types[0]){
            return tanks[0].fill(resource, doFill);
        }
        return 0;
    }

    @Override
    public FluidStack drain(FluidStack resource, boolean doDrain){
        FluidStack drain = null;
        if(resource.getFluid() == types[1]){
            drain = tanks[1].drain(resource, doDrain);
        }
        return drain;
    }

    @Override
    public FluidStack drain(int maxDrain, boolean doDrain){
        FluidStack drain = tanks[1].drain(maxDrain, doDrain);
        return drain;
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        super.readFromNBT(nbt);
        FFUtils.deserializeTankArray(nbt.getTagList("tanks", 10), tanks);
        for(int i=0; i<tanks.length; i++){
            if(tanks[i].getFluid() != null){
                types[i] = tanks[i].getFluid().getFluid();
            } else {
                types[i] = null;
            }
        }
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
        super.writeToNBT(nbt);
        for(int i=0; i<tanks.length; i++){
            if(types[i] != null){
                tanks[i].setFluid(new FluidStack(types[i], tanks[i].getFluidAmount()));
            } else {
                tanks[i].setFluid(null);
            }
        }
        nbt.setTag("tanks", FFUtils.serializeTankArray(tanks));
        return nbt;
    }


    public void networkPack(){
        NBTTagCompound data = new NBTTagCompound();
        for(int i=0; i<tanks.length; i++){
            if(types[i] != null){
                tanks[i].setFluid(new FluidStack(types[i], tanks[i].getFluidAmount()));
            } else {
                tanks[i].setFluid(null);
            }
        }
        data.setTag("tanks", FFUtils.serializeTankArray(tanks));
        INBTPacketReceiver.networkPack(this, data, 25);
    }

    @Override
    public void networkUnpack(NBTTagCompound nbt) {
        FFUtils.deserializeTankArray(nbt.getTagList("tanks", 10), tanks);
        for(int i=0; i<tanks.length; i++){
            if(tanks[i].getFluid() != null){
                types[i] = tanks[i].getFluid().getFluid();
            } else {
                types[i] = null;
            }
        }
    }

    private void setupTanks() {
        FluidStack[] fluids = BoilerRecipes.getOutputsFromFluid(types[0]);
        if (fluids != null) {
            setTankType(0, types[0]);
            setTankType(1, fluids[0].getFluid());
        } else {
            setTankType(0, null);
            setTankType(1, null);
        }
    }

    private void tryConvert() {
        FluidStack[] outputFluids = BoilerRecipes.getOutputsFromFluid(types[0]);
        if (heat > TileEntityHeatBoiler.maxHeat / 3) {
            if (outputFluids != null) {
                if (tanks[0].getFluidAmount() >= 100) {
                    tanks[0].drain(100, true);
                    tanks[1].fill(outputFluids[0].copy(),true);
                }
            }
        }
    }
    protected void tryPullHeat() {

        if(this.heat >= TileEntityHeatBoiler.maxHeat) return;
        BlockPos blockBelow = pos.down();
        TileEntity con = world.getTileEntity(blockBelow);

        if(con instanceof IHeatSource) {
            IHeatSource source = (IHeatSource) con;
            int diff = source.getHeatStored() - this.heat;

            if(diff == 0) {
                return;
            }

            if(diff > 0) {
                diff = (int) Math.ceil(diff * diffusion);
                source.useUpHeat(diff);
                this.heat += diff;
                if(this.heat > TileEntityHeatBoiler.maxHeat)
                    this.heat = TileEntityHeatBoiler.maxHeat;
                return;
            }
        }

        this.heat = Math.max(this.heat - Math.max(this.heat / 1000, 1), 0);
    }

    AxisAlignedBB bb = null;

    @Override
    public AxisAlignedBB getRenderBoundingBox() {

        if(bb == null) {
            bb = new AxisAlignedBB(
                    pos.getX() - 1,
                    pos.getY(),
                    pos.getZ() - 1,
                    pos.getX() + 2,
                    pos.getY() + 4,
                    pos.getZ() + 2
            );
        }

        return bb;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public double getMaxRenderDistanceSquared() {
        return 65536.0D;
    }

}
package com.hbm.blocks;

import java.util.List;

import com.hbm.main.MainRegistry;

import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyInteger;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

public class BlockBase extends Block {
	
	public BlockBase(Material m, String s){
		super(m);
		this.setUnlocalizedName(s);
		this.setRegistryName(s);
		this.setCreativeTab(MainRegistry.controlTab);
		ModBlocks.ALL_BLOCKS.add(this);
	}
	
	@Override
	public void addInformation(ItemStack stack, World player, List<String> tooltip, ITooltipFlag advanced) {
		if(stack.getItem() == Item.getItemFromBlock(ModBlocks.meteor_battery)){
			tooltip.add("Provides infinite charge to tesla coils");
		}
		
		float hardness = this.getExplosionResistance(null);
		if(hardness > 50){
			tooltip.add("§6Blast Resistance: "+hardness+"§r");
		}
	}

	public Block setSoundType(SoundType sound){
		return super.setSoundType(sound);
	}
}

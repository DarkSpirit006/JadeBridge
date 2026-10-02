package com.darkspirit69.jadebridge.provider;

import com.darkspirit69.jadebridge.provider.block.BeehiveProvider;
import com.darkspirit69.jadebridge.provider.block.BlockLootTableProvider;
import com.darkspirit69.jadebridge.provider.block.BrewingStandProvider;
import com.darkspirit69.jadebridge.provider.block.CommandBlockProvider;
import com.darkspirit69.jadebridge.provider.block.FurnaceProvider;
import com.darkspirit69.jadebridge.provider.block.HopperLockProvider;
import com.darkspirit69.jadebridge.provider.block.JukeboxProvider;
import com.darkspirit69.jadebridge.provider.block.LecternProvider;
import com.darkspirit69.jadebridge.provider.block.ObjectNameProvider;
import com.darkspirit69.jadebridge.provider.block.RedstoneProvider;
import com.darkspirit69.jadebridge.provider.block.ShelfProvider;
import com.darkspirit69.jadebridge.provider.block.TrialSpawnerProvider;
import com.darkspirit69.jadebridge.provider.entity.AnimalOwnerProvider;
import com.darkspirit69.jadebridge.provider.entity.EntityHealthProvider;
import com.darkspirit69.jadebridge.provider.entity.EntityLootTableProvider;
import com.darkspirit69.jadebridge.provider.entity.MobBreedingProvider;
import com.darkspirit69.jadebridge.provider.entity.MobGrowthProvider;
import com.darkspirit69.jadebridge.provider.entity.NextEntityDropProvider;
import com.darkspirit69.jadebridge.provider.entity.PetArmorProvider;
import com.darkspirit69.jadebridge.provider.entity.StatusEffectsProvider;
import com.darkspirit69.jadebridge.provider.entity.VillagerRestockProvider;
import com.darkspirit69.jadebridge.provider.entity.WaxedProvider;
import com.darkspirit69.jadebridge.provider.entity.ZombieVillagerProvider;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.sniffer.Sniffer;
import net.minecraft.world.entity.animal.allay.Allay;
import net.minecraft.world.entity.animal.armadillo.Armadillo;
import net.minecraft.world.entity.animal.chicken.Chicken;
import net.minecraft.world.entity.animal.frog.Tadpole;
import net.minecraft.world.entity.animal.golem.CopperGolem;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.monster.zombie.ZombieVillager;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.BeehiveBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BrewingStandBlockEntity;
import net.minecraft.world.level.block.entity.ChiseledBookShelfBlockEntity;
import net.minecraft.world.level.block.entity.CommandBlockEntity;
import net.minecraft.world.level.block.entity.ComparatorBlockEntity;
import net.minecraft.world.level.block.entity.CalibratedSculkSensorBlockEntity;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.entity.JukeboxBlockEntity;
import net.minecraft.world.level.block.entity.LecternBlockEntity;
import net.minecraft.world.level.block.entity.ShelfBlockEntity;
import net.minecraft.world.level.block.entity.TrialSpawnerBlockEntity;

/**
 * Registration of the vanilla provider set, in the order Jade's plugins register
 * theirs (core, vanilla, universal, debug). The order defines the wire ids, which
 * the client adopts during the handshake, so it only has to stay deterministic.
 */
public final class VanillaProviders {

    private VanillaProviders() {
    }

    public static ProviderRegistry create() {
        ProviderRegistry registry = new ProviderRegistry();

        registry.registerBlock(BlockEntity.class, ObjectNameProvider.INSTANCE);

        registry.registerBlock(BrewingStandBlockEntity.class, BrewingStandProvider.INSTANCE);
        registry.registerBlock(BeehiveBlockEntity.class, BeehiveProvider.INSTANCE);
        registry.registerBlock(CommandBlockEntity.class, CommandBlockProvider.INSTANCE);
        registry.registerBlock(HopperBlockEntity.class, HopperLockProvider.INSTANCE);
        registry.registerBlock(JukeboxBlockEntity.class, JukeboxProvider.INSTANCE);
        registry.registerBlock(LecternBlockEntity.class, LecternProvider.INSTANCE);
        registry.registerBlock(ComparatorBlockEntity.class, RedstoneProvider.INSTANCE);
        registry.registerBlock(CalibratedSculkSensorBlockEntity.class, RedstoneProvider.INSTANCE);
        registry.registerBlock(AbstractFurnaceBlockEntity.class, FurnaceProvider.INSTANCE);
        registry.registerBlock(ChiseledBookShelfBlockEntity.class, ShelfProvider.INSTANCE);
        registry.registerBlock(ShelfBlockEntity.class, ShelfProvider.INSTANCE);
        registry.registerBlock(TrialSpawnerBlockEntity.class, TrialSpawnerProvider.INSTANCE);

        registry.registerBlock(Block.class, ItemStorageProvider.BLOCK);

        registry.registerBlock(BlockEntity.class, BlockLootTableProvider.INSTANCE);

        registry.registerEntity(Entity.class, AnimalOwnerProvider.INSTANCE);
        registry.registerEntity(LivingEntity.class, StatusEffectsProvider.INSTANCE);
        registry.registerEntity(AgeableMob.class, MobGrowthProvider.INSTANCE);
        registry.registerEntity(Tadpole.class, MobGrowthProvider.INSTANCE);
        registry.registerEntity(net.minecraft.world.entity.animal.Animal.class, MobBreedingProvider.INSTANCE);
        registry.registerEntity(Villager.class, MobBreedingProvider.INSTANCE);
        registry.registerEntity(Allay.class, MobBreedingProvider.INSTANCE);
        registry.registerEntity(Villager.class, VillagerRestockProvider.INSTANCE);
        registry.registerEntity(Chicken.class, NextEntityDropProvider.INSTANCE);
        registry.registerEntity(Armadillo.class, NextEntityDropProvider.INSTANCE);
        registry.registerEntity(Sniffer.class, NextEntityDropProvider.INSTANCE);
        registry.registerEntity(ZombieVillager.class, ZombieVillagerProvider.INSTANCE);
        registry.registerEntity(Mob.class, PetArmorProvider.INSTANCE);
        registry.registerEntity(CopperGolem.class, WaxedProvider.INSTANCE);
        registry.registerEntity(LivingEntity.class, EntityHealthProvider.INSTANCE);

        registry.registerEntity(Entity.class, ItemStorageProvider.ENTITY);

        registry.registerEntity(Entity.class, EntityLootTableProvider.INSTANCE);

        return registry;
    }
}

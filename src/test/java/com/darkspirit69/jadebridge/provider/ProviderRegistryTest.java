package com.darkspirit69.jadebridge.provider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.darkspirit69.jadebridge.TestEnv;
import java.util.List;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class ProviderRegistryTest {

    @BeforeAll
    static void boot() {
        TestEnv.boot();
    }

    private record Provider(Identifier id, int priority) implements BlockDataProvider, EntityDataProvider {
        @Override
        public Identifier id() {
            return id;
        }

        @Override
        public int priority() {
            return priority;
        }

        @Override
        public void appendServerData(BlockContext context) {
        }

        @Override
        public void appendServerData(EntityContext context) {
        }
    }

    private static Provider provider(String path, int priority) {
        return new Provider(Identifier.parse(path), priority);
    }

    @Test
    void idsFollowRegistrationOrder() {
        ProviderRegistry registry = new ProviderRegistry();
        Provider first = provider("jade:first", 0);
        Provider second = provider("jade:second", 0);
        Provider third = provider("jade:third", 0);
        registry.registerBlock(Block.class, first);
        registry.registerBlock(BlockEntity.class, second);
        registry.registerEntity(Entity.class, third);

        assertEquals(List.of(first.id(), second.id()), registry.blockProviderIds());
        assertEquals(List.of(third.id()), registry.entityProviderIds());
        assertEquals(first, registry.blockProvider(0));
        assertEquals(second, registry.blockProvider(1));
        assertEquals(third, registry.entityProvider(0));
        assertNull(registry.blockProvider(2));
        assertNull(registry.blockProvider(-1));
    }

    @Test
    void entityHierarchyLookupAndPriorityOrder() {
        ProviderRegistry registry = new ProviderRegistry();
        Provider base = provider("jade:base", 0);
        Provider lowPriority = provider("jade:low", 1000);
        Provider mid = provider("jade:mid", 10);
        registry.registerEntity(LivingEntity.class, base);
        registry.registerEntity(AgeableMob.class, mid);
        registry.registerEntity(Cow.class, lowPriority);

        assertEquals(List.of(base, mid, lowPriority), registry.providersFor(Cow.class));
    }

    @Test
    void blockAndBlockEntitySidesAreMerged() {
        ProviderRegistry registry = new ProviderRegistry();
        Provider blockSide = provider("jade:block_side", 0);
        Provider entitySide = provider("jade:be_side", 0);
        Provider storage = provider("jade:storage", 1000);
        registry.registerBlock(Block.class, blockSide);
        registry.registerBlock(RandomizableContainerBlockEntity.class, entitySide);
        registry.registerBlock(BlockEntity.class, storage);

        List<BlockDataProvider> merged = registry.providersFor(ChestBlock.class, ChestBlockEntity.class);
        assertEquals(List.of(blockSide, entitySide, storage), merged);
    }

    @Test
    void blockEntityIsNullSkipsThatSide() {
        ProviderRegistry registry = new ProviderRegistry();
        Provider blockSide = provider("jade:block_side", 0);
        Provider beSide = provider("jade:be_side", 0);
        registry.registerBlock(Block.class, blockSide);
        registry.registerBlock(RandomizableContainerBlockEntity.class, beSide);

        assertEquals(List.of(blockSide), registry.providersFor(ChestBlock.class, null));
    }

    @Test
    void unrelatedTargetsGetNoProviders() {
        ProviderRegistry registry = new ProviderRegistry();
        registry.registerEntity(LivingEntity.class, provider("jade:living", 0));
        assertEquals(List.of(), registry.providersFor(PrimedTnt.class));

        registry.registerBlock(ChestBlockEntity.class, provider("jade:chest", 0));
        assertEquals(List.of(), registry.providersFor(Block.class, null));
    }
}

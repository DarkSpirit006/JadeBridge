package com.darkspirit69.jadebridge.provider.entity;

import com.darkspirit69.jadebridge.provider.EntityContext;
import com.darkspirit69.jadebridge.provider.EntityDataProvider;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.Mob;

/** {@code minecraft:pet_armor} — the body armor of wolves, horses, happy ghasts, ... */
public final class PetArmorProvider implements EntityDataProvider {

    public static final PetArmorProvider INSTANCE = new PetArmorProvider();
    public static final Identifier ID = Identifier.withDefaultNamespace("pet_armor");

    private PetArmorProvider() {
    }

    @Override
    public Identifier id() {
        return ID;
    }

    @Override
    public void appendServerData(EntityContext context) {
        ItemStack armor = context.entity(Mob.class).getBodyArmorItem();
        if (!armor.isEmpty()) {
            context.put(ID, ItemStack.OPTIONAL_STREAM_CODEC, armor);
        }
    }
}

package com.darkspirit69.jadebridge.protocol;

import com.darkspirit69.jadebridge.JadeProtocol;
import com.darkspirit69.jadebridge.Settings;
import com.darkspirit69.jadebridge.provider.EntityContext;
import com.darkspirit69.jadebridge.provider.EntityDataProvider;
import com.darkspirit69.jadebridge.provider.ProviderRegistry;
import com.darkspirit69.jadebridge.session.JadeSession;
import com.darkspirit69.jadebridge.session.SessionManager;
import com.darkspirit69.jadebridge.util.ReachCheck;
import io.netty.buffer.Unpooled;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.enderdragon.EnderDragonPart;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jspecify.annotations.Nullable;

/**
 * Handles {@code jade:request_entity}. The entity is resolved through the level's
 * id lookup (ender dragon parts through their parent), never by scanning.
 */
public final class EntityRequestHandler {

    private final Plugin plugin;
    private final Settings settings;
    private final ProviderRegistry providers;
    private final SessionManager sessions;
    private final ReachCheck reach;
    private final Set<Object> failedProviders = new HashSet<>();

    public EntityRequestHandler(
            Plugin plugin,
            Settings settings,
            ProviderRegistry providers,
            SessionManager sessions,
            ReachCheck reach) {
        this.plugin = plugin;
        this.settings = settings;
        this.providers = providers;
        this.sessions = sessions;
        this.reach = reach;
    }

    public void handle(Player player, byte[] bytes) {
        if (bytes.length == 0 || bytes.length > JadeProtocol.MAX_INCOMING_SIZE) {
            return;
        }
        JadeSession session = sessions.get(player);
        if (session == null || !session.tryAcquireEntityRequest(System.currentTimeMillis())) {
            sessions.countRejected();
            return;
        }

        ServerPlayer serverPlayer = ((CraftPlayer) player).getHandle();
        RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(
                Unpooled.wrappedBuffer(bytes),
                serverPlayer.level().registryAccess());
        try {
            EntityRequest request = EntityRequest.decode(buf, providers::entityProvider);
            if (buf.readableBytes() != 0) {
                throw new IllegalArgumentException("Trailing bytes in request");
            }
            sessions.countEntityRequest();
            respond(serverPlayer, request);
        } catch (Exception e) {
            sessions.countRejected();
            if (settings.isDebug()) {
                plugin.getSLF4JLogger().warn("Dropped malformed entity request from " + player.getName() + ": " + e);
            }
        } finally {
            buf.release();
        }
    }

    private void respond(ServerPlayer player, EntityRequest request) {
        Entity entity = resolveEntity(player.level().getEntity(request.entityId()), request.partIndex());
        if (entity == null) {
            // Jade's server also stays silent when the entity is gone
            return;
        }

        CompoundTag tag = request.data();
        tag.putInt("EntityId", entity.getId());
        if (reach.isOutOfReach(player, entity.blockPosition(), player.entityInteractionRange())
                || !canBeTarget(entity, player)) {
            ReceiveData.send(player.getBukkitEntity(), plugin, tag);
            return;
        }

        RegistryFriendlyByteBuf scratch = new RegistryFriendlyByteBuf(
                Unpooled.buffer(),
                player.level().registryAccess());
        try {
            EntityContext context = new EntityContext(
                    player,
                    entity,
                    request.hitVec(),
                    request.showDetails(),
                    tag,
                    scratch);
            for (EntityDataProvider provider : providers.providersFor(entity.getClass())) {
                if (!request.providers().contains(provider)) {
                    continue;
                }
                try {
                    provider.appendServerData(context);
                } catch (Exception e) {
                    logProviderError(provider, e);
                }
            }
        } finally {
            scratch.release();
        }
        ReceiveData.send(player.getBukkitEntity(), plugin, tag);
    }

    /**
     * Ender dragon hits arrive as the parent id plus the part index; everything else
     * resolves directly. Mirrors Jade's CommonProxy.getPartEntity.
     */
    @Nullable
    private static Entity resolveEntity(@Nullable Entity parent, int partIndex) {
        if (parent == null) {
            return null;
        }
        if (partIndex < 0) {
            return parent;
        }
        if (parent instanceof EnderDragon dragon) {
            EnderDragonPart[] parts = dragon.getSubEntities();
            if (partIndex < parts.length) {
                return parts[partIndex];
            }
        }
        return parent;
    }

    /**
     * Jade 26.3 also refuses entities the player cannot meaningfully target:
     * removed ones, spectators, the player's own vehicle, non-pickable dragon
     * bodies and invisible entities without armor.
     */
    private static boolean canBeTarget(Entity target, ServerPlayer player) {
        if (target.isRemoved() || target.isSpectator() || target == player.getVehicle()) {
            return false;
        }
        if (target instanceof EnderDragon && !target.isPickable()) {
            return false;
        }
        return !(target.isInvisibleTo(player) && hasNoEquipment(target));
    }

    private static boolean hasNoEquipment(Entity entity) {
        if (!(entity instanceof LivingEntity living)) {
            return true;
        }
        for (EquipmentSlot slot : EquipmentSlot.VALUES) {
            if (slot.isArmor() && living.hasItemInSlot(slot)) {
                return false;
            }
        }
        return true;
    }

    private void logProviderError(EntityDataProvider provider, Exception e) {
        if (failedProviders.add(provider)) {
            plugin.getSLF4JLogger().warn("Entity data provider " + provider.id() + " failed; further errors suppressed", e);
        } else if (settings.isDebug()) {
            plugin.getSLF4JLogger().warn("Entity data provider " + provider.id() + " failed again: " + e);
        }
    }
}

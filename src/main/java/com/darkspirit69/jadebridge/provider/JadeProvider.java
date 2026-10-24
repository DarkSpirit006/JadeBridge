package com.darkspirit69.jadebridge.provider;

import net.minecraft.resources.Identifier;

/**
 * Base contract of a server-side data provider, the counterpart of Jade's
 * {@code IServerDataProvider}. Block and entity providers each get their own
 * context type; ids must match the ids Jade's client registers them under.
 */
public interface JadeProvider {

    Identifier id();

    /** Lower values run first, mirroring Jade's tooltip priority ordering. */
    default int priority() {
        return 0;
    }
}

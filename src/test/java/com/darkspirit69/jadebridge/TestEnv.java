package com.darkspirit69.jadebridge;

import net.minecraft.SharedConstants;

/**
 * Boots vanilla registries far enough to construct identifiers, NBT and buffers
 * outside a running server. ItemStacks with real items still need a full server
 * (item components bind during datapack load), so tests use empty stacks.
 */
public final class TestEnv {

    private static boolean booted;

    private TestEnv() {
    }

    public static synchronized void boot() {
        if (booted) {
            return;
        }
        booted = true;
        SharedConstants.tryDetectVersion();
        net.minecraft.server.Bootstrap.bootStrap();
    }
}

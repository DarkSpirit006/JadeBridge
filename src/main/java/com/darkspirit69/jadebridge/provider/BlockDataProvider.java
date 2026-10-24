package com.darkspirit69.jadebridge.provider;

/** Server data provider for block targets. */
public interface BlockDataProvider extends JadeProvider {

    void appendServerData(BlockContext context);
}

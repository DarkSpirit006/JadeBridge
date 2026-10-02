package com.darkspirit69.jadebridge.provider;

/** Server data provider for entity targets. */
public interface EntityDataProvider extends JadeProvider {

    void appendServerData(EntityContext context);
}

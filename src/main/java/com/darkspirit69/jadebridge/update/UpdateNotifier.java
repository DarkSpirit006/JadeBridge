package com.darkspirit69.jadebridge.update;

import com.darkspirit69.jadebridge.JadeBridge;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/** Presents update results to administrators and command senders. */
public final class UpdateNotifier {

    private static final String PROJECT_URL = "https://modrinth.com/project/jadebridge";

    private final JadeBridge plugin;
    private final UpdateService updateService;

    public UpdateNotifier(JadeBridge plugin) {
        this.plugin = plugin;
        this.updateService = new UpdateService(plugin);
    }

    public void notifyIfUpdateAvailable(Player player) {
        updateService.checkForPlayer(player, result -> {
            String installed = plugin.getPluginMeta().getVersion();
            if (!result.hasRelease() || !updateService.isNewer(result.latestVersion(), installed)) {
                return;
            }
            int versionsBehind = updateService.countVersionsBehind(result, installed);
            player.sendMessage(Component.text("JadeBridge is " + versionsBehind + " " + versionWord(versionsBehind) + " behind.", NamedTextColor.GOLD));
            player.sendMessage(Component.text("Download the newest version here:", NamedTextColor.YELLOW));
            player.sendMessage(updateLink());
        });
    }

    public void checkNow(CommandSender sender) {
        updateService.checkForSender(sender, result -> sendVersionResult(sender, result));
    }

    private void sendVersionResult(CommandSender sender, UpdateResult result) {
        String installed = plugin.getPluginMeta().getVersion();
        if (!result.hasRelease()) {
            sender.sendMessage(Component.text("Current version: v" + installed, NamedTextColor.GOLD));
            sender.sendMessage(Component.text("Could not determine the latest Modrinth version.", NamedTextColor.RED));
            return;
        }

        sender.sendMessage(Component.text("Current version: v" + installed, NamedTextColor.GOLD));
        if (!updateService.isNewer(result.latestVersion(), installed)) {
            sender.sendMessage(Component.text("JadeBridge is up to date.", NamedTextColor.GREEN));
            return;
        }

        int versionsBehind = updateService.countVersionsBehind(result, installed);
        sender.sendMessage(Component.text("JadeBridge is " + versionsBehind + " " + versionWord(versionsBehind) + " behind.", NamedTextColor.GOLD));
        sender.sendMessage(Component.text("Download the newest version here:", NamedTextColor.YELLOW));
        sender.sendMessage(updateLink());
    }

    private String versionWord(int count) {
        return count == 1 ? "version" : "versions";
    }

    private Component updateLink() {
        return Component.text(PROJECT_URL, NamedTextColor.AQUA)
                .clickEvent(ClickEvent.openUrl(PROJECT_URL));
    }
}

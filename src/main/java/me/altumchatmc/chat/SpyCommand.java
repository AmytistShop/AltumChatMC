package me.altumchatmc.chat;

import org.bukkit.command.*;
import org.bukkit.entity.Player;

public class SpyCommand implements CommandExecutor {

    private final AltumChatPlugin plugin;

    public SpyCommand(AltumChatPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {

        if (!(sender instanceof Player p)) {
            sender.sendMessage("Only players.");
            return true;
        }

        if (!p.hasPermission("altumchat.spy.toggle")) {
            p.sendMessage(plugin.color(plugin.cfg().getString("messages.noPerm", "&cНет прав.")));
            return true;
        }

        plugin.toggleSpy(p.getUniqueId());

        boolean enabled = plugin.isSpy(p.getUniqueId());
        String msg = enabled
                ? plugin.color(plugin.cfg().getString("spy.enabled", "&aSpy включён."))
                : plugin.color(plugin.cfg().getString("spy.disabled", "&cSpy выключен."));

        p.sendMessage(msg);
        return true;
    }
}

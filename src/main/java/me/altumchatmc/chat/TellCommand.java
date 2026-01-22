package me.altumchatmc.chat;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

public class TellCommand implements CommandExecutor {

    private final AltumChatPlugin plugin;

    public TellCommand(AltumChatPlugin plugin) {
        this.plugin = plugin;
    }

    private String joinArgs(String[] args, int from) {
        StringBuilder sb = new StringBuilder();
        for (int i = from; i < args.length; i++) {
            if (i > from) sb.append(" ");
            sb.append(args[i]);
        }
        return sb.toString();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {

        if (!sender.hasPermission("altumchat.tell")) {
            sender.sendMessage(plugin.color(plugin.cfg().getString("messages.noPerm", "&cНет прав.")));
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(plugin.color(plugin.cfg().getString("tell.usage", "&cИспользуй: &f/tell <ник> <сообщение>")));
            return true;
        }

        String targetName = args[0];
        String message = joinArgs(args, 1);

        Player target = plugin.findOnlinePlayerByNameExactIgnoreCase(targetName);
        if (target == null) {
            sender.sendMessage(plugin.color(plugin.cfg().getString("tell.playerOffline", "&cИгрок не в сети.")));
            return true;
        }

        String receiverFmt = plugin.color(plugin.cfg().getString("tell.toReceiver", "&d[ЛС] &7{from} &8» &f{message}"));
        String senderFmt = plugin.color(plugin.cfg().getString("tell.toSender", "&d[ЛС] &7Ты &8» &7{to}&8: &f{message}"));
        String spyFmt = plugin.color(plugin.cfg().getString("tell.toSpy", "&8[&cSPY&8] &d[ЛС] &7{from} &8» &7{to}&8: &f{message}"));

        String fromName = (sender instanceof Player p) ? p.getName() : "CONSOLE";

        // Receiver message
        String receiverMsg;
        if (sender instanceof Player sp) {
            receiverMsg = plugin.applyCommonPlaceholders(receiverFmt, sp, message, sp.getName())
                    .replace("{from}", fromName)
                    .replace("{to}", target.getName());
        } else {
            receiverMsg = ChatColor.translateAlternateColorCodes('&', receiverFmt)
                    .replace("{from}", fromName)
                    .replace("{to}", target.getName())
                    .replace("{message}", message);
        }
        target.sendMessage(receiverMsg);

        // Sender message
        String senderMsg;
        if (sender instanceof Player sp) {
            senderMsg = plugin.applyCommonPlaceholders(senderFmt, sp, message, sp.getName())
                    .replace("{from}", fromName)
                    .replace("{to}", target.getName());
        } else {
            senderMsg = ChatColor.translateAlternateColorCodes('&', senderFmt)
                    .replace("{from}", fromName)
                    .replace("{to}", target.getName())
                    .replace("{message}", message);
        }
        sender.sendMessage(senderMsg);

        // Spy notify (exclude sender+receiver)
        for (var u : plugin.getSpySet()) {
            Player spy = Bukkit.getPlayer(u);
            if (spy == null || !spy.isOnline()) continue;
            if (!spy.hasPermission("altumchat.spy.see")) continue;
            if (sender instanceof Player sp && spy.getUniqueId().equals(sp.getUniqueId())) continue;
            if (spy.getUniqueId().equals(target.getUniqueId())) continue;

            String spyMsg;
            if (sender instanceof Player sp) {
                spyMsg = plugin.applyCommonPlaceholders(spyFmt, sp, message, sp.getName())
                        .replace("{from}", fromName)
                        .replace("{to}", target.getName());
            } else {
                spyMsg = ChatColor.translateAlternateColorCodes('&', spyFmt)
                        .replace("{from}", fromName)
                        .replace("{to}", target.getName())
                        .replace("{message}", message);
            }
            spy.sendMessage(spyMsg);
        }

        return true;
    }
}

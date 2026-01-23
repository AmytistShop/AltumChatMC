package me.altumchatmc.chat;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;
import net.luckperms.api.query.QueryOptions;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class AltumChatPlugin extends JavaPlugin implements Listener {

    private LuckPerms luckPerms;
    private boolean floodgatePresent;
    private boolean placeholderApiPresent;

    private final Set<UUID> spy = ConcurrentHashMap.newKeySet();

    @Override
    public void onEnable() {
        saveDefaultConfig();

        try {
            luckPerms = LuckPermsProvider.get();
        } catch (Throwable t) {
            luckPerms = null;
            getLogger().warning("LuckPerms API not found. Prefix/Suffix will be empty.");
        }

        floodgatePresent = Bukkit.getPluginManager().getPlugin("floodgate") != null;
        placeholderApiPresent = Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null;

        if (placeholderApiPresent) {
            try {
                new AltumChatExpansion(this).register();
                getLogger().info("PlaceholderAPI detected: registered %altumchat_*% placeholders.");
            } catch (Throwable t) {
                getLogger().warning("Failed to register PlaceholderAPI expansion: " + t.getMessage());
            }
        }

        Bukkit.getPluginManager().registerEvents(this, this);

        PluginCommand tell = getCommand("tell");
        if (tell != null) tell.setExecutor(new TellCommand(this));

        PluginCommand spyCmd = getCommand("spy");
        if (spyCmd != null) spyCmd.setExecutor(new SpyCommand(this));

        getLogger().info("AltumChatMC enabled.");
    }

    public FileConfiguration cfg() {
        return getConfig();
    }

    public String color(String s) {
        if (s == null) return "";
        return ChatColor.translateAlternateColorCodes('&', s);
    }

    public boolean isSpy(UUID uuid) {
        return spy.contains(uuid);
    }

    public void toggleSpy(UUID uuid) {
        if (!spy.add(uuid)) spy.remove(uuid);
    }

    public Set<UUID> getSpySet() {
        return spy;
    }

    public boolean isBedrock(Player p) {
        if (!floodgatePresent) return false;
        // FloodgateApi.getInstance().isFloodgatePlayer(uuid) через reflection, чтобы не было hard-dependency
        try {
            Class<?> apiClass = Class.forName("org.geysermc.floodgate.api.FloodgateApi");
            Object api = apiClass.getMethod("getInstance").invoke(null);
            return (boolean) apiClass.getMethod("isFloodgatePlayer", UUID.class).invoke(api, p.getUniqueId());
        } catch (Throwable ignored) {
            return false;
        }
    }

    public String platformTag(Player p) {
        String be = color(cfg().getString("platform.bedrock", "&7[&bB&f&7]"));
        String je = color(cfg().getString("platform.java", "&7[&cJ&f&7]"));
        return isBedrock(p) ? be : je;
    }

    public String luckPermsPrefix(Player p) {
        if (luckPerms == null) return "";
        User user = luckPerms.getUserManager().getUser(p.getUniqueId());
        if (user == null) return "";
        QueryOptions qo = luckPerms.getContextManager().getQueryOptions(p);
        String prefix = user.getCachedData().getMetaData(qo).getPrefix();
        return prefix == null ? "" : ChatColor.translateAlternateColorCodes('&', prefix);
    }

    public String luckPermsSuffix(Player p) {
        if (luckPerms == null) return "";
        User user = luckPerms.getUserManager().getUser(p.getUniqueId());
        if (user == null) return "";
        QueryOptions qo = luckPerms.getContextManager().getQueryOptions(p);
        String suffix = user.getCachedData().getMetaData(qo).getSuffix();
        return suffix == null ? "" : ChatColor.translateAlternateColorCodes('&', suffix);
    }

    public String applyCommonPlaceholders(String format, Player p, String message, String nickOverride) {
        String nick = (nickOverride != null) ? nickOverride : p.getName();

        return format
                .replace("%LuckPerms_prefix%", luckPermsPrefix(p))
                .replace("%LuckPerms_suffix%", luckPermsSuffix(p))
                .replace("{prefix}", luckPermsPrefix(p))
                .replace("{suffix}", luckPermsSuffix(p))
                .replace("{nick}", nick)
                .replace("{player}", nick)
                .replace("{message}", message == null ? "" : message)
                .replace("{platform}", platformTag(p));
    }

    public Player findOnlinePlayerByNameExactIgnoreCase(String name) {
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.getName().equalsIgnoreCase(name)) return p;
        }
        return null;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onAsyncChat(AsyncPlayerChatEvent e) {
        e.setCancelled(true);

        Player p = e.getPlayer();
        String msg = e.getMessage();

        String fmt = color(cfg().getString("chat.format",
                "{platform} %LuckPerms_prefix%{nick}%LuckPerms_suffix% &7: &f{message}"
        ));
        String out = applyCommonPlaceholders(fmt, p, msg, null);

        Bukkit.getConsoleSender().sendMessage(out);
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            viewer.sendMessage(out);
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();

        String joinFmt = color(cfg().getString("messages.join", "&a+ &7{nick} &8{platform}"));
        e.setJoinMessage(applyCommonPlaceholders(joinFmt, p, null, null));

        Bukkit.getScheduler().runTaskLater(this, () -> {
            if (Bukkit.getOnlinePlayers().size() == 1) {
                String alone = color(cfg().getString("messages.alone", "&eНа сервере кроме тебя никого нет."));
                if (!alone.isEmpty()) p.sendMessage(applyCommonPlaceholders(alone, p, null, null));
            }
        }, 10L);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        Player p = e.getPlayer();
        String quitFmt = color(cfg().getString("messages.quit", "&c- &7{nick} &8{platform}"));
        e.setQuitMessage(applyCommonPlaceholders(quitFmt, p, null, null));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCommand(PlayerCommandPreprocessEvent e) {
        Player p = e.getPlayer();
        String full = e.getMessage();

        String lower = full.toLowerCase(Locale.ROOT);
        if (lower.startsWith("/tell ") || lower.startsWith("/w ") || lower.startsWith("/msg ") || lower.startsWith("/whisper ")) {
            return;
        }

        String fmt = color(cfg().getString("spy.commandFormat", "&8[&cSPY&8] &7{nick} &8» &f{command}"));
        String out = applyCommonPlaceholders(fmt, p, null, null).replace("{command}", full);

        broadcastToSpies(out, p.getUniqueId());
    }

    public void broadcastToSpies(String msg, UUID exclude) {
        for (UUID u : spy) {
            if (u.equals(exclude)) continue;
            Player sp = Bukkit.getPlayer(u);
            if (sp != null && sp.isOnline() && sp.hasPermission("altumchat.spy.see")) {
                sp.sendMessage(msg);
            }
        }
    }
}

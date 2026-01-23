package me.altumchatmc.chat;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class AltumChatExpansion extends PlaceholderExpansion {

    private final AltumChatPlugin plugin;

    public AltumChatExpansion(AltumChatPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "altumchat";
    }

    @Override
    public @NotNull String getAuthor() {
        return "AltumChatMC";
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public @Nullable String onPlaceholderRequest(Player player, @NotNull String params) {
        if (player == null) return "";
        if (params.equalsIgnoreCase("platform")) {
            return plugin.platformTag(player);
        }
        if (params.equalsIgnoreCase("platform_raw")) {
            return plugin.isBedrock(player) ? "BEDROCK" : "JAVA";
        }
        return null;
    }
}

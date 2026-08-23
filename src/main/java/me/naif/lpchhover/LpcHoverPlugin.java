package me.naif.lpchhover;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.types.InheritanceNode;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.time.Duration;

public class LpcHoverPlugin extends JavaPlugin implements Listener {

    private LuckPerms luckPerms;

    @Override
    public void onEnable() {

        try {
            luckPerms = LuckPermsProvider.get();
        } catch (IllegalStateException e) {
            getLogger().severe("LuckPerms was not found!");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        getServer().getPluginManager().registerEvents(this, this);

        getLogger().info("LPC-Hover enabled!");
        getLogger().info("LuckPerms 5.3.86 hooked successfully!");
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {

        Player player = event.getPlayer();
        String message = event.getMessage();
        String name = player.getName();

        User user = luckPerms.getUserManager().getUser(player.getUniqueId());

        String rank = "Player";
        String expired = "LifeTime";

        if (user != null) {

            rank = user.getPrimaryGroup();

            /*
             * Find the group node belonging to the player's
             * current primary rank.
             */
            for (InheritanceNode node : user.getNodes().stream()
                    .filter(node -> node instanceof InheritanceNode)
                    .map(node -> (InheritanceNode) node)
                    .collect(java.util.stream.Collectors.toList())) {

                if (node.getGroupName().equalsIgnoreCase(rank)) {

                    if (node.hasExpiry()) {

                        Duration duration = node.getExpiryDuration();

                        if (duration != null) {
                            expired = formatDuration(duration);
                        } else {
                            expired = "Expired";
                        }

                    } else {
                        expired = "LifeTime";
                    }

                    break;
                }
            }
        }

        TextComponent component = new TextComponent(
                ChatColor.WHITE + name
        );

        component.setHoverEvent(new HoverEvent(
                HoverEvent.Action.SHOW_TEXT,
                new ComponentBuilder(
                        ChatColor.YELLOW + "Player: "
                                + ChatColor.WHITE + name
                                + "\n"
                                + ChatColor.YELLOW + "Rank: "
                                + ChatColor.WHITE + rank
                                + "\n"
                                + ChatColor.YELLOW + "Expired: "
                                + ChatColor.WHITE + expired
                ).create()
        ));

        component.setClickEvent(new ClickEvent(
                ClickEvent.Action.SUGGEST_COMMAND,
                "/msg " + name + " "
        ));

        TextComponent finalMessage = new TextComponent();

        finalMessage.addExtra(component);

        finalMessage.addExtra(
                new TextComponent(
                        ChatColor.WHITE + ": " + message
                )
        );

        for (Player online : getServer().getOnlinePlayers()) {
            online.spigot().sendMessage(finalMessage);
        }

        event.setCancelled(true);
    }

    private String formatDuration(Duration duration) {

        long seconds = duration.getSeconds();

        if (seconds <= 0) {
            return "Expired";
        }

        long days = seconds / 86400;
        seconds %= 86400;

        long hours = seconds / 3600;
        seconds %= 3600;

        long minutes = seconds / 60;

        StringBuilder result = new StringBuilder();

        if (days > 0) {
            result.append(days).append(" day");

            if (days != 1) {
                result.append("s");
            }
        }

        if (hours > 0) {

            if (result.length() > 0) {
                result.append(" ");
            }

            result.append(hours).append(" hour");

            if (hours != 1) {
                result.append("s");
            }
        }

        if (minutes > 0 && days == 0) {

            if (result.length() > 0) {
                result.append(" ");
            }

            result.append(minutes).append(" minute");

            if (minutes != 1) {
                result.append("s");
            }
        }

        if (result.length() == 0) {
            return "Less than 1 minute";
        }

        return result.toString();
    }
}

package me.naif.lpchhover;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.Node;
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
import java.util.UUID;

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

        getLogger().info("LPC-Hover-Nova enabled!");
        getLogger().info("LuckPerms hooked successfully!");
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {

        Player player = event.getPlayer();

        String message = event.getMessage();
        String name = player.getName();

        String rank = "Player";
        String expired = "LifeTime";

        /*
         * Get LuckPerms user
         */
        User user = luckPerms.getUserManager().getUser(player.getUniqueId());

        if (user != null) {

            /*
             * Get primary rank
             */
            rank = user.getPrimaryGroup();

            /*
             * Find the inheritance node
             * matching the primary rank.
             */
            for (Node node : user.getNodes()) {

                if (!(node instanceof InheritanceNode)) {
                    continue;
                }

                InheritanceNode inheritanceNode =
                        (InheritanceNode) node;

                if (!inheritanceNode.getGroupName()
                        .equalsIgnoreCase(rank)) {
                    continue;
                }

                /*
                 * Permanent rank
                 */
                if (!inheritanceNode.hasExpiry()) {

                    expired = "LifeTime";

                } else {

                    /*
                     * Temporary rank
                     */
                    Duration duration =
                            inheritanceNode.getExpiryDuration();

                    if (duration == null) {

                        expired = "Expired";

                    } else {

                        expired = formatDuration(duration);
                    }
                }

                break;
            }
        }

        /*
         * Account Age
         *
         * Bukkit stores the player's
         * first join timestamp.
         */
        String accountAge =
                getAccountAge(player.getFirstPlayed());

        /*
         * Player name
         */
        TextComponent component =
                new TextComponent(
                        ChatColor.WHITE + name
                );

        /*
         * Hover information
         */
        String hoverText =
                ChatColor.YELLOW + "Player: "
                        + ChatColor.WHITE + name
                        + "\n"
                        + ChatColor.YELLOW + "Rank: "
                        + ChatColor.WHITE + rank
                        + "\n"
                        + ChatColor.YELLOW + "Expired: "
                        + ChatColor.WHITE + expired
                        + "\n"
                        + ChatColor.YELLOW + "Account Age: "
                        + ChatColor.WHITE + accountAge;

        component.setHoverEvent(
                new HoverEvent(
                        HoverEvent.Action.SHOW_TEXT,
                        new ComponentBuilder(
                                hoverText
                        ).create()
                )
        );

        /*
         * Click player name to suggest /msg
         */
        component.setClickEvent(
                new ClickEvent(
                        ClickEvent.Action.SUGGEST_COMMAND,
                        "/msg " + name + " "
                )
        );

        /*
         * Final chat message
         */
        TextComponent finalMessage =
                new TextComponent();

        finalMessage.addExtra(component);

        finalMessage.addExtra(
                new TextComponent(
                        ChatColor.WHITE
                                + ": "
                                + message
                )
        );

        /*
         * Send message to everyone
         */
        for (Player online :
                getServer().getOnlinePlayers()) {

            online.spigot().sendMessage(
                    finalMessage
            );
        }

        /*
         * Prevent the normal chat message
         */
        event.setCancelled(true);
    }

    /*
     * Format rank expiration duration
     */
    private String formatDuration(Duration duration) {

        long seconds =
                duration.getSeconds();

        if (seconds <= 0) {
            return "Expired";
        }

        long days =
                seconds / 86400;

        seconds %= 86400;

        long hours =
                seconds / 3600;

        seconds %= 3600;

        long minutes =
                seconds / 60;

        StringBuilder result =
                new StringBuilder();

        if (days > 0) {

            result.append(days)
                    .append(" day");

            if (days != 1) {
                result.append("s");
            }
        }

        if (hours > 0) {

            if (result.length() > 0) {
                result.append(" ");
            }

            result.append(hours)
                    .append(" hour");

            if (hours != 1) {
                result.append("s");
            }
        }

        if (minutes > 0 && days == 0) {

            if (result.length() > 0) {
                result.append(" ");
            }

            result.append(minutes)
                    .append(" minute");

            if (minutes != 1) {
                result.append("s");
            }
        }

        if (result.length() == 0) {
            return "Less than 1 minute";
        }

        return result.toString();
    }

    /*
     * Calculate Account Age
     * from player's first join.
     */
    private String getAccountAge(long firstPlayed) {

        if (firstPlayed <= 0) {
            return "Unknown";
        }

        long now =
                System.currentTimeMillis();

        long difference =
                now - firstPlayed;

        if (difference < 0) {
            return "Unknown";
        }

        Duration duration =
                Duration.ofMillis(
                        difference
                );

        long seconds =
                duration.getSeconds();

        long days =
                seconds / 86400;

        seconds %= 86400;

        long hours =
                seconds / 3600;

        seconds %= 3600;

        long minutes =
                seconds / 60;

        /*
         * Years
         */
        long years =
                days / 365;

        days %= 365;

        /*
         * Months
         */
        long months =
                days / 30;

        days %= 30;

        StringBuilder result =
                new StringBuilder();

        if (years > 0) {

            result.append(years)
                    .append(" year");

            if (years != 1) {
                result.append("s");
            }
        }

        if (months > 0) {

            if (result.length() > 0) {
                result.append(" ");
            }

            result.append(months)
                    .append(" month");

            if (months != 1) {
                result.append("s");
            }
        }

        if (days > 0) {

            if (result.length() > 0) {
                result.append(" ");
            }

            result.append(days)
                    .append(" day");

            if (days != 1) {
                result.append("s");
            }
        }

        /*
         * For accounts younger than one day
         */
        if (result.length() == 0) {

            if (hours > 0) {

                result.append(hours)
                        .append(" hour");

                if (hours != 1) {
                    result.append("s");
                }

            } else if (minutes > 0) {

                result.append(minutes)
                        .append(" minute");

                if (minutes != 1) {
                    result.append("s");
                }

            } else {

                return "Less than 1 minute";
            }
        }

        return result.toString();
    }
}

package me.naif.lpchhover;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.Node;
import net.luckperms.api.node.types.InheritanceNode;

import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;

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
        } catch (Exception e) {
            getLogger().severe("Could not hook into LuckPerms!");
            e.printStackTrace();
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

        String name = player.getName();
        String message = event.getMessage();

        String rank = "Player";
        String expired = "LifeTime";

        /*
         * Get LuckPerms user
         */
        User user = luckPerms
                .getUserManager()
                .getUser(player.getUniqueId());

        if (user != null) {

            rank = user.getPrimaryGroup();

            /*
             * Find temporary/permanent
             * inheritance node.
             */
            for (Node node : user.getNodes()) {

                if (!(node instanceof InheritanceNode)) {
                    continue;
                }

                InheritanceNode inheritance =
                        (InheritanceNode) node;

                if (!inheritance.getGroupName()
                        .equalsIgnoreCase(rank)) {
                    continue;
                }

                if (!inheritance.hasExpiry()) {

                    expired = "LifeTime";

                } else {

                    Duration duration =
                            inheritance.getExpiryDuration();

                    if (duration != null) {
                        expired = formatDuration(duration);
                    } else {
                        expired = "Expired";
                    }
                }

                break;
            }
        }

        /*
         * Account Age
         */
        String accountAge =
                getAccountAge(player.getFirstPlayed());

        /*
         * Player name
         */
        TextComponent nameComponent =
                new TextComponent(
                        ChatColor.WHITE + name
                );

        /*
         * Build Hover line by line
         */
        TextComponent hover =
                new TextComponent();

        TextComponent line1 =
                new TextComponent(
                        ChatColor.YELLOW + "Player: "
                                + ChatColor.WHITE
                                + name
                );

        TextComponent line2 =
                new TextComponent(
                        ChatColor.YELLOW + "\nRank: "
                                + ChatColor.WHITE
                                + rank
                );

        TextComponent line3 =
                new TextComponent(
                        ChatColor.YELLOW + "\nExpired: "
                                + ChatColor.WHITE
                                + expired
                );

        TextComponent line4 =
                new TextComponent(
                        ChatColor.YELLOW + "\nAccount Age: "
                                + ChatColor.WHITE
                                + accountAge
                );

        hover.addExtra(line1);
        hover.addExtra(line2);
        hover.addExtra(line3);
        hover.addExtra(line4);

        /*
         * Hover
         */
        nameComponent.setHoverEvent(
                new HoverEvent(
                        HoverEvent.Action.SHOW_TEXT,
                        new TextComponent[]{
                                hover
                        }
                )
        );

        /*
         * Click -> /msg
         */
        nameComponent.setClickEvent(
                new ClickEvent(
                        ClickEvent.Action.SUGGEST_COMMAND,
                        "/msg " + name + " "
                )
        );

        /*
         * Final message
         */
        TextComponent finalMessage =
                new TextComponent();

        finalMessage.addExtra(nameComponent);

        finalMessage.addExtra(
                new TextComponent(
                        ChatColor.WHITE
                                + ": "
                                + message
                )
        );

        /*
         * Send to players
         */
        for (Player online :
                getServer().getOnlinePlayers()) {

            online.spigot().sendMessage(
                    finalMessage
            );
        }

        event.setCancelled(true);
    }

    /*
     * Format rank expiration
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
     * Account Age
     */
    private String getAccountAge(long firstPlayed) {

        if (firstPlayed <= 0) {
            return "Unknown";
        }

        long difference =
                System.currentTimeMillis()
                        - firstPlayed;

        if (difference < 0) {
            return "Unknown";
        }

        long totalSeconds =
                difference / 1000;

        long days =
                totalSeconds / 86400;

        totalSeconds %= 86400;

        long hours =
                totalSeconds / 3600;

        totalSeconds %= 3600;

        long minutes =
                totalSeconds / 60;

        long years =
                days / 365;

        days %= 365;

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

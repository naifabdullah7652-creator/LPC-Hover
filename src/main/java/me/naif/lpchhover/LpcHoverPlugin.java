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

public class LpcHoverPlugin extends JavaPlugin implements Listener {

    private LuckPerms luckPerms;

    @Override
    public void onEnable() {

        try {
            luckPerms = LuckPermsProvider.get();
        } catch (Exception e) {
            getLogger().severe("LuckPerms not found!");
            e.printStackTrace();
            return;
        }

        getServer().getPluginManager().registerEvents(this, this);

        getLogger().info("LPC-Hover enabled!");
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {

        Player player = event.getPlayer();

        String message = event.getMessage();
        String name = player.getName();

        String rank = "Player";
        String expired = "LifeTime";

        /*
         * LuckPerms
         */
        if (luckPerms != null) {

            User user = luckPerms
                    .getUserManager()
                    .getUser(player.getUniqueId());

            if (user != null) {

                rank = user.getPrimaryGroup();

                /*
                 * Check rank expiration
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

                    if (inheritance.hasExpiry()) {

                        Duration duration =
                                inheritance.getExpiryDuration();

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

        /*
         * Account Age
         */
        String accountAge =
                getAccountAge(player.getFirstPlayed());

        TextComponent component = new TextComponent(
                ChatColor.WHITE + name
        );

        /*
         * Hover
         */
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
                                + "\n"
                                + ChatColor.YELLOW + "Account Age: "
                                + ChatColor.WHITE + accountAge
                ).create()
        ));

        /*
         * Click
         */
        component.setClickEvent(new ClickEvent(
                ClickEvent.Action.SUGGEST_COMMAND,
                "/msg " + name + " "
        ));

        TextComponent finalMessage =
                new TextComponent();

        finalMessage.addExtra(component);

        finalMessage.addExtra(
                new TextComponent(
                        ChatColor.WHITE + ": " + message
                )
        );

        for (Player online :
                getServer().getOnlinePlayers()) {

            online.spigot().sendMessage(
                    finalMessage
            );
        }

        event.setCancelled(true);
    }

    /*
     * Rank duration
     */
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

        if (minutes > 0) {

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

        long days =
                difference / 86400000L;

        long years =
                days / 365;

        days %= 365;

        long months =
                days / 30;

        days %= 30;

        long hours =
                (difference / 3600000L) % 24;

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

        if (result.length() == 0 && hours > 0) {

            result.append(hours)
                    .append(" hour");

            if (hours != 1) {
                result.append("s");
            }
        }

        if (result.length() == 0) {
            return "Less than 1 hour";
        }

        return result.toString();
    }
}

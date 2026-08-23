package com.infiniteplugins.lpc;

import me.clip.placeholderapi.PlaceholderAPI;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.cacheddata.CachedMetaData;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.Node;
import net.luckperms.api.node.types.InheritanceNode;

import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public final class LPC extends JavaPlugin implements Listener {

    private static final Pattern HEX_PATTERN =
            Pattern.compile("&#([A-Fa-f0-9]{6})");

    private static final Pattern BUKKIT_HEX_PATTERN =
            Pattern.compile("&x(&[A-Fa-f0-9]){6}");

    private static final String NAME_MARKER =
            "\uE000LPC_NAME\uE001";

    private LuckPerms luckPerms;

    @Override
    public void onEnable() {

        luckPerms =
                getServer()
                        .getServicesManager()
                        .load(LuckPerms.class);

        if (luckPerms == null) {

            getLogger().severe(
                    "LuckPerms not found! LPC requires LuckPerms."
            );

            getServer()
                    .getPluginManager()
                    .disablePlugin(this);

            return;
        }

        saveDefaultConfig();

        getServer()
                .getPluginManager()
                .registerEvents(this, this);

        getLogger().info(
                "LPC-Hover-Nova enabled for Minecraft 1.8.8."
        );
    }

    @Override
    public boolean onCommand(
            CommandSender sender,
            Command command,
            String label,
            String[] args) {

        if (args.length == 1
                && "reload".equalsIgnoreCase(args[0])
                && sender.hasPermission("lpc.reload")) {

            reloadConfig();

            sender.sendMessage(
                    colorize("&aLPC has been reloaded.")
            );

            return true;
        }

        if (args.length == 1
                && "clear".equalsIgnoreCase(args[0])
                && sender.hasPermission("lpc.clearchat")) {

            for (Player player :
                    getServer().getOnlinePlayers()) {

                for (int i = 0; i < 100; i++) {
                    player.sendMessage("");
                }
            }

            String clearMessage =
                    getConfig().getString(
                            "clear-chat-message",
                            "&7Chat has been cleared."
                    );

            getServer().broadcastMessage(
                    colorize(clearMessage)
            );

            return true;
        }

        if (args.length == 2
                && "debug".equalsIgnoreCase(args[0])
                && sender.hasPermission("lpc.debug")) {

            Player target =
                    getServer().getPlayer(args[1]);

            if (target == null) {

                sender.sendMessage(
                        colorize("&cPlayer not found.")
                );

                return true;
            }

            CachedMetaData meta =
                    luckPerms
                            .getPlayerAdapter(Player.class)
                            .getMetaData(target);

            sender.sendMessage(
                    colorize(
                            "&6&lLPC Debug: &f"
                                    + target.getName()
                    )
            );

            sender.sendMessage(
                    colorize(
                            "&7Primary Group: &f"
                                    + meta.getPrimaryGroup()
                    )
            );

            sender.sendMessage(
                    colorize(
                            "&7Rank Expired: &f"
                                    + getRankExpiration(target)
                    )
            );

            sender.sendMessage(
                    colorize(
                            "&7Account Age: &f"
                                    + getAccountAge(target)
                    )
            );

            return true;
        }

        return false;
    }

    @Override
    public List<String> onTabComplete(
            CommandSender sender,
            Command command,
            String alias,
            String[] args) {

        List<String> completions =
                new ArrayList<String>();

        if (args.length == 1) {

            String input =
                    args[0].toLowerCase();

            if (sender.hasPermission("lpc.reload")
                    && "reload".startsWith(input)) {

                completions.add("reload");
            }

            if (sender.hasPermission("lpc.clearchat")
                    && "clear".startsWith(input)) {

                completions.add("clear");
            }

            if (sender.hasPermission("lpc.debug")
                    && "debug".startsWith(input)) {

                completions.add("debug");
            }
        }

        else if (args.length == 2
                && "debug".equalsIgnoreCase(args[0])
                && sender.hasPermission("lpc.debug")) {

            return getServer()
                    .getOnlinePlayers()
                    .stream()
                    .map(Player::getName)
                    .filter(name ->
                            name.toLowerCase()
                                    .startsWith(
                                            args[1].toLowerCase()
                                    )
                    )
                    .collect(Collectors.toList());
        }

        return completions;
    }

    @EventHandler(
            priority = EventPriority.HIGHEST,
            ignoreCancelled = true
    )
    public void onChat(
            AsyncPlayerChatEvent event) {

        Player player =
                event.getPlayer();

        String message =
                processMessage(
                        player,
                        event.getMessage()
                );

        String format =
                buildFormat(player);

        event.setCancelled(true);

        BaseComponent[] components =
                buildChatComponents(
                        player,
                        format,
                        message
                );

        for (Player recipient :
                event.getRecipients()) {

            recipient.spigot()
                    .sendMessage(components);
        }
    }

    private BaseComponent[] buildChatComponents(
            Player player,
            String format,
            String processedMessage) {

        String full =
                format.replace(
                        "{message}",
                        processedMessage
                );

        BaseComponent[] parsed =
                TextComponent.fromLegacyText(full);

        List<BaseComponent> result =
                new ArrayList<BaseComponent>();

        boolean markerFound = false;

        for (BaseComponent component : parsed) {

            if (!(component instanceof TextComponent)) {

                result.add(component);
                continue;
            }

            TextComponent original =
                    (TextComponent) component;

            String text =
                    original.getText();

            if (text == null
                    || !text.contains(NAME_MARKER)) {

                result.add(component);
                continue;
            }

            String[] pieces =
                    text.split(
                            Pattern.quote(NAME_MARKER),
                            -1
                    );

            for (int i = 0;
                 i < pieces.length;
                 i++) {

                if (!pieces[i].isEmpty()) {

                    TextComponent part =
                            new TextComponent(original);

                    part.setText(pieces[i]);

                    result.add(part);
                }

                if (i < pieces.length - 1) {

                    TextComponent name =
                            new TextComponent(original);

                    name.setText(
                            player.getName()
                    );

                    String hoverText =
                            "&6Player: &f"
                                    + player.getName()
                                    + "\n"
                                    + "&6Rank: &f"
                                    + getRank(player)
                                    + "\n"
                                    + "&6Expired: &f"
                                    + getRankExpiration(player)
                                    + "\n"
                                    + "&6Account Age: &f"
                                    + getAccountAge(player);

                    name.setHoverEvent(
                            new HoverEvent(
                                    HoverEvent.Action.SHOW_TEXT,
                                    new ComponentBuilder(
                                            colorize(hoverText)
                                    ).create()
                            )
                    );

                    name.setClickEvent(
                            new ClickEvent(
                                    ClickEvent.Action.SUGGEST_COMMAND,
                                    "/msg "
                                            + player.getName()
                                            + " "
                            )
                    );

                    result.add(name);

                    markerFound = true;
                }
            }
        }

        if (!markerFound) {
            return parsed;
        }

        return result.toArray(
                new BaseComponent[result.size()]
        );
    }

    private String getRank(
            Player player) {

        CachedMetaData meta =
                luckPerms
                        .getPlayerAdapter(Player.class)
                        .getMetaData(player);

        String group =
                meta.getPrimaryGroup();

        return group != null
                ? group
                : "default";
    }

    private String getRankExpiration(
            Player player) {

        User user =
                luckPerms
                        .getUserManager()
                        .getUser(player.getUniqueId());

        if (user == null) {
            return "LifeTime";
        }

        String primaryGroup =
                getRank(player);

        for (Node node :
                user.getNodes()) {

            if (!(node instanceof InheritanceNode)) {
                continue;
            }

            InheritanceNode inheritance =
                    (InheritanceNode) node;

            if (!inheritance
                    .getGroupName()
                    .equalsIgnoreCase(primaryGroup)) {

                continue;
            }

            if (!node.hasExpiry()) {
                return "LifeTime";
            }

            Duration duration =
                    node.getExpiryDuration();

            if (duration == null) {
                return "LifeTime";
            }

            if (duration.isZero()
                    || duration.isNegative()) {

                return "Expired";
            }

            return formatDuration(duration);
        }

        return "LifeTime";
    }

    private String formatDuration(
            Duration duration) {

        long seconds =
                duration.getSeconds();

        if (seconds <= 0) {
            return "Expired";
        }

        long days =
                seconds / 86400L;

        seconds %= 86400L;

        long hours =
                seconds / 3600L;

        seconds %= 3600L;

        long minutes =
                seconds / 60L;

        StringBuilder result =
                new StringBuilder();

        if (days > 0) {

            result.append(days)
                    .append(
                            days == 1
                                    ? " day"
                                    : " days"
                    );
        }

        if (hours > 0) {

            if (result.length() > 0) {
                result.append(" ");
            }

            result.append(hours)
                    .append(
                            hours == 1
                                    ? " hour"
                                    : " hours"
                    );
        }

        if (minutes > 0) {

            if (result.length() > 0) {
                result.append(" ");
            }

            result.append(minutes)
                    .append(
                            minutes == 1
                                    ? " minute"
                                    : " minutes"
                    );
        }

        if (result.length() == 0) {
            return "Less than 1 minute";
        }

        return result.toString();
    }

    private String getAccountAge(
            Player player) {

        long firstPlayed =
                player.getFirstPlayed();

        if (firstPlayed <= 0) {
            return "Unknown";
        }

        long seconds =
                (System.currentTimeMillis()
                        - firstPlayed) / 1000L;

        if (seconds < 0) {
            return "Unknown";
        }

        long days =
                seconds / 86400L;

        seconds %= 86400L;

        long hours =
                seconds / 3600L;

        StringBuilder result =
                new StringBuilder();

        if (days > 0) {

            result.append(days)
                    .append(
                            days == 1
                                    ? " day"
                                    : " days"
                    );
        }

        if (hours > 0
                && result.length() == 0) {

            result.append(hours)
                    .append(
                            hours == 1
                                    ? " hour"
                                    : " hours"
                    );
        }

        if (result.length() == 0) {
            return "Less than 1 hour";
        }

        return result.toString();
    }

    String buildFormat(
            Player player) {

        CachedMetaData meta =
                luckPerms
                        .getPlayerAdapter(Player.class)
                        .getMetaData(player);

        String group =
                meta.getPrimaryGroup();

        String format;

        if (getConfig().getString(
                "group-formats." + group
        ) != null) {

            format =
                    getConfig().getString(
                            "group-formats." + group
                    );

        } else {

            format =
                    getConfig().getString(
                            "chat-format"
                    );
        }

        if (format == null) {
            format = "{prefix}{name}&r: {message}";
        }

        String prefix =
                meta.getPrefix();

        String suffix =
                meta.getSuffix();

        String usernameColor =
                meta.getMetaValue(
                        "username-color"
                );

        String messageColor =
                meta.getMetaValue(
                        "message-color"
                );

        format = format
                .replace(
                        "{prefix}",
                        prefix != null
                                ? prefix
                                : ""
                )
                .replace(
                        "{suffix}",
                        suffix != null
                                ? suffix
                                : ""
                )
                .replace(
                        "{prefixes}",
                        meta.getPrefixes()
                                .values()
                                .stream()
                                .filter(value ->
                                        value != null)
                                .collect(
                                        Collectors.joining()
                                )
                )
                .replace(
                        "{suffixes}",
                        meta.getSuffixes()
                                .values()
                                .stream()
                                .filter(value ->
                                        value != null)
                                .collect(
                                        Collectors.joining()
                                )
                )
                .replace(
                        "{world}",
                        player.getWorld()
                                .getName()
                )
                .replace(
                        "{name}",
                        NAME_MARKER
                )
                .replace(
                        "{displayname}",
                        player.getDisplayName()
                )
                .replace(
                        "{username-color}",
                        usernameColor != null
                                ? usernameColor
                                : ""
                )
                .replace(
                        "{message-color}",
                        messageColor != null
                                ? messageColor
                                : ""
                );

        format =
                translateHexColorCodes(format);

        if (getServer()
                .getPluginManager()
                .isPluginEnabled(
                        "PlaceholderAPI"
                )) {

            format =
                    PlaceholderAPI.setPlaceholders(
                            player,
                            format
                    );
        }

        return colorize(
                translateHexColorCodes(format)
        );
    }

    String processMessage(
            Player player,
            String message) {

        boolean colors =
                player.hasPermission(
                        "lpc.colorcodes"
                );

        boolean rgb =
                player.hasPermission(
                        "lpc.rgbcodes"
                );

        if (colors && rgb) {

            return colorize(
                    translateHexColorCodes(
                            message
                    )
            );

        } else if (colors) {

            return colorize(
                    stripHexCodes(message)
            );

        } else if (rgb) {

            return stripColorCodes(
                    translateHexColorCodes(
                            message
                    )
            );

        } else {

            return stripColorCodes(
                    stripHexCodes(message)
            );
        }
    }

    String colorize(
            String message) {

        return ChatColor
                .translateAlternateColorCodes(
                        '&',
                        message
                );
    }

    String translateHexColorCodes(
            String message) {

        final char colorChar =
                ChatColor.COLOR_CHAR;

        Matcher matcher =
                HEX_PATTERN.matcher(message);

        StringBuffer buffer =
                new StringBuffer(
                        message.length() + 32
                );

        while (matcher.find()) {

            String group =
                    matcher.group(1);

            matcher.appendReplacement(
                    buffer,
                    colorChar
                            + "x"
                            + colorChar
                            + group.charAt(0)
                            + colorChar
                            + group.charAt(1)
                            + colorChar
                            + group.charAt(2)
                            + colorChar
                            + group.charAt(3)
                            + colorChar
                            + group.charAt(4)
                            + colorChar
                            + group.charAt(5)
            );
        }

        String result =
                matcher.appendTail(buffer)
                        .toString();

        matcher =
                BUKKIT_HEX_PATTERN.matcher(result);

        StringBuffer bukkitBuffer =
                new StringBuffer(
                        result.length()
                );

        while (matcher.find()) {

            matcher.appendReplacement(
                    bukkitBuffer,
                    matcher.group()
                            .replace(
                                    '&',
                                    colorChar
                            )
            );
        }

        return matcher.appendTail(
                bukkitBuffer
        ).toString();
    }

    String stripColorCodes(
            String message) {

        return message.replaceAll(
                "&[0-9a-fA-Fk-oK-OrR]",
                ""
        );
    }

    String stripHexCodes(
            String message) {

        String result =
                message.replaceAll(
                        "&#[0-9a-fA-F]{6}",
                        ""
                );

        result =
                result.replaceAll(
                        "&x(&[0-9a-fA-F]){6}",
                        ""
                );

        return result;
    }
}

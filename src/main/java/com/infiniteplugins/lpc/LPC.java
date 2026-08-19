package com.infiniteplugins.lpc;

import me.clip.placeholderapi.PlaceholderAPI;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.cacheddata.CachedMetaData;
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

        this.luckPerms =
                getServer().getServicesManager().load(LuckPerms.class);

        if (this.luckPerms == null) {
            getLogger().severe(
                    "LuckPerms not found! LPC requires LuckPerms to function."
            );

            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        saveDefaultConfig();

        getServer().getPluginManager().registerEvents(this, this);

        final String[] chatPlugins = {
                "EssentialsChat",
                "VentureChat",
                "HeroChat",
                "DeluxeChat",
                "ChatManager",
                "ChatEx",
                "UltraChat",
                "TownyChat"
        };

        for (final String pluginName : chatPlugins) {

            if (getServer().getPluginManager()
                    .isPluginEnabled(pluginName)) {

                getLogger().warning(
                        "Detected " + pluginName
                                + " which may also format chat. "
                                + "Disable its chat formatting to avoid "
                                + "duplicate chat messages."
                );
            }
        }

        getLogger().info(
                "LPC-Hover 3.7.2 enabled for Spigot/Paper 1.8.8."
        );
    }

    @Override
    public boolean onCommand(
            final CommandSender sender,
            final Command command,
            final String label,
            final String[] args) {

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

            for (final Player player :
                    getServer().getOnlinePlayers()) {

                for (int i = 0; i < 100; i++) {
                    player.sendMessage("");
                }
            }

            final String clearMessage =
                    getConfig().getString(
                            "clear-chat-message",
                            "&7Chat has been cleared by a staff member."
                    );

            getServer().broadcastMessage(
                    colorize(clearMessage)
            );

            return true;
        }

        if (args.length == 2
                && "debug".equalsIgnoreCase(args[0])
                && sender.hasPermission("lpc.debug")) {

            final Player target =
                    getServer().getPlayer(args[1]);

            if (target == null) {

                sender.sendMessage(
                        colorize("&cPlayer not found.")
                );

                return true;
            }

            final CachedMetaData meta =
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
                            "&7Prefix: &f"
                                    + (meta.getPrefix() != null
                                    ? meta.getPrefix()
                                    : "&cnone")
                    )
            );

            sender.sendMessage(
                    colorize(
                            "&7Suffix: &f"
                                    + (meta.getSuffix() != null
                                    ? meta.getSuffix()
                                    : "&cnone")
                    )
            );

            sender.sendMessage(
                    colorize(
                            "&7Username-color: &f"
                                    + (meta.getMetaValue(
                                    "username-color") != null
                                    ? meta.getMetaValue(
                                    "username-color")
                                    : "&cnone")
                    )
            );

            sender.sendMessage(
                    colorize(
                            "&7Message-color: &f"
                                    + (meta.getMetaValue(
                                    "message-color") != null
                                    ? meta.getMetaValue(
                                    "message-color")
                                    : "&cnone")
                    )
            );

            return true;
        }

        return false;
    }

    @Override
    public List<String> onTabComplete(
            final CommandSender sender,
            final Command command,
            final String alias,
            final String[] args) {

        final List<String> completions =
                new ArrayList<>();

        if (args.length == 1) {

            final String input =
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

        } else if (args.length == 2
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
            final AsyncPlayerChatEvent event) {

        final Player player =
                event.getPlayer();

        final String processedMessage =
                processMessage(
                        player,
                        event.getMessage()
                );

        final String format =
                buildFormat(player);

        /*
         * Cancel the original chat event.
         * We will send our own BungeeCord components.
         */
        event.setCancelled(true);

        final BaseComponent[] components =
                buildChatComponents(
                        player,
                        format,
                        processedMessage
                );

        for (final Player recipient :
                event.getRecipients()) {

            recipient.spigot()
                    .sendMessage(components);
        }
    }

    private BaseComponent[] buildChatComponents(
            final Player player,
            final String format,
            final String processedMessage) {

        final String full =
                format.replace(
                        "{message}",
                        processedMessage
                );

        final BaseComponent[] parsed =
                TextComponent.fromLegacyText(full);

        final List<BaseComponent> result =
                new ArrayList<>();

        boolean markerFound = false;

        for (final BaseComponent component :
                parsed) {

            if (!(component instanceof TextComponent)) {
                result.add(component);
                continue;
            }

            final TextComponent original =
                    (TextComponent) component;

            final String text =
                    original.getText();

            if (text == null
                    || !text.contains(NAME_MARKER)) {

                result.add(component);
                continue;
            }

            final String[] pieces =
                    text.split(
                            Pattern.quote(NAME_MARKER),
                            -1
                    );

            for (int i = 0;
                 i < pieces.length;
                 i++) {

                /*
                 * IMPORTANT:
                 * Do NOT use copyFormatting().
                 *
                 * The BungeeCord API available with
                 * Spigot 1.8.8 does not provide it.
                 *
                 * Copy the entire TextComponent instead.
                 */
                if (!pieces[i].isEmpty()) {

                    TextComponent part =
                            new TextComponent(original);

                    part.setText(pieces[i]);

                    result.add(part);
                }

                /*
                 * Replace NAME_MARKER with the real
                 * player's name.
                 */
                if (i < pieces.length - 1) {

                    TextComponent name =
                            new TextComponent(original);

                    name.setText(
                            player.getName()
                    );

                    /*
                     * Hover information.
                     */
                    name.setHoverEvent(
                            new HoverEvent(
                                    HoverEvent.Action.SHOW_TEXT,
                                    new ComponentBuilder(
                                            colorize(
                                                    "&6Player: &f"
                                                            + player.getName()
                                                            + "\n"
                                                            + "&6Rank: &f"
                                                            + getRank(player)
                                            )
                                    ).create()
                            )
                    );

                    /*
                     * Click on name:
                     * /msg PlayerName
                     */
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

        /*
         * Safety fallback.
         */
        if (!markerFound) {
            return parsed;
        }

        return result.toArray(
                new BaseComponent[result.size()]
        );
    }

    private String getRank(
            final Player player) {

        final CachedMetaData meta =
                luckPerms
                        .getPlayerAdapter(Player.class)
                        .getMetaData(player);

        final String group =
                meta.getPrimaryGroup();

        return group != null
                ? group
                : "default";
    }

    String buildFormat(
            final Player player) {

        final CachedMetaData meta =
                luckPerms
                        .getPlayerAdapter(Player.class)
                        .getMetaData(player);

        final String group =
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

            format =
                    "{prefix}{name}&r: {message}";
        }

        final String prefix =
                meta.getPrefix();

        final String suffix =
                meta.getSuffix();

        final String usernameColor =
                meta.getMetaValue(
                        "username-color"
                );

        final String messageColor =
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

        /*
         * PlaceholderAPI is optional.
         */
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
            final Player player,
            final String message) {

        final boolean colors =
                player.hasPermission(
                        "lpc.colorcodes"
                );

        final boolean rgb =
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
            final String message) {

        return ChatColor
                .translateAlternateColorCodes(
                        '&',
                        message
                );
    }

    String translateHexColorCodes(
            final String message) {

        final char colorChar =
                ChatColor.COLOR_CHAR;

        Matcher matcher =
                HEX_PATTERN.matcher(message);

        final StringBuffer buffer =
                new StringBuffer(
                        message.length() + 32
                );

        while (matcher.find()) {

            final String group =
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

        final StringBuffer bukkitBuffer =
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
            final String message) {

        return message.replaceAll(
                "&[0-9a-fA-Fk-oK-OrR]",
                ""
        );
    }

    String stripHexCodes(
            final String message) {

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

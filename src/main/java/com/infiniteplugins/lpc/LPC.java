package com.infiniteplugins.lpc;

import me.clip.placeholderapi.PlaceholderAPI;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.cacheddata.CachedMetaData;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
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

    /*
     * Private marker used internally to identify the player's name.
     * It is replaced with a real clickable/hoverable component later.
     */
    private static final String NAME_MARKER = "\uE000LPC_NAME\uE001";

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
                                + "To avoid message duplication, disable "
                                + "chat formatting in " + pluginName + "."
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

        /*
         * /lpc reload
         */
        if (args.length == 1
                && "reload".equalsIgnoreCase(args[0])
                && sender.hasPermission("lpc.reload")) {

            reloadConfig();

            sender.sendMessage(
                    colorize("&aLPC has been reloaded.")
            );

            return true;
        }

        /*
         * /lpc clear
         */
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

        /*
         * /lpc debug <player>
         */
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

            final CachedMetaData debugMeta =
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
                                    + debugMeta.getPrimaryGroup()
                    )
            );

            sender.sendMessage(
                    colorize(
                            "&7Prefix: &f"
                                    + (debugMeta.getPrefix() != null
                                    ? debugMeta.getPrefix()
                                    : "&cnone")
                    )
            );

            sender.sendMessage(
                    colorize(
                            "&7Suffix: &f"
                                    + (debugMeta.getSuffix() != null
                                    ? debugMeta.getSuffix()
                                    : "&cnone")
                    )
            );

            sender.sendMessage(
                    colorize(
                            "&7Username-color: &f"
                                    + (debugMeta.getMetaValue(
                                    "username-color") != null
                                    ? debugMeta.getMetaValue(
                                    "username-color")
                                    : "&cnone")
                    )
            );

            sender.sendMessage(
                    colorize(
                            "&7Message-color: &f"
                                    + (debugMeta.getMetaValue(
                                    "message-color") != null
                                    ? debugMeta.getMetaValue(
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
    public void onChat(final AsyncPlayerChatEvent event) {

        final Player player = event.getPlayer();

        final String processedMessage =
                processMessage(player, event.getMessage());

        final String format =
                buildFormat(player);

        /*
         * Cancel the normal Spigot chat event and send
         * our own BungeeCord component message.
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

            recipient.spigot().sendMessage(components);
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

        for (final BaseComponent component : parsed) {

            if (!(component instanceof TextComponent)) {
                result.add(component);
                continue;
            }

            final TextComponent textComponent =
                    (TextComponent) component;

            final String text =
                    textComponent.getText();

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
                 * Normal text before/after the player's name.
                 *
                 * IMPORTANT:
                 * We clone the original TextComponent instead
                 * of using copyFormatting(), because the old
                 * BungeeCord API used by Minecraft 1.8.8 does
                 * not contain copyFormatting().
                 */
                if (!pieces[i].isEmpty()) {

                    TextComponent part =
                            new TextComponent(textComponent);

                    part.setText(pieces[i]);

                    result.add(part);
                }

                /*
                 * Actual player name component.
                 */
                if (i < pieces.length - 1) {

                    TextComponent name =
                            new TextComponent(textComponent);

                    name.setText(player.getName());

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
                     * Click -> opens /msg player
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

    private String getRank(final Player player) {

        final CachedMetaData metaData =
                luckPerms
                        .getPlayerAdapter(Player.class)
                        .getMetaData(player);

        return metaData.getPrimaryGroup() != null
                ? metaData.getPrimaryGroup()
                : "default";
    }

    String buildFormat(final Player player) {

        final CachedMetaData metaData =
                luckPerms
                        .getPlayerAdapter(Player.class)
                        .getMetaData(player);

        final String group =
                metaData.getPrimaryGroup();

        String format =
                getConfig().getString(
                        getConfig().getString(
                                "group-formats." + group
                        ) != null
                                ? "group-formats." + group
                                : "chat-format"
                );

        if (format == null) {
            format =
                    "{prefix}{name}&r: {message}";
        }

        final String prefix =
                metaData.getPrefix();

        final String suffix =
                metaData.getSuffix();

        final String usernameColor =
                metaData.getMetaValue(
                        "username-color"
                );

        final String messageColor =
                metaData.getMetaValue(
                        "message-color"
                );

        format = format
                .replace(
                        "{prefix}",
                        prefix != null ? prefix : ""
                )
                .replace(
                        "{suffix}",
                        suffix != null ? suffix : ""
                )
                .replace(
                        "{prefixes}",
                        metaData.getPrefixes()
                                .keySet()
                                .stream()
                                .map(key ->
                                        metaData
                                                .getPrefixes()
                                                .get(key)
                                )
                                .collect(Collectors.joining())
                )
                .replace(
                        "{suffixes}",
                        metaData.getSuffixes()
                                .keySet()
                                .stream()
                                .map(key ->
                                        metaData
                                                .getSuffixes()
                                                .get(key)
                                )
                                .collect(Collectors.joining())
                )
                .replace(
                        "{world}",
                        player.getWorld().getName()
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
                .isPluginEnabled("PlaceholderAPI")) {

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

        if (player.hasPermission("lpc.colorcodes")
                && player.hasPermission("lpc.rgbcodes")) {

            return colorize(
                    translateHexColorCodes(message)
            );

        } else if (player.hasPermission("lpc.colorcodes")) {

            return colorize(
                    stripHexCodes(message)
            );

        } else if (player.hasPermission("lpc.rgbcodes")) {

            return stripColorCodes(
                    translateHexColorCodes(message)
            );

        } else {

            return stripColorCodes(
                    stripHexCodes(message)
            );
        }
    }

    String colorize(final String message) {

        return ChatColor.translateAlternateColorCodes(
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
                        message.length() + 4 * 8
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
                new StringBuffer(result.length());

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

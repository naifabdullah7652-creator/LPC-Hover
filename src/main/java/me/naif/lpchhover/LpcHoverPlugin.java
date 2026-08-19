package me.naif.lpchhover;

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

public class LpcHoverPlugin extends JavaPlugin implements Listener {

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
        getLogger().info("LPC-Hover enabled!");
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();

        String message = event.getMessage();

        String name = player.getName();

        TextComponent component = new TextComponent(
                ChatColor.WHITE + name
        );

        component.setHoverEvent(new HoverEvent(
                HoverEvent.Action.SHOW_TEXT,
                new ComponentBuilder(
                        ChatColor.YELLOW + "Player: " + ChatColor.WHITE + name +
                        "\n" +
                        ChatColor.YELLOW + "Rank: " + ChatColor.WHITE + "Player"
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
}

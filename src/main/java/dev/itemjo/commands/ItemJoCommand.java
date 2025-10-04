package dev.itemjo.commands;

import dev.itemjo.ItemJoPlugin;
import dev.itemjo.items.ItemId;
import dev.itemjo.items.ItemRegistry;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class ItemJoCommand implements CommandExecutor, TabCompleter {
    private final ItemJoPlugin plugin;

    public ItemJoCommand(ItemJoPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(ChatColor.YELLOW + "/" + label + " <reload|give>");
            return true;
        }
        String sub = args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "reload":
                if (!sender.hasPermission("itemjo.reload")) {
                    sender.sendMessage(ChatColor.RED + "Нет прав.");
                    return true;
                }
                plugin.reload();
                sender.sendMessage(ChatColor.GREEN + "Конфиг перезагружен.");
                return true;
            case "give":
                if (!sender.hasPermission("itemjo.give")) {
                    sender.sendMessage(ChatColor.RED + "Нет прав.");
                    return true;
                }
                if (args.length < 3) {
                    sender.sendMessage(ChatColor.YELLOW + "Использование: /" + label + " give <ник> <id_предмета> [кол-во]");
                    return true;
                }
                Player target = Bukkit.getPlayerExact(args[1]);
                if (target == null) {
                    sender.sendMessage(ChatColor.RED + "Игрок не найден.");
                    return true;
                }
                ItemId id = ItemId.fromString(args[2]);
                if (id == null) {
                    sender.sendMessage(ChatColor.RED + "Неизвестный предмет: " + args[2]);
                    return true;
                }
                int amount = 1;
                if (args.length >= 4) {
                    try { amount = Math.max(1, Integer.parseInt(args[3])); } catch (NumberFormatException ignored) {}
                }
                ItemStack stack = plugin.getItemRegistry().create(id, amount);
                if (stack == null) {
                    sender.sendMessage(ChatColor.RED + "Не удалось создать предмет.");
                    return true;
                }
                target.getInventory().addItem(stack);
                sender.sendMessage(ChatColor.GREEN + "Выдано: " + id.name() + " x" + amount + " игроку " + target.getName());
                return true;
            default:
                sender.sendMessage(ChatColor.YELLOW + "/" + label + " <reload|give>");
                return true;
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return Arrays.asList("reload", "give");
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("give")) {
            List<String> names = new ArrayList<>();
            for (Player p : Bukkit.getOnlinePlayers()) names.add(p.getName());
            return names;
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("give")) {
            List<String> ids = new ArrayList<>();
            for (ItemId id : ItemId.values()) ids.add(id.name());
            return ids;
        }
        return Collections.emptyList();
    }
}

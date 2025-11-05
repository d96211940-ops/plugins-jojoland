package dev.itemjo;

import dev.itemjo.commands.ItemJoCommand;
import dev.itemjo.items.ItemRegistry;
import dev.itemjo.listeners.InteractListener;
import dev.itemjo.listeners.ProjectileListener;
import dev.itemjo.util.CooldownService;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public final class ItemJoPlugin extends JavaPlugin {
    private static ItemJoPlugin instance;

    private Logger fileLogger;
    private CooldownService cooldownService;
    private ItemRegistry itemRegistry;

    public static ItemJoPlugin getInstance() {
        return instance;
    }

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        setupFileLogger();

        this.cooldownService = new CooldownService(this);
        this.itemRegistry = new ItemRegistry(this);

        getServer().getPluginManager().registerEvents(new ProjectileListener(this), this);
        getServer().getPluginManager().registerEvents(new InteractListener(this), this);

        ItemJoCommand cmd = new ItemJoCommand(this);
        if (getCommand("itemjo") != null) {
            getCommand("itemjo").setExecutor(cmd);
            getCommand("itemjo").setTabCompleter(cmd);
        }

        getLogger().info("ItemJo enabled");
    }

    @Override
    public void onDisable() {
        getLogger().info("ItemJo disabled");
    }

    public void reload() {
        reloadConfig();
        cooldownService.reload();
        itemRegistry.reload();
    }

    private void setupFileLogger() {
        File dataFolder = getDataFolder();
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
        File logsDir = new File(dataFolder, "logs");
        if (!logsDir.exists()) {
            logsDir.mkdirs();
        }
        try {
            FileHandler handler = new FileHandler(new File(logsDir, "itemjo.%g.log").getAbsolutePath(), 1024 * 1024, 5, true);
            handler.setFormatter(new SimpleFormatter());
            fileLogger = Logger.getLogger("ItemJoFileLogger");
            fileLogger.addHandler(handler);
        } catch (IOException e) {
            getLogger().warning("Failed to initialize file logger: " + e.getMessage());
        }
    }

    public Logger getFileLogger() {
        return fileLogger != null ? fileLogger : getLogger();
    }

    public CooldownService getCooldownService() {
        return cooldownService;
    }

    public ItemRegistry getItemRegistry() {
        return itemRegistry;
    }
}

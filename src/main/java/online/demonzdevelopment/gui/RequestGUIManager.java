package online.demonzdevelopment.gui;

import online.demonzdevelopment.DZEconomy;
import online.demonzdevelopment.data.CurrencyRequest;
import online.demonzdevelopment.rank.Rank;
import online.demonzdevelopment.util.ColorUtil;
import online.demonzdevelopment.util.NumberFormatter;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages currency request GUIs
 */
public class RequestGUIManager implements Listener {
    
    private final DZEconomy plugin;
    private final Map<UUID, Inventory> openGUIs;
    private final Map<UUID, BukkitTask> updateTasks;
    
    public RequestGUIManager(DZEconomy plugin) {
        this.plugin = plugin;
        this.openGUIs = new ConcurrentHashMap<>();
        this.updateTasks = new ConcurrentHashMap<>();
        
        // Register event listener
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }
    
    /**
     * Open request GUI for a player
     */
    public void openRequestGUI(Player player, CurrencyRequest request) {
        // Check if GUI is enabled
        if (!plugin.getConfigManager().getConfig().getBoolean("gui.request.enabled", true)) {
            return;
        }
        
        // Check if player has inventory open (respect existing GUIs)
        if (plugin.getConfigManager().getConfig().getBoolean("gui.request.respect-open-inventories", true)) {
            InventoryView openInventory = player.getOpenInventory();
            if (openInventory != null && openInventory.getType() != InventoryType.CRAFTING) {
                // Player has another inventory open, don't force-close it
                return;
            }
        }
        
        // Create GUI
        String currencyName = plugin.getCurrencyManager().getCurrencyDisplayName(request.getCurrency());
        String requesterName = Bukkit.getOfflinePlayer(request.getRequesterUUID()).getName();
        String title = ColorUtil.translate(plugin.getConfigManager().getMessages()
                .getString("gui.request.title", "&e{currency} Request from {player}")
                .replace("{currency}", ColorUtil.strip(currencyName))
                .replace("{player}", requesterName));
        
        Inventory gui = Bukkit.createInventory(null, 27, title);
        
        // Build GUI contents
        updateGUIContents(gui, player, request);
        
        // Open GUI
        player.openInventory(gui);
        openGUIs.put(player.getUniqueId(), gui);
        
        // Play sound
        String soundName = plugin.getConfigManager().getConfig().getString("gui.request.sounds.open", "BLOCK_NOTE_BLOCK_PLING");
        try {
            player.playSound(player.getLocation(), Sound.valueOf(soundName), 1.0f, 1.0f);
        } catch (IllegalArgumentException ignored) {}
        
        // Start update task
        startUpdateTask(player, request);
    }
    
    /**
     * Update GUI contents
     */
    private void updateGUIContents(Inventory gui, Player player, CurrencyRequest request) {
        gui.clear();
        
        String symbol = plugin.getCurrencyManager().getCurrencySymbol(request.getCurrency());
        double amount = request.getAmount();
        
        // Calculate tax
        Rank playerRank = plugin.getRankManager().getPlayerRank(player.getUniqueId());
        Rank.RankCurrencySettings settings = playerRank.getSettingsFor(request.getCurrency());
        double taxPercent = settings.getTransferTax();
        double tax = NumberFormatter.truncateDecimal(amount * (taxPercent / 100.0));
        double total = NumberFormatter.truncateDecimal(amount + tax);
        
        // Calculate remaining time
        int timeoutSeconds = plugin.getConfigManager().getConfig().getInt("limits.request-timeout", 120);
        long remaining = request.getRemainingTime(timeoutSeconds);
        
        // Info item (slot 4)
        ItemStack infoItem = new ItemStack(Material.PAPER);
        ItemMeta infoMeta = infoItem.getItemMeta();
        infoMeta.setDisplayName(ColorUtil.translate("&e&lRequest Information"));
        
        List<String> lore = new ArrayList<>();
        List<String> configLore = plugin.getConfigManager().getMessages().getStringList("gui.request.info-lore");
        
        String requesterName = Bukkit.getOfflinePlayer(request.getRequesterUUID()).getName();
        
        for (String line : configLore) {
            lore.add(ColorUtil.translate(line
                    .replace("{player}", requesterName)
                    .replace("{symbol}", symbol)
                    .replace("{amount}", NumberFormatter.formatShort(amount))
                    .replace("{total}", NumberFormatter.formatShort(total))
                    .replace("{tax}", NumberFormatter.formatShort(tax))
                    .replace("{tax_percent}", String.valueOf((int) taxPercent))
                    .replace("{time}", String.valueOf(remaining))));
        }
        
        infoMeta.setLore(lore);
        infoItem.setItemMeta(infoMeta);
        gui.setItem(4, infoItem);
        
        // Accept button (slot 20)
        ItemStack acceptItem = new ItemStack(Material.GREEN_WOOL);
        ItemMeta acceptMeta = acceptItem.getItemMeta();
        String acceptName = plugin.getConfigManager().getMessages().getString("gui.request.accept-name", "&a&lACCEPT");
        acceptMeta.setDisplayName(ColorUtil.translate(acceptName));
        acceptItem.setItemMeta(acceptMeta);
        gui.setItem(20, acceptItem);
        
        // Deny button (slot 24)
        ItemStack denyItem = new ItemStack(Material.RED_WOOL);
        ItemMeta denyMeta = denyItem.getItemMeta();
        String denyName = plugin.getConfigManager().getMessages().getString("gui.request.deny-name", "&c&lDENY");
        denyMeta.setDisplayName(ColorUtil.translate(denyName));
        denyItem.setItemMeta(denyMeta);
        gui.setItem(24, denyItem);
        
        // Fill empty slots with glass pane
        ItemStack filler = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta fillerMeta = filler.getItemMeta();
        fillerMeta.setDisplayName(" ");
        filler.setItemMeta(fillerMeta);
        
        for (int i = 0; i < gui.getSize(); i++) {
            if (gui.getItem(i) == null) {
                gui.setItem(i, filler);
            }
        }
    }
    
    /**
     * Start GUI update task
     */
    private void startUpdateTask(Player player, CurrencyRequest request) {
        int updateInterval = plugin.getConfigManager().getConfig().getInt("gui.request.update-interval", 20);
        
        BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            Inventory gui = openGUIs.get(player.getUniqueId());
            if (gui == null) {
                stopUpdateTask(player);
                return;
            }
            
            // Check if request expired
            int timeoutSeconds = plugin.getConfigManager().getConfig().getInt("limits.request-timeout", 120);
            if (request.isExpired(timeoutSeconds)) {
                closeGUI(player);
                player.sendMessage(ColorUtil.translate(plugin.getConfigManager().getMessages()
                        .getString("gui.timeout-message", "&cRequest expired!")));
                return;
            }
            
            // Update GUI
            updateGUIContents(gui, player, request);
        }, updateInterval, updateInterval);
        
        updateTasks.put(player.getUniqueId(), task);
    }
    
    /**
     * Stop GUI update task
     */
    private void stopUpdateTask(Player player) {
        BukkitTask task = updateTasks.remove(player.getUniqueId());
        if (task != null) {
            task.cancel();
        }
    }
    
    /**
     * Close GUI for a player
     */
    public void closeGUI(Player player) {
        stopUpdateTask(player);
        openGUIs.remove(player.getUniqueId());
        player.closeInventory();
    }
    
    /**
     * Close all open GUIs
     */
    public void closeAllGUIs() {
        for (UUID uuid : new HashSet<>(openGUIs.keySet())) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                closeGUI(player);
            }
        }
    }
    
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        
        Player player = (Player) event.getWhoClicked();
        Inventory clickedInventory = event.getClickedInventory();
        
        if (clickedInventory == null) return;
        
        // Check if it's a request GUI
        if (!openGUIs.containsKey(player.getUniqueId())) return;
        if (!clickedInventory.equals(openGUIs.get(player.getUniqueId()))) return;
        
        event.setCancelled(true);
        
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;
        
        // Get request
        CurrencyRequest request = plugin.getCurrencyManager().getPendingRequest(player.getUniqueId());
        if (request == null) {
            closeGUI(player);
            return;
        }
        
        // Handle clicks
        int slot = event.getSlot();
        
        if (slot == 20) {
            // Accept button
            closeGUI(player);
            player.performCommand(request.getCurrency().getId() + " accept");
        } else if (slot == 24) {
            // Deny button
            closeGUI(player);
            player.performCommand(request.getCurrency().getId() + " deny");
        }
    }
    
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) return;
        
        Player player = (Player) event.getPlayer();
        
        if (openGUIs.containsKey(player.getUniqueId())) {
            stopUpdateTask(player);
            openGUIs.remove(player.getUniqueId());
        }
    }
}
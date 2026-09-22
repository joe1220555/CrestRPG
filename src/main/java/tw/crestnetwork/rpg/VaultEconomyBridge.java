package tw.crestnetwork.rpg;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

public final class VaultEconomyBridge {
    private static Object economyProvider;
    private static boolean checked = false;

    private VaultEconomyBridge() {}

    public static synchronized boolean isAvailable() {
        if (!checked) {
            checked = true;
            if (Bukkit.getPluginManager().getPlugin("Vault") != null) {
                try {
                    RegisteredServiceProvider<?> rsp = Bukkit.getServicesManager().getRegistration(net.milkbowl.vault.economy.Economy.class);
                    if (rsp != null) {
                        economyProvider = rsp.getProvider();
                    }
                } catch (Throwable ignored) {
                    economyProvider = null;
                }
            }
        }
        return economyProvider != null;
    }

    public static void deposit(Player player, double amount) {
        if (isAvailable() && economyProvider instanceof net.milkbowl.vault.economy.Economy econ) {
            econ.depositPlayer(player, amount);
        }
    }

    public static boolean withdraw(Player player, double amount) {
        if (amount < 0 || !isAvailable() || !(economyProvider instanceof net.milkbowl.vault.economy.Economy econ)) return false;
        return econ.withdrawPlayer(player, amount).transactionSuccess();
    }
}

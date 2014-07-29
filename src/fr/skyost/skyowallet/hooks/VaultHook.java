package fr.skyost.skyowallet.hooks;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.milkbowl.vault.economy.AbstractEconomy;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import net.milkbowl.vault.economy.EconomyResponse.ResponseType;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.ServicePriority;

import com.google.common.base.Charsets;

import fr.skyost.skyowallet.Skyowallet;
import fr.skyost.skyowallet.SkyowalletAPI;
import fr.skyost.skyowallet.SkyowalletAPI.SkyowalletAccount;
import fr.skyost.skyowallet.SkyowalletAPI.SkyowalletBank;
import fr.skyost.skyowallet.utils.Utils;

public class VaultHook extends AbstractEconomy implements Listener {
	
	private final Skyowallet skyowallet = SkyowalletAPI.getPlugin();
	
	public VaultHook(final Plugin plugin) {
		Bukkit.getPluginManager().registerEvents(this, plugin);
	}
	
	public static final void addToVault(final Plugin vault) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException {
		final VaultHook hook = new VaultHook(vault);
		Bukkit.getServicesManager().register(Economy.class, hook, vault, ServicePriority.Normal);
		final Logger logger = vault.getLogger();
		final String name = hook.getName();
		logger.log(Level.INFO, "[Economy] " + name + " found: Loaded.");
		logger.log(Level.INFO, "[Economy] " + name + " hooked.");
		/*final Method hookEconomy = vault.getClass().getDeclaredMethod("hookEconomy", String.class, Class.class, ServicePriority.class, String[].class);
		hookEconomy.setAccessible(true);
		hookEconomy.invoke(vault, "Skyowallet", VaultHook.class, ServicePriority.Normal, new String[]{"fr.skyost.skyowallet.Skyowallet"});*/
	}
	
	@Override
	public final String getName() {
		return skyowallet.getName();
	}
	
	@Override
	public final boolean isEnabled() {
		return skyowallet != null;
	}
	
	@Override
	public final boolean hasBankSupport() {
		return true;
	}
	
	@Override
	public final String currencyNamePlural() {
		return SkyowalletAPI.getCurrencyNamePlural();
	}
	
	@Override
	public final String currencyNameSingular() {
		return SkyowalletAPI.getCurrencyNameSingular();
	}
	
	@Override
	public final String format(final double amount) {
		return amount + " " + SkyowalletAPI.getCurrencyName(amount);
	}
	
	@Override
	public final boolean createPlayerAccount(final String playerName) {
		if(hasAccount(playerName)) {
			return true;
		}
		final OfflinePlayer player = Bukkit.getOfflinePlayer(playerName);
		if(player == null) {
			return false;
		}
		SkyowalletAPI.registerAccount(player.getUniqueId());
		return true;
	}
	
	@Override
	public final int fractionalDigits() {
		return 0;
	}
	
	@Override
	public final double getBalance(final String playerName) {
		if(!hasAccount(playerName)) {
			return -1d;
		}
		return getAccountByName(playerName).getWallet();
	}
	
	@Override
	public final EconomyResponse withdrawPlayer(final String playerName, final double amount) {
		double balance = getBalance(playerName);
		if(amount < 0) {
			return new EconomyResponse(amount, balance, ResponseType.FAILURE, "Cannot withdraw negative funds.");
		}
		if(!has(playerName, amount)) {
			return new EconomyResponse(amount, balance, ResponseType.FAILURE, "Insufficient funds");
		}
		if(!hasAccount(playerName)) {
			return new EconomyResponse(amount, balance, ResponseType.FAILURE, "This player does not exist or does not have an account.");
		}
		final SkyowalletAccount account = getAccountByName(playerName);
		balance -= account.getWallet();
		account.setWallet(balance);
		return new EconomyResponse(amount, balance, ResponseType.SUCCESS, "Success.");
	}
	
	@Override
	public final EconomyResponse depositPlayer(final String playerName, final double amount) {
		double balance = getBalance(playerName);
		if(amount < 0) {
			return new EconomyResponse(amount, balance, ResponseType.FAILURE, "Cannot withdraw deposit funds.");
		}
		if(!hasAccount(playerName)) {
			return new EconomyResponse(amount, balance, ResponseType.FAILURE, "This player does not exist or does not have an account.");
		}
		final SkyowalletAccount account = getAccountByName(playerName);
		balance += account.getWallet();
		account.setWallet(balance);
		return new EconomyResponse(amount, balance, ResponseType.SUCCESS, "Success.");
	}
	
	@Override
	public final boolean has(final String playerName, final double amount) {
		return getBalance(playerName) >= amount;
	}
	
	@Override
	public final boolean hasAccount(final String playerName) {
		return getAccountByName(playerName) != null;
	}
	
	@Override
	public final EconomyResponse createBank(final String bankName, final String playerName) {
		if(!Utils.isValidFileName(bankName)) {
			return new EconomyResponse(0.0, 0.0, ResponseType.FAILURE, "This is not a valid bank name.");
		}
		if(SkyowalletAPI.isBankExists(bankName)) {
			return new EconomyResponse(0.0, 0.0, ResponseType.FAILURE, "A bank with the same name already exists.");
		}
		if(!hasAccount(playerName)) {
			return new EconomyResponse(0.0, 0.0, ResponseType.FAILURE, "This player does not exist or does not have an account.");
		}
		final SkyowalletBank bank = SkyowalletAPI.createBank(bankName);
		final SkyowalletAccount account = getAccountByName(playerName);
		account.setBank(bank, false);
		account.setBankOwner(true);
		return new EconomyResponse(0.0, 0.0, ResponseType.SUCCESS, "Success.");
	}
	
	@Override
	public final EconomyResponse deleteBank(final String bankName) {
		if(!SkyowalletAPI.isBankExists(bankName)) {
			return new EconomyResponse(0.0, 0.0, ResponseType.FAILURE, "This bank does not exist.");
		}
		SkyowalletAPI.deleteBank(SkyowalletAPI.getBank(bankName));
		return new EconomyResponse(0.0, 0.0, ResponseType.SUCCESS, "Success.");
	}
	
	@Override
	public final EconomyResponse bankBalance(final String playerName) {
		if(!hasAccount(playerName)) {
			return new EconomyResponse(0.0, 0.0, ResponseType.FAILURE, "This player does not exist or does not have an account.");
		}
		final SkyowalletAccount account = getAccountByName(playerName);
		if(!account.hasBank()) {
			return new EconomyResponse(0.0, 0.0, ResponseType.FAILURE, "This player does not have a bank account.");
		}
		return new EconomyResponse(0.0, account.getBankBalance(), ResponseType.SUCCESS, "Success.");
	}
	
	@Override
	public final EconomyResponse bankHas(final String playerName, final double amount) {
		final EconomyResponse bankBalance = bankBalance(playerName);
		if(bankBalance.type == ResponseType.FAILURE) {
			return bankBalance;
		}
		return bankBalance.balance >= amount ? new EconomyResponse(amount, bankBalance.amount, ResponseType.SUCCESS, "Yes.") : new EconomyResponse(amount, bankBalance.amount, ResponseType.FAILURE, "No.");
	}
	
	@Override
	public final EconomyResponse bankWithdraw(final String playerName, final double amount) {
		final EconomyResponse bankBalance = bankBalance(playerName);
		if(bankBalance.type == ResponseType.FAILURE) {
			return bankBalance;
		}
		if(bankHas(playerName, amount).type == ResponseType.FAILURE) {
			return new EconomyResponse(amount, bankBalance.balance, ResponseType.FAILURE, "This player does not have enough money on his bank account.");
		}
		final SkyowalletAccount account = getAccountByName(playerName);
		final double balance = bankBalance.balance - amount;
		account.setBankBalance(balance);
		return new EconomyResponse(amount, balance, ResponseType.SUCCESS, "Success.");
	}
	
	@Override
	public final EconomyResponse bankDeposit(final String playerName, final double amount) {
		final EconomyResponse bankBalance = bankBalance(playerName);
		if(bankBalance.type == ResponseType.FAILURE) {
			return bankBalance;
		}
		final SkyowalletAccount account = getAccountByName(playerName);
		final double balance = bankBalance.balance + amount;
		account.setBankBalance(balance);
		return new EconomyResponse(amount, balance, ResponseType.SUCCESS, "Success.");
	}
	
	@Override
	public final EconomyResponse isBankOwner(final String bankName, final String playerName) {
		if(!SkyowalletAPI.isBankExists(bankName)) {
			return new EconomyResponse(0.0, 0.0, ResponseType.FAILURE, "This bank does not exist.");
		}
		if(!hasAccount(playerName)) {
			return new EconomyResponse(0.0, 0.0, ResponseType.FAILURE, "This player does not exist or does not have an account.");
		}
		return getAccountByName(playerName).isBankOwner() ? new EconomyResponse(0.0, 0.0, ResponseType.SUCCESS, "Yes.") : new EconomyResponse(0.0, 0.0, ResponseType.FAILURE, "No.");
	}
	
	@Override
	public final EconomyResponse isBankMember(final String bankName, final String playerName) {
		if(!SkyowalletAPI.isBankExists(bankName)) {
			return new EconomyResponse(0.0, 0.0, ResponseType.FAILURE, "This bank does not exist.");
		}
		if(!hasAccount(playerName)) {
			return new EconomyResponse(0.0, 0.0, ResponseType.FAILURE, "This player does not exist or does not have an account.");
		}
		final SkyowalletBank playerBank = getAccountByName(playerName).getBank();
		if(playerBank == null) {
			return new EconomyResponse(0.0, 0.0, ResponseType.FAILURE, "This player does not have a bank account.");
		}
		return playerBank.getName().equals(bankName) ? new EconomyResponse(0.0, 0.0, ResponseType.SUCCESS, "Yes.") : new EconomyResponse(0.0, 0.0, ResponseType.FAILURE, "No.");
	}
	
	@Override
	public final List<String> getBanks() {
		final List<String> banks = new ArrayList<String>();
		for(final SkyowalletBank bank : SkyowalletAPI.getBanks()) {
			banks.add(bank.getName());
		}
		return banks;
	}
	
	@Override
	public final boolean hasAccount(final String playerName, final String worldName) {
		return hasAccount(playerName);
	}
	
	@Override
	public final double getBalance(final String playerName, final String worldName) {
		return getBalance(playerName);
	}
	
	@Override
	public final boolean has(final String playerName, final String worldName, final double amount) {
		return has(playerName, amount);
	}
	
	@Override
	public final EconomyResponse withdrawPlayer(final String playerName, final String worldName, final double amount) {
		return withdrawPlayer(playerName, amount);
	}
	
	@Override
	public final EconomyResponse depositPlayer(final String playerName, final String worldName, final double amount) {
		return depositPlayer(playerName, amount);
	}
	
	@Override
	public final boolean createPlayerAccount(final String playerName, final String worldName) {
		return createPlayerAccount(playerName);
	}
	
	private final SkyowalletAccount getAccountByName(final String playerName) {
		OfflinePlayer player = Bukkit.getOfflinePlayer(playerName);
		if(player == null) {
			player = Bukkit.getOfflinePlayer(UUID.nameUUIDFromBytes(("OfflinePlayer:" + playerName).getBytes(Charsets.UTF_8)));
		}
		if(player == null || !SkyowalletAPI.hasAccount(player)) {
			return null;
		}
		return SkyowalletAPI.getAccount(player);
	}
		
}
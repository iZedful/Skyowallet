package fr.skyost.skyowallet.commands;

import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import fr.skyost.skyowallet.Skyowallet;
import fr.skyost.skyowallet.SkyowalletAPI;
import fr.skyost.skyowallet.SkyowalletAPI.SkyowalletAccount;
import fr.skyost.skyowallet.SkyowalletAPI.SkyowalletBank;

public class BankCommand extends SubCommandsExecutor {
	
	@Override
	public boolean onCommand(final CommandSender sender, final Command command, final String label, String[] args) {
		if(args.length <= 0) {
			if(sender instanceof Player) {
				final SkyowalletAccount account = SkyowalletAPI.getAccount((Player)sender);
				final SkyowalletBank bank = account.getBank();
				if(bank == null) {
					sender.sendMessage(Skyowallet.messages.message21);
					return true;
				}
				final double bankBalance = bank.getMemberBalance(account);
				if(bank.isOwner(account)) {
					sender.sendMessage(Skyowallet.messages.message14.replace("/bank/", bank.getName()));
				}
				else {
					final String owners;
					final StringBuilder builder = new StringBuilder();
					for(final SkyowalletAccount ownerAccount : bank.getOwners()) {
						final OfflinePlayer owner = Bukkit.getOfflinePlayer(UUID.fromString(ownerAccount.getUUID()));
						if(owner == null) {
							continue;
						}
						builder.append(owner.getName() + ", ");
					}
					owners = builder.toString();
					sender.sendMessage(Skyowallet.messages.message15.replace("/bank/", bank.getName()).replace("/owners/", owners.length() == 0 ? "X" : owners.substring(0, owners.length() - 2)));
				}
				sender.sendMessage(Skyowallet.messages.message16.replace("/amount/", String.valueOf(bankBalance)).replace("/currency-name/", SkyowalletAPI.getCurrencyName(bankBalance)));
			}
			else {
				sender.sendMessage(Skyowallet.messages.message2);
			}
			return true;
		}
		return super.onCommand(sender, command, label, args);
	}

}
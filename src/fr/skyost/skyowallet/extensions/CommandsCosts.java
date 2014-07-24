package fr.skyost.skyowallet.extensions;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;

import org.bukkit.ChatColor;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.plugin.Plugin;

import fr.skyost.skyowallet.SkyowalletAPI;
import fr.skyost.skyowallet.SkyowalletAPI.SkyowalletAccount;
import fr.skyost.skyowallet.utils.Skyoconfig;

public class CommandsCosts extends SkyowalletExtension {
	
	private ExtensionConfig config;
	
	public CommandsCosts(final Plugin plugin) throws InvalidConfigurationException {
		super(plugin);
	}
	
	@Override
	public final String getName() {
		return "CommandsCosts";
	}

	@Override
	public final HashMap<String, PermissionDefault> getPermissions() {
		final HashMap<String, PermissionDefault> permissions = new HashMap<String, PermissionDefault>();
		permissions.put("commandscosts.bypass", PermissionDefault.FALSE);
		return permissions;
	}
	
	@Override
	public final Skyoconfig getConfiguration() {
		if(config == null) {
			config = new ExtensionConfig(this.getConfigurationFile());
		}
		return config;
	}
	
	@Override
	public final boolean isEnabled() {
		return config.enable;
	}
	
	@EventHandler
	private final void onPlayerCommandPreprocessEvent(final PlayerCommandPreprocessEvent event) {
		final Double cost = Double.valueOf(config.data.get(event.getMessage().substring(1).split(" ")[0]));
		if(cost != null) {
			final Player player = event.getPlayer();
			if(!player.hasPermission("commandscosts.bypass")) {
				final SkyowalletAccount account = SkyowalletAPI.getAccount(player.getUniqueId().toString());
				final double wallet = account.getWallet() - cost;
				if(wallet < 0.0) {
					player.sendMessage(ChatColor.RED + "You do not have enough money to run that command.\nCost : " + cost);
					event.setCancelled(true);
					return;
				}
				account.setWallet(wallet);
			}
		}
	}
	
	public class ExtensionConfig extends Skyoconfig {
		
		@ConfigOptions(name = "commands-costs.enable")
		public boolean enable = false;
		@ConfigOptions(name = "commands-costs.data")
		public HashMap<String, String> data = new HashMap<String, String>();
		
		private ExtensionConfig(final File file) {
			super(file, Arrays.asList("CommandsCosts Configuration"));
			data.put("pl", "10.0");
		}
		
	}

}

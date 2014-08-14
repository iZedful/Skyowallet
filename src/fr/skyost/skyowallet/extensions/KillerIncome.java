package fr.skyost.skyowallet.extensions;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.bukkit.ChatColor;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.plugin.Plugin;

import fr.skyost.skyowallet.SkyowalletAPI;
import fr.skyost.skyowallet.SkyowalletAPI.SkyowalletAccount;
import fr.skyost.skyowallet.utils.Skyoconfig;
import fr.skyost.skyowallet.utils.Utils;

public class KillerIncome extends SkyowalletExtension {
	
	private ExtensionConfig config;
	
	public KillerIncome(final Plugin plugin) {
		super(plugin);
	}
	
	@Override
	public final String getName() {
		return "KillerIncome";
	}
	
	@Override
	public final Map<String, PermissionDefault> getPermissions() {
		final Map<String, PermissionDefault> permissions = new HashMap<String, PermissionDefault>();
		permissions.put("killerincome.earn", PermissionDefault.TRUE);
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
	public final String getFileName() {
		return "killer-income.yml";
	}
	
	@Override
	public final boolean isEnabled() {
		return config.enable;
	}
	
	@EventHandler
	private final void onEntityDeath(final EntityDeathEvent event) {
		final LivingEntity entity = event.getEntity();
		final EntityType type = entity.getType();
		final String rawAmount = config.data.get(type.name());
		if(rawAmount == null) {
			return;
		}
		final Double amount = Utils.doubleTryParse(rawAmount);
		if(amount == null) {
			return;
		}
		final Player killer = entity.getKiller();
		if(killer == null) {
			return;
		}
		final SkyowalletAccount account = SkyowalletAPI.getAccount(killer);
		account.setWallet(account.getWallet() + amount);
		final String entityName = type.name();
		killer.sendMessage(config.message1.replace("/amount/", String.valueOf(amount)).replace("/currency-name/", SkyowalletAPI.getCurrencyName(amount)).replace("/entity/", entityName.charAt(0) + entityName.substring(1).toLowerCase()));
	}
	
	public class ExtensionConfig extends Skyoconfig {
		
		@ConfigOptions(name = "enable")
		public boolean enable = false;
		@ConfigOptions(name = "data")
		public LinkedHashMap<String, String> data = new LinkedHashMap<String, String>();
		
		@ConfigOptions(name = "messages.1")
		public String message1 = ChatColor.GOLD + "Congracubations ! You have won /amount/ /currency-name/ because you have killed a /entity/.";
		
		private ExtensionConfig(final File file) {
			super(file, Arrays.asList("KillerIncome Configuration"));
			data.put(EntityType.CREEPER.name(), "10.0");
			data.put(EntityType.SPIDER.name(), "10.0");
			data.put(EntityType.ZOMBIE.name(), "12.5");
			data.put(EntityType.SKELETON.name(), "15.0");
			data.put(EntityType.ENDERMAN.name(), "17.5");
			data.put(EntityType.WITCH.name(), "20.0");
			data.put(EntityType.BLAZE.name(), "25.0");
			data.put(EntityType.PLAYER.name(), "30.0");
			data.put(EntityType.WITHER.name(), "50.0");
			data.put(EntityType.ENDER_DRAGON.name(), "100.0");
		}
		
	}

}

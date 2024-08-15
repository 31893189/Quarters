package net.earthmc.quarters.listener;

import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.object.Resident;
import net.earthmc.quarters.api.QuartersMessaging;
import net.earthmc.quarters.api.manager.ConfigManager;
import net.earthmc.quarters.api.manager.QuarterManager;
import net.earthmc.quarters.api.manager.ResidentMetadataManager;
import net.earthmc.quarters.object.entity.Quarter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * This class handles the notifications that may appear when entering a quarter
 */
public class QuarterEntryNotificationListener implements Listener {

    private static final Map<Player, Quarter> QUARTER_PLAYER_IS_IN = new ConcurrentHashMap<>();

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        if (!ConfigManager.areEntryNotificationsAllowed()) return;

        Player player = event.getPlayer();
        Resident resident = TownyAPI.getInstance().getResident(player);
        if (resident == null) return;

        if (!ResidentMetadataManager.getInstance().hasEntryNotifications(resident)) return;

        Location to = event.getTo();

        Quarter quarter = QuarterManager.getInstance().getQuarter(to);

        Quarter previousQuarter = QUARTER_PLAYER_IS_IN.get(player);
        if (quarter != null && (previousQuarter == null || !previousQuarter.equals(quarter))) sendQuarterEntryAlert(quarter, player);

        QUARTER_PLAYER_IS_IN.put(player, quarter);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) { // This listener is to prevent an alert when connecting inside a quarter
        if (!ConfigManager.areEntryNotificationsAllowed()) return;

        Player player = event.getPlayer();
        Resident resident = TownyAPI.getInstance().getResident(player);
        if (resident == null) return;

        if (!ResidentMetadataManager.getInstance().hasEntryNotifications(resident)) return;

        Quarter quarter = QuarterManager.getInstance().getQuarter(player.getLocation());

        QUARTER_PLAYER_IS_IN.put(player, quarter);
    }

    private void sendQuarterEntryAlert(Quarter quarter, Player player) {
        TextComponent.Builder builder = Component.text();
        builder.append(Component.text("You have entered a quarter named ", NamedTextColor.GRAY));
        builder.append(Component.text(quarter.getName(), NamedTextColor.GRAY, TextDecoration.ITALIC).clickEvent(ClickEvent.runCommand("/quarters:q here " + quarter.getUUID())));
        if (quarter.hasOwner()) builder.append(Component.text(" owned by ", NamedTextColor.GRAY).append(
                ConfigManager.getFormattedName(quarter.getOwner(), Component.text(quarter.getTown().getName()))) // Town name is better than some error if it doesn't work
        );

        QuartersMessaging.sendMessage(player, builder.build());
    }
}

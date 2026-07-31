package com.velocityreport.listener;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PostLoginEvent;
import com.velocityreport.ReportManager;

/**
 * Listens for player events.
 * <p>
 * When a staff member joins, notifies them of pending open reports.
 */
public class PlayerListener {

    private final ReportManager manager;

    public PlayerListener(ReportManager manager) {
        this.manager = manager;
    }

    @Subscribe
    public void onPostLogin(PostLoginEvent event) {
        // PostLoginEvent fires after the player has fully joined the proxy,
        // so we can safely send them a message directly.
        manager.notifyStaffOnJoin(event.getPlayer());
    }
}

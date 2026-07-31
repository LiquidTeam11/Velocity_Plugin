package com.velocityreport.command;

import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.command.CommandSource;
import com.velocityreport.VelocityReport;

import net.kyori.adventure.text.minimessage.MiniMessage;

/**
 * /reportreload
 * <p>
 * Reloads config.yml without restarting the proxy.
 * Permission: {@code velocityreport.staff}.
 */
public class ReportReloadCommand implements SimpleCommand {

    private final VelocityReport plugin;

    public ReportReloadCommand(VelocityReport plugin) {
        this.plugin = plugin;
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();

        try {
            plugin.reloadConfig();
            source.sendMessage(MiniMessage.miniMessage()
                    .deserialize(plugin.getMessage("command-reload-success")));
        } catch (Exception e) {
            source.sendMessage(MiniMessage.miniMessage()
                    .deserialize(plugin.getMessage("command-reload-fail")));
        }
    }

    @Override
    public boolean hasPermission(Invocation invocation) {
        return invocation.source().hasPermission("velocityreport.staff");
    }
}

package com.oneshot.modules;
import net.runelite.client.plugins.Plugin;
import javax.inject.Singleton;

@Singleton
public class ModTools extends Plugin {

    private ModToolsPanel modToolsPanel;

    public void init(ModToolsPanel modToolsPanel)
    {
        this.modToolsPanel = modToolsPanel;
    }
}

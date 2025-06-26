package com.mingun.privatechest;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public class PrivateChest extends JavaPlugin {

    @Override
    public void onEnable() {
        // 각 이벤트 리스너 클래스를 인스턴스화하고 등록합니다.
        Bukkit.getPluginManager().registerEvents(new SignEngraverListener(this), this);
        Bukkit.getPluginManager().registerEvents(new ChestProtectorListener(this), this);
        getLogger().info("PrivateChest 플러그인이 활성화되었습니다!");
    }

    @Override
    public void onDisable() {
        getLogger().info("PrivateChest 플러그인이 비활성화되었습니다.");
    }
}

package com.signtp;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

public class SignTpConfig {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    public boolean allowTpSigns = true;
    public boolean allowPresetSigns = true;

    public RawCommands rawCommands = new RawCommands();

    /** Preset name -> full command, for example "spawn": "tp @p 0 64 0". */
    public Map<String, String> presets = new LinkedHashMap<>();

    public static class RawCommands {
        /** When true, [cmd] signs and lines starting with / are executed. */
        public boolean enabled = true;
        /** When true, only operators can use [cmd] signs. */
        public boolean requireOp = true;
    }

    private transient Path path;

    public static SignTpConfig load() {
        Path path = FabricLoader.getInstance().getConfigDir().resolve("signtp.json");
        SignTpConfig config = new SignTpConfig();
        config.path = path;

        if (Files.exists(path)) {
            try {
                String json = Files.readString(path, StandardCharsets.UTF_8);
                SignTpConfig loaded = GSON.fromJson(json, SignTpConfig.class);
                if (loaded != null) {
                    config = loaded;
                    config.path = path;
                }
            } catch (Exception e) {
                SignTpMod.LOGGER.error("[SignTP] Failed to read config {}, using defaults", path, e);
            }
        }

        if (config.presets == null) {
            config.presets = new LinkedHashMap<>();
        }
        if (config.rawCommands == null) {
            config.rawCommands = new RawCommands();
        }
        if (config.presets.isEmpty()) {
            // Sensible default preset: teleports the clicking player to world spawn.
            config.presets.put("spawn", "tp @p 0 64 0");
        }

        config.save();
        return config;
    }

    public void save() {
        if (path == null) {
            path = FabricLoader.getInstance().getConfigDir().resolve("signtp.json");
        }
        try {
            Path parent = path.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.writeString(path, GSON.toJson(this), StandardCharsets.UTF_8);
        } catch (IOException e) {
            SignTpMod.LOGGER.error("[SignTP] Failed to write config {}", path, e);
        }
    }
}

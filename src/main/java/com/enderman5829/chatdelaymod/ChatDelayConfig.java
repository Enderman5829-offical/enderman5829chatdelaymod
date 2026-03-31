package com.enderman5829.chatdelaymod;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

public final class ChatDelayConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("chatdelaymod.json");

    private double delaySeconds = 3.0;
    private boolean blockImmediateDuplicate = true;
    private String warningColor = "red";

    public static ChatDelayConfig load() {
        if (!Files.exists(CONFIG_PATH)) {
            ChatDelayConfig cfg = new ChatDelayConfig();
            cfg.save();
            return cfg;
        }

        try (Reader reader = Files.newBufferedReader(CONFIG_PATH)) {
            ChatDelayConfig cfg = GSON.fromJson(reader, ChatDelayConfig.class);
            if (cfg == null) cfg = new ChatDelayConfig();
            cfg.delaySeconds = Math.max(0.0, cfg.delaySeconds);
            String normalizedColor = ChatDelayModClient.normalizeColorName(cfg.warningColor);
            cfg.warningColor = normalizedColor == null ? "red" : normalizedColor;
            cfg.save();
            return cfg;
        } catch (IOException | JsonParseException e) {
            ChatDelayModClient.LOGGER.warn("Failed to read chatdelay config, using defaults.", e);
            ChatDelayConfig cfg = new ChatDelayConfig();
            cfg.save();
            return cfg;
        }
    }

    public void save() {
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            try (Writer writer = Files.newBufferedWriter(CONFIG_PATH)) {
                GSON.toJson(this, writer);
            }
        } catch (IOException e) {
            ChatDelayModClient.LOGGER.error("Failed to write chatdelay config.", e);
        }
    }

    public double getDelaySeconds() { return delaySeconds; }
    public void setDelaySeconds(double delaySeconds) { this.delaySeconds = Math.max(0.0, delaySeconds); }
    public boolean isBlockImmediateDuplicate() { return blockImmediateDuplicate; }
    public void setBlockImmediateDuplicate(boolean blockImmediateDuplicate) { this.blockImmediateDuplicate = blockImmediateDuplicate; }
    public String getWarningColor() { return warningColor; }
    public void setWarningColor(String warningColor) { this.warningColor = ChatDelayModClient.normalizeColorName(warningColor); }
}

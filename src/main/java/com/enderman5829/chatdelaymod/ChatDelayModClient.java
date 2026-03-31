package com.enderman5829.chatdelaymod;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Locale;
import java.util.stream.Collectors;

public final class ChatDelayModClient implements ClientModInitializer {
    public static final String MOD_ID = "chatdelaymod";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    private static final String WARNING_TEXT = "Remember To Refrain from Spamming.";
    private static final String MOD_PREFIX = "◆ ChatDelayMod ◆ ";

    private ChatDelayConfig config;
    private long lastSentAtMillis;
    private String lastAcceptedMessage;

    @Override
    public void onInitializeClient() {
        this.config = ChatDelayConfig.load();
        ClientSendMessageEvents.ALLOW_CHAT.register(this::allowChatMessage);
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
            dispatcher.register(ClientCommandManager.literal("chatdelay").executes(context -> {
                context.getSource().sendFeedback(Text.literal(buildStatusMessage()));
                return 1;
            }))
        );
    }

    private boolean allowChatMessage(String message) {
        long now = System.currentTimeMillis();
        String trimmed = message.trim();
        if (config.isBlockImmediateDuplicate() && lastAcceptedMessage != null && lastAcceptedMessage.equals(trimmed)) {
            sendWarning();
            return false;
        }
        long minGapMillis = Math.round(config.getDelaySeconds() * 1000.0);
        if (minGapMillis > 0 && lastSentAtMillis > 0 && now - lastSentAtMillis < minGapMillis) {
            sendWarning();
            return false;
        }
        lastSentAtMillis = now;
        lastAcceptedMessage = trimmed;
        return true;
    }

    private void sendWarning() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null) {
            Text warningText = buildWarningText();
            client.player.sendMessage(warningText, false);
            client.player.sendMessage(warningText.copy(), true);
            client.player.playSound(SoundEvents.BLOCK_NOTE_BLOCK_BELL.value(), 0.9F, 1.1F);
        }
    }

    private Text buildWarningText() {
        MutableText prefix = Text.literal(MOD_PREFIX).formatted(Formatting.GOLD, Formatting.BOLD);
        MutableText warning = Text.literal(WARNING_TEXT).formatted(getConfiguredColor(), Formatting.BOLD);
        return prefix.append(warning);
    }

    private Formatting getConfiguredColor() {
        Formatting configured = parseColor(config.getWarningColor());
        return configured == null ? Formatting.RED : configured;
    }

    public static String normalizeColorName(String colorName) {
        Formatting formatting = parseColor(colorName);
        return formatting == null ? Formatting.RED.getName() : formatting.getName();
    }

    private static Formatting parseColor(String value) {
        if (value == null) return null;
        Formatting formatting = Formatting.byName(value.toLowerCase(Locale.ROOT));
        return (formatting != null && formatting.isColor()) ? formatting : null;
    }

    private static String getAllowedColors() {
        return Arrays.stream(Formatting.values()).filter(Formatting::isColor).map(Formatting::getName).sorted().collect(Collectors.joining(", "));
    }

    private String buildStatusMessage() {
        return "Chat delay: " + config.getDelaySeconds() + "s, duplicate blocking: " + (config.isBlockImmediateDuplicate() ? "on" : "off") + ", warning color: " + config.getWarningColor() + ". Allowed: " + getAllowedColors();
    }
}

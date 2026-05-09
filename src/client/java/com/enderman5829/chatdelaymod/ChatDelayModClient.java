package com.enderman5829.chatdelaymod;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
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
            dispatcher.register(ClientCommands.literal("chatdelay").executes(context -> {
                context.getSource().sendFeedback(Component.literal(buildStatusMessage()));
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
        Minecraft client = Minecraft.getInstance();
        if (client.player != null) {
            Component warningText = buildWarningText();
            client.player.sendSystemMessage(warningText);
            client.player.sendOverlayMessage(warningText.copy());
            client.player.playSound(SoundEvents.NOTE_BLOCK_BELL.value(), 0.9F, 1.1F);
        }
    }

    private Component buildWarningText() {
        MutableComponent prefix = Component.literal(MOD_PREFIX).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD);
        MutableComponent warning = Component.literal(WARNING_TEXT).withStyle(getConfiguredColor(), ChatFormatting.BOLD);
        return prefix.append(warning);
    }

    private ChatFormatting getConfiguredColor() {
        ChatFormatting configured = parseColor(config.getWarningColor());
        return configured == null ? ChatFormatting.RED : configured;
    }

    public static String normalizeColorName(String colorName) {
        ChatFormatting formatting = parseColor(colorName);
        return formatting == null ? ChatFormatting.RED.getName() : formatting.getName();
    }

    private static ChatFormatting parseColor(String value) {
        if (value == null) return null;
        ChatFormatting formatting = ChatFormatting.getByName(value.toLowerCase(Locale.ROOT));
        return (formatting != null && formatting.isColor()) ? formatting : null;
    }

    private static String getAllowedColors() {
        return Arrays.stream(ChatFormatting.values()).filter(ChatFormatting::isColor).map(ChatFormatting::getName).sorted().collect(Collectors.joining(", "));
    }

    private String buildStatusMessage() {
        return "Chat delay: " + config.getDelaySeconds() + "s, duplicate blocking: " + (config.isBlockImmediateDuplicate() ? "on" : "off") + ", warning color: " + config.getWarningColor() + ". Allowed: " + getAllowedColors();
    }
}

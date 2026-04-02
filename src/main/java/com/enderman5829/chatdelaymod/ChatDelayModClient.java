package com.enderman5829.chatdelaymod;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
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
        LOGGER.info("ChatDelayMod initialized. Config: delay={}, duplicate-block={}, color={}", config.getDelaySeconds(), config.isBlockImmediateDuplicate(), config.getWarningColor());
        
        // Register custom command
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> registerChatDelayCommand(dispatcher));
        
        // Register chat message handler
        ClientSendMessageEvents.ALLOW_CHAT.register(this::onSendChatMessage);
    }
    
    private boolean onSendChatMessage(String messageText) {
        return allowChatMessage(messageText);
    }

    private void registerChatDelayCommand(com.mojang.brigadier.CommandDispatcher<FabricClientCommandSource> dispatcher) {
        dispatcher.register(ClientCommandManager.literal("chatdelay")
            .executes(context -> sendStatus(context))
            .then(ClientCommandManager.literal("delay")
                .then(ClientCommandManager.argument("seconds", DoubleArgumentType.doubleArg(0.0))
                    .executes(context -> setDelay(context, DoubleArgumentType.getDouble(context, "seconds"))))
            )
            .then(ClientCommandManager.literal("duplicate")
                .then(ClientCommandManager.literal("on").executes(context -> setDuplicate(context, true)))
                .then(ClientCommandManager.literal("off").executes(context -> setDuplicate(context, false)))
            )
            .then(ClientCommandManager.literal("color")
                .then(ClientCommandManager.argument("color", StringArgumentType.word())
                    .executes(context -> setColor(context, StringArgumentType.getString(context, "color"))))
            )
        );
    }

    private int sendStatus(com.mojang.brigadier.context.CommandContext<FabricClientCommandSource> context) {
        context.getSource().sendFeedback(Text.literal(buildStatusMessage()));
        return 1;
    }

    private int setDelay(com.mojang.brigadier.context.CommandContext<FabricClientCommandSource> context, double delaySeconds) {
        config.setDelaySeconds(delaySeconds);
        config.save();
        context.getSource().sendFeedback(Text.literal("Chat delay set to " + config.getDelaySeconds() + "s."));
        return 1;
    }

    private int setDuplicate(com.mojang.brigadier.context.CommandContext<FabricClientCommandSource> context, boolean block) {
        config.setBlockImmediateDuplicate(block);
        config.save();
        context.getSource().sendFeedback(Text.literal("Immediate duplicate blocking " + (block ? "enabled" : "disabled") + "."));
        return 1;
    }

    private int setColor(com.mojang.brigadier.context.CommandContext<FabricClientCommandSource> context, String colorName) {
        String normalized = normalizeColorName(colorName);
        if (normalized == null) {
            context.getSource().sendFeedback(Text.literal("Unknown color: " + colorName + ". Use one of: " + getAllowedColors()));
            return 0;
        }
        config.setWarningColor(normalized);
        config.save();
        context.getSource().sendFeedback(Text.literal("Warning color set to " + config.getWarningColor() + "."));
        return 1;
    }

    public boolean allowChatMessage(String message) {
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
        return formatting == null ? null : formatting.getName();
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

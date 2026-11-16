package com.signtp;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.PermissionSet;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SignTpMod implements ModInitializer {

    public static final Logger LOGGER = LoggerFactory.getLogger("signtp");

    private SignTpConfig config;

    @Override
    public void onInitialize() {
        config = SignTpConfig.load();
        UseBlockCallback.EVENT.register(this::onUseBlock);
        PlayerBlockBreakEvents.BEFORE.register(this::onBlockBreak);
        LOGGER.info("[SignTP] Ready. Right-click a sign whose first line is [tp], [preset] or [cmd].");
    }

    private boolean onBlockBreak(Level level, Player player, BlockPos pos, BlockState state, BlockEntity blockEntity) {
        if (blockEntity instanceof SignBlockEntity sign) {
            if (isProtectedSign(sign)) {
                if (player instanceof ServerPlayer serverPlayer && isOp(level, serverPlayer)) {
                    return true;
                }
                if (player instanceof ServerPlayer serverPlayer) {
                    send(serverPlayer, "\u00a7c[SignTP] 命令告示牌仅管理员可破坏");
                }
                return false;
            }
        }
        return true;
    }

    private InteractionResult onUseBlock(Player player, Level level, InteractionHand hand, BlockHitResult hitResult) {
        if (level.isClientSide()) {
            return InteractionResult.PASS;
        }
        if (player.isShiftKeyDown()) {
            if (player instanceof ServerPlayer serverPlayer) {
                BlockPos pos = hitResult.getBlockPos();
                if (level.getBlockEntity(pos) instanceof SignBlockEntity sign && isProtectedSign(sign) && isOp(level, serverPlayer) == false) {
                    send(serverPlayer, "\u00a7c[SignTP] 命令告示牌仅管理员可编辑");
                    return InteractionResult.SUCCESS;
                }
            }
            return InteractionResult.PASS;
        }
        if (player instanceof ServerPlayer serverPlayer) {
            BlockPos pos = hitResult.getBlockPos();
            if (level.getBlockEntity(pos) instanceof SignBlockEntity sign) {
                SignText text = sign.isFacingFrontText(player) ? sign.getFrontText() : sign.getBackText();
                String[] lines = readLines(text);

                if (handleTpSign(serverPlayer, lines)) {
                    return InteractionResult.SUCCESS;
                }
                if (handlePresetSign(serverPlayer, lines)) {
                    return InteractionResult.SUCCESS;
                }
                if (handleRawCommandSign(serverPlayer, level, lines)) {
                    return InteractionResult.SUCCESS;
                }
                if (handleSlashSign(serverPlayer, level, lines)) {
                    return InteractionResult.SUCCESS;
                }
            }
        }

        return InteractionResult.PASS;
    }

    private boolean handleTpSign(ServerPlayer player, String[] lines) {
        if (config.allowTpSigns == false) {
            return false;
        }
        if (lines[0].equalsIgnoreCase("[tp]") == false) {
            return false;
        }

        String args = join(lines, 1, 3);
        if (args.isEmpty()) {
            send(player, "\u00a7c[SignTP] Put /tp arguments on lines 2-4, e.g. 0 64 0");
            return true;
        }

        String preset = config.presets.get(args);
        if (preset != null && preset.isBlank() == false) {
            execute(player, preset);
        } else {
            execute(player, "tp " + args);
        }
        return true;
    }

    private boolean handlePresetSign(ServerPlayer player, String[] lines) {
        if (config.allowPresetSigns == false) {
            return false;
        }
        if (lines[0].equalsIgnoreCase("[preset]") == false) {
            return false;
        }

        String key = lines[1].trim();
        if (key.isEmpty()) {
            send(player, "\u00a7c[SignTP] Put the preset name on line 2");
            return true;
        }

        String command = config.presets.get(key);
        if (command == null || command.isBlank()) {
            send(player, "\u00a7c[SignTP] Unknown preset: " + key);
            return true;
        }

        execute(player, command);
        return true;
    }

    private boolean handleRawCommandSign(ServerPlayer player, Level level, String[] lines) {
        if (config.rawCommands.enabled == false) {
            return false;
        }
        if (lines[0].equalsIgnoreCase("[cmd]") == false && lines[0].equalsIgnoreCase("[command]") == false) {
            return false;
        }

        if (config.rawCommands.requireOp && isOp(level, player) == false) {
            send(player, "\u00a7c[SignTP] [cmd] signs are for operators only");
            return true;
        }

        String command = join(lines, 1, 3);
        if (command.isEmpty()) {
            send(player, "\u00a7c[SignTP] Put the command on lines 2-4");
            return true;
        }

        execute(player, command);
        return true;
    }

    private boolean handleSlashSign(ServerPlayer player, Level level, String[] lines) {
        if (config.rawCommands.enabled == false) {
            return false;
        }
        if (lines[0].startsWith("/") == false) {
            return false;
        }

        if (config.rawCommands.requireOp && isOp(level, player) == false) {
            send(player, "\u00a7c[SignTP] Command signs are for operators only");
            return true;
        }

        String command = join(lines, 0, 3);
        if (command.isEmpty()) {
            return false;
        }

        execute(player, command);
        return true;
    }

    private boolean isProtectedSign(SignBlockEntity sign) {
        return isCommandSign(sign.getFrontText()) || isCommandSign(sign.getBackText());
    }

    private boolean isCommandSign(SignText text) {
        String[] lines = readLines(text);
        if (lines.length < 1) {
            return false;
        }
        String first = lines[0];
        if (first.equalsIgnoreCase("[tp]") && config.allowTpSigns) {
            return true;
        }
        if (first.equalsIgnoreCase("[preset]") && config.allowPresetSigns) {
            return true;
        }
        if ((first.equalsIgnoreCase("[cmd]") || first.equalsIgnoreCase("[command]") || first.startsWith("/"))
                && config.rawCommands.enabled) {
            return true;
        }
        return false;
    }

    private String[] readLines(SignText text) {
        String[] lines = new String[4];
        for (int i = 0; i < lines.length; i++) {
            lines[i] = text.getMessage(i, false).getString().trim();
        }
        return lines;
    }

    private String join(String[] lines, int from, int to) {
        StringBuilder sb = new StringBuilder();
        for (int i = from; i <= to; i++) {
            if (lines[i].isEmpty()) {
                continue;
            }
            if (sb.length() > 0) {
                sb.append(' ');
            }
            sb.append(lines[i]);
        }
        return sb.toString();
    }

    private boolean isOp(Level level, Player player) {
        MinecraftServer server = level.getServer();
        return server != null && server.getPlayerList().isOp(player.nameAndId());
    }

    private void execute(ServerPlayer player, String command) {
        MinecraftServer server = player.level().getServer();
        if (server == null) {
            return;
        }
        String cmd = command.trim();
        if (cmd.startsWith("/")) {
            cmd = cmd.substring(1);
        }
        if (cmd.isEmpty()) {
            return;
        }

        CommandSourceStack source = player.createCommandSourceStack().withPermission(PermissionSet.ALL_PERMISSIONS);
        try {
            server.getCommands().performPrefixedCommand(source, cmd);
        } catch (Exception e) {
            LOGGER.warn("[SignTP] Failed to execute command '{}': {}", cmd, e.toString());
            send(player, "\u00a7c[SignTP] Command failed, check the sign text");
        }
    }

    private void send(Player player, String message) {
        player.displayClientMessage(Component.literal(message), false);
    }
}

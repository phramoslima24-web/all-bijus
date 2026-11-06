package com.narutotoallbijuu.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.narutotoallbijuu.AllBijuuMod;
import com.narutotoallbijuu.bijuu.Bijuu;
import com.narutotoallbijuu.bijuu.BijuuState;
import com.narutotoallbijuu.data.BijuuWorldData;
import com.narutotoallbijuu.data.JinchuurikiData;
import com.narutotoallbijuu.entity.BijuuEntity;
import com.narutotoallbijuu.logic.QuestManager;
import com.narutotoallbijuu.logic.SealingManager;
import com.narutotoallbijuu.net.ModNet;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Arrays;
import java.util.List;

/** /allbijuu (op 2): ferramentas de teste e administracao. */
@Mod.EventBusSubscriber(modid = AllBijuuMod.MODID)
public final class ModCommands {
    private ModCommands() {
    }

    private static List<String> ids() {
        return Arrays.stream(Bijuu.values()).map(b -> b.id).toList();
    }

    @SubscribeEvent
    public static void register(RegisterCommandsEvent e) {
        CommandDispatcher<CommandSourceStack> d = e.getDispatcher();
        d.register(Commands.literal("allbijuu").requires(s -> s.hasPermission(2))
                .then(Commands.literal("seal").then(Commands.argument("bijuu", StringArgumentType.word())
                        .suggests((c, b) -> SharedSuggestionProvider.suggest(ids(), b)).executes(ModCommands::seal)))
                .then(Commands.literal("release").executes(ModCommands::release))
                .then(Commands.literal("reset").then(Commands.argument("bijuu", StringArgumentType.word())
                        .suggests((c, b) -> SharedSuggestionProvider.suggest(ids(), b)).executes(ModCommands::reset)))
                .then(Commands.literal("locate").then(Commands.argument("bijuu", StringArgumentType.word())
                        .suggests((c, b) -> SharedSuggestionProvider.suggest(ids(), b)).executes(ModCommands::locate)))
                .then(Commands.literal("mastery").then(Commands.argument("amount", DoubleArgumentType.doubleArg(0))
                        .executes(ModCommands::mastery)))
                .then(Commands.literal("status").executes(ModCommands::status)));
    }

    private static Bijuu arg(CommandContext<CommandSourceStack> c) {
        Bijuu b = Bijuu.byId(StringArgumentType.getString(c, "bijuu"));
        if (b == null) {
            c.getSource().sendFailure(Component.literal("Bijuu invalido: " + ids()));
        }
        return b;
    }

    private static int seal(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        ServerPlayer p = c.getSource().getPlayerOrException();
        Bijuu b = arg(c);
        if (b == null) {
            return 0;
        }
        if (JinchuurikiData.of(p).bijuu >= 0) {
            c.getSource().sendFailure(Component.literal("Voce ja hospeda um Bijuu (use /allbijuu release)."));
            return 0;
        }
        BijuuWorldData data = BijuuWorldData.get(p.server);
        BijuuWorldData.Entry en = data.get(b);
        if (en.state == BijuuState.SEALED) {
            c.getSource().sendFailure(Component.literal("Esse Bijuu ja esta selado em alguem."));
            return 0;
        }
        if (en.state == BijuuState.ENTITY && en.entity != null) {
            for (ServerLevel l : p.server.getAllLevels()) {
                Entity ent = l.getEntity(en.entity);
                if (ent instanceof BijuuEntity be) {
                    be.sealing = true;
                    be.discard();
                }
            }
        }
        SealingManager.sealTo(p, b);
        return 1;
    }

    private static int release(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        ServerPlayer p = c.getSource().getPlayerOrException();
        JinchuurikiData d = JinchuurikiData.of(p);
        if (d.bijuu < 0) {
            c.getSource().sendFailure(Component.literal("Sem Bijuu."));
            return 0;
        }
        SealingManager.onHostDeath(p, d);
        ModNet.sync(p);
        return 1;
    }

    private static int reset(CommandContext<CommandSourceStack> c) {
        Bijuu b = arg(c);
        if (b == null) {
            return 0;
        }
        BijuuWorldData data = BijuuWorldData.get(c.getSource().getServer());
        BijuuWorldData.Entry en = data.get(b);
        en.state = BijuuState.FREE;
        en.entity = null;
        en.host = null;
        en.token = null;
        en.availableAt = 0;
        data.setDirty();
        c.getSource().sendSuccess(() -> Component.literal(b.id + " liberado (FREE)."), true);
        return 1;
    }

    private static int locate(CommandContext<CommandSourceStack> c) {
        Bijuu b = arg(c);
        if (b == null) {
            return 0;
        }
        BlockPos l = QuestManager.lairXZ(c.getSource().getServer(), b);
        c.getSource().sendSuccess(() -> Component.literal(b.id + ": X=" + l.getX() + " Z=" + l.getZ()), false);
        return 1;
    }

    private static int mastery(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        ServerPlayer p = c.getSource().getPlayerOrException();
        JinchuurikiData d = JinchuurikiData.of(p);
        d.addMastery(DoubleArgumentType.getDouble(c, "amount"));
        ModNet.sync(p);
        return 1;
    }

    private static int status(CommandContext<CommandSourceStack> c) {
        BijuuWorldData data = BijuuWorldData.get(c.getSource().getServer());
        for (Bijuu b : Bijuu.values()) {
            BijuuWorldData.Entry en = data.get(b);
            c.getSource().sendSuccess(() -> Component.literal(b.id + ": " + en.state), false);
        }
        return 1;
    }
}

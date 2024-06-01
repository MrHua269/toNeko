package org.cneko.toneko.fabric.commands;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import org.cneko.toneko.common.api.NekoQuery;
import org.cneko.toneko.fabric.util.PlayerUtil;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

import static org.cneko.toneko.fabric.util.CommandUtil.*;
import static org.cneko.toneko.fabric.util.TextUtil.translatable;
public class ToNekoCommand {
    public static void init(){
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            //------------------------------------------------toneko-----------------------------------------------
            dispatcher.register(literal("toneko")
                    //----------------------------------------player-------------------------------------
                    .then(literal("player")
                            .then(argument("neko", StringArgumentType.string())
                                    .suggests(getOnlinePlayers)  //获取玩家列表
                                    .executes(ToNekoCommand::playerCommand)
                            )
                    )
                    //--------------------------------------------------aliases--------------------------------------
                    .then(literal("aliases")
                            .then(argument("neko", StringArgumentType.string())
                                    .suggests(getOnlinePlayers)  //获取玩家列表
                                    //-------------------------------------add---------------------------------------
                                    .then(literal("add")
                                            .then(argument("aliases", StringArgumentType.string())
                                                    .executes(ToNekoCommand::AliasesAdd)
                                            )
                                    ).then(literal("remove")
                                            .then(argument("aliases", StringArgumentType.string())
                                                    .executes(ToNekoCommand::AliasesRemove)
                                            )
                                    )
                            )
                    )
                    //-----------------------------------------------block--------------------------------------------
                    .then(literal("block")
                            .then(argument("neko", StringArgumentType.string())
                                    .suggests(getOnlinePlayers)
                                    //--------------------------------add------------------------------------
                                    .then(literal("add")
                                            .then(argument("block",StringArgumentType.string())
                                                    .then(argument("replace",StringArgumentType.string())
                                                            .then(argument("method",StringArgumentType.string())
                                                                    .suggests((context, builder) -> {
                                                                        builder.suggest("all");
                                                                        builder.suggest("word");
                                                                        return builder.buildFuture();
                                                                    })
                                                                    .executes(ToNekoCommand::addBlock)
                                                            )
                                                    )
                                            )
                                    )
                                    //----------------------------remove----------------------------------
                                    .then(literal("remove")
                                            .then(argument("block",StringArgumentType.string())
                                                    .executes(ToNekoCommand::removeBlock)
                                            )
                                    )
                            )
                    )
                    //-----------------------------------------xp------------------------------------------------
                    .then(literal("xp")
                            .then(argument("neko",StringArgumentType.string())
                                    .suggests(getOnlinePlayers)
                                    .executes(ToNekoCommand::xp)
                            )
                    )
                    //-------------------------------------------remove--------------------------------------
                    .then(literal("remove")
                            .then(argument("neko",StringArgumentType.string())
                                    .suggests(getOnlinePlayers)
                                    .executes(ToNekoCommand::remove)
                            )
                    )
                    //-------------------------------------help---------------------------------------------------
                    .then(literal("help")
                            .executes(ToNekoCommand::help)
                    )

                    //----------------------------------------无参数-----------------------------------------
                    .executes(ToNekoCommand::help)
            );
        });
    }

    private static int help(CommandContext<ServerCommandSource> context) {
        final ServerCommandSource source = context.getSource();
        source.sendMessage(translatable("command.toneko.help"));
        return 1;
    }

    private static int remove(CommandContext<ServerCommandSource> context) {
        final PlayerEntity player = context.getSource().getPlayer();
        String nekoName = context.getArgument("neko", String.class);
        NekoQuery.Neko neko = NekoQuery.getNeko(PlayerUtil.getPlayerByName(nekoName).getUuid());
        if(!neko.hasOwner(player.getUuid())){
            player.sendMessage(translatable("messages.toneko.notOwner"));
            return 1;
        }
        neko.removeOwner(player.getUuid());
        player.sendMessage(translatable("messages.toneko.remove"));
        neko.save();
        return 1;
    }

    private static int addBlock(CommandContext<ServerCommandSource> context) {
        final ServerCommandSource source = context.getSource();
        final PlayerEntity player = source.getPlayer();
        //获取关键信息
        String nekoName = context.getArgument("neko", String.class); //猫娘的名称
        String block = context.getArgument("block", String.class); //屏蔽词
        String replace = context.getArgument("replace", String.class); //替换词
        String method = context.getArgument("method", String.class); //all or word

        NekoQuery.Neko neko = NekoQuery.getNeko(PlayerUtil.getPlayerByName(nekoName).getUuid());

        if(!neko.hasOwner(player.getUuid())){
            player.sendMessage(translatable("messages.toneko.notOwner"));
            return 1;
        }
        // 添加屏蔽词
        neko.addBlock(block, replace, method);
        neko.save();
        player.sendMessage(translatable("messages.toneko.block.add"));
        return 1;
    }

    private static int removeBlock(CommandContext<ServerCommandSource> context) {
        final ServerCommandSource source = context.getSource();
        final PlayerEntity player = source.getPlayer();
        String nekoName = context.getArgument("neko", String.class); //猫娘的名称
        String block = context.getArgument("block", String.class); //屏蔽词

        NekoQuery.Neko neko = NekoQuery.getNeko(PlayerUtil.getPlayerByName(nekoName).getUuid());
        if(!neko.hasOwner(player.getUuid())){
            player.sendMessage(translatable("messages.toneko.notOwner"));
            return 1;
        }
        neko.removeBlock(block);
        neko.save();
        player.sendMessage(translatable("messages.toneko.block.remove"));
        return 1;
    }

    private static int xp(CommandContext<ServerCommandSource> context) {
        ServerPlayerEntity player = context.getSource().getPlayer();
        String nekoName = StringArgumentType.getString(context, "neko");
        NekoQuery.Neko neko = NekoQuery.getNeko(PlayerUtil.getPlayerByName(nekoName).getUuid());
        if(neko.hasOwner(player.getUuid())){
            player.sendMessage(translatable("command.toneko.xp", nekoName, neko.getXp(player.getUuid())));
        }else {
            player.sendMessage(translatable("messages.toneko.notOwner"));
        }
        return 1;
    }

    private static int AliasesRemove(CommandContext<ServerCommandSource> context) {
        ServerPlayerEntity player = context.getSource().getPlayer();
        NekoQuery.Neko neko = NekoQuery.getNeko(PlayerUtil.getPlayerByName(StringArgumentType.getString(context, "neko")).getUuid());
        if(neko.hasOwner(player.getUuid())){
            String aliases = StringArgumentType.getString(context, "aliases");
            neko.removeAlias(player.getUuid(), aliases);
            player.sendMessage(translatable("command.toneko.aliases.remove", aliases));
        }else {
            player.sendMessage(translatable("messages.toneko.notOwner"));
        }
        neko.save();
        return 1;
    }

    private static int AliasesAdd(CommandContext<ServerCommandSource> context) {
        ServerPlayerEntity player = context.getSource().getPlayer();
        NekoQuery.Neko neko = NekoQuery.getNeko(PlayerUtil.getPlayerByName(StringArgumentType.getString(context, "neko")).getUuid());
        if(neko.hasOwner(player.getUuid())){
            String aliases = StringArgumentType.getString(context, "aliases");
            neko.addAlias(player.getUuid(), aliases);
            player.sendMessage(translatable("command.toneko.aliases.add", aliases));
        }else {
            player.sendMessage(translatable("messages.toneko.notOwner"));
        }
        neko.save();
        return 1;
    }

    private static int playerCommand(CommandContext<ServerCommandSource> context) {
        ServerPlayerEntity player = context.getSource().getPlayer(); // 命令发送者
        String nekoName = StringArgumentType.getString(context, "neko"); // 猫娘名称
        PlayerEntity nekoPlayer = PlayerUtil.getPlayerByName(nekoName);
        NekoQuery.Neko neko = NekoQuery.getNeko(nekoPlayer.getUuid());
        assert player != null;
        if(!neko.isNeko()){
            // 不是猫娘
            player.sendMessage(translatable("command.toneko.player.notNeko", nekoName));
            return 1;
        }
        if(neko.hasOwner(player.getUuid())){
            // 已经是主人
            player.sendMessage(translatable("command.toneko.player.alreadyOwner", nekoName));
            return 1;
        }
        neko.addOwner(player.getUuid());
        neko.save();
        player.sendMessage(translatable("command.toneko.player.success", nekoName));
        return 1;
    }
}
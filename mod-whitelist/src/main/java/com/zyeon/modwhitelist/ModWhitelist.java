package com.zyeon.modwhitelist;

import com.mojang.brigadier.Command;
import com.zyeon.modwhitelist.config.WhitelistConfig;
import com.zyeon.modwhitelist.network.ModListPayload;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class ModWhitelist implements ModInitializer {

    public static final String MOD_ID = "modwhitelist";

    /** Config rechargeable via /modwhitelist reload */
    public static WhitelistConfig config;

    /**
     * Joueurs ayant envoyé leur liste de mods (vérification OK ou kick effectué).
     * Thread-safe : accédé depuis le thread réseau et le thread serveur.
     */
    private static final Set<UUID> confirmedPlayers = ConcurrentHashMap.newKeySet();

    /**
     * Joueurs connectés mais n'ayant pas encore envoyé leur liste.
     * UUID → timestamp de connexion (ms).
     * Si le délai expire → kick (client sans le mod, ou vanilla).
     */
    private static final Map<UUID, Long> pendingPlayers = new ConcurrentHashMap<>();

    /** Délai max pour recevoir la liste de mods (millisecondes). */
    private static final long TIMEOUT_MS = 10_000L;

    // ─── Initialisation ────────────────────────────────────────────────────────

    @Override
    public void onInitialize() {
        config = WhitelistConfig.load();

        registerNetwork();
        registerConnectionEvents();
        registerTickEvent();
        registerCommands();

        System.out.printf("[ModWhitelist] Prêt — %d mods autorisés, timeout=%ds%n",
                config.getAllowedMods().size(), TIMEOUT_MS / 1000);
    }

    // ─── Réseau ────────────────────────────────────────────────────────────────

    private void registerNetwork() {
        // Déclare le type de payload C2S (client → serveur)
        // Depuis 26.1 : playC2S() a été renommé serverboundPlay()
        PayloadTypeRegistry.serverboundPlay().register(ModListPayload.TYPE, ModListPayload.CODEC);

        // Réception de la liste de mods envoyée par le client
        ServerPlayNetworking.registerGlobalReceiver(ModListPayload.TYPE, (payload, context) -> {
            ServerPlayer player = context.player();
            UUID playerId = player.getUUID();

            // Marquer le joueur comme traité (annule le timeout)
            confirmedPlayers.add(playerId);
            pendingPlayers.remove(playerId);

            if (!config.isEnabled()) return;

            // Identifier les mods non autorisés
            Set<String> allowed = config.getAllowedMods();
            List<String> unauthorized = payload.modIds().stream()
                    .filter(mod -> !allowed.contains(mod))
                    .sorted()
                    .collect(Collectors.toList());

            if (unauthorized.isEmpty()) {
                System.out.printf("[ModWhitelist] ✔ %s — %d mods vérifiés%n",
                        player.getName().getString(), payload.modIds().size());
                return;
            }

            // Kick avec liste des mods problématiques
            String modList = String.join(", ", unauthorized);
            String message = config.getKickMessage().replace("%mods%", modList);

            // Exécution sur le thread serveur (le receiver tourne sur le thread réseau)
            context.server().execute(() ->
                    player.connection.disconnect(Component.literal(message))
            );

            System.out.printf("[ModWhitelist] ✘ %s expulsé — mods non autorisés : %s%n",
                    player.getName().getString(), modList);
        });
    }

    // ─── Événements de connexion ───────────────────────────────────────────────

    private void registerConnectionEvents() {
        // Quand un joueur entre en phase Play → démarrer le timeout
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            UUID id = handler.getPlayer().getUUID();
            pendingPlayers.put(id, System.currentTimeMillis());
        });

        // Nettoyage à la déconnexion
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            UUID id = handler.getPlayer().getUUID();
            confirmedPlayers.remove(id);
            pendingPlayers.remove(id);
        });
    }

    // ─── Tick : vérification des timeouts ─────────────────────────────────────

    private void registerTickEvent() {
        // Ce callback s'exécute sur le thread serveur → pas besoin de server.execute()
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (!config.isEnabled() || pendingPlayers.isEmpty()) return;

            long now = System.currentTimeMillis();
            List<UUID> expired = new ArrayList<>();

            pendingPlayers.forEach((uuid, joinTime) -> {
                if (now - joinTime >= TIMEOUT_MS) expired.add(uuid);
            });

            for (UUID playerId : expired) {
                pendingPlayers.remove(playerId);
                // getPlayerManager() -> getPlayerList() depuis 26.1
                ServerPlayer player = server.getPlayerList().getPlayer(playerId);
                if (player == null) continue;

                player.connection.disconnect(Component.literal(
                        "§cLe mod §fModWhitelist §cest requis pour rejoindre ce serveur.\n" +
                        "§7Installez le modpack officiel."
                ));
                System.out.printf("[ModWhitelist] ✘ %s expulsé — ModWhitelist absent (timeout)%n",
                        player.getName().getString());
            }
        });
    }

    // ─── Commandes ────────────────────────────────────────────────────────────

    private void registerCommands() {
        // CommandManager -> Commands, ServerCommandSource -> CommandSourceStack (26.1)
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
            dispatcher.register(Commands.literal("modwhitelist")
                .requires(src -> src.permissions().hasPermission(Permissions.COMMANDS_ADMIN)) // niveau "admin" (ex OP 3+)

                // /modwhitelist reload
                .then(Commands.literal("reload")
                    .executes(ctx -> {
                        config = WhitelistConfig.load();
                        ctx.getSource().sendSuccess(
                            () -> Component.literal("§a[ModWhitelist] Config rechargée — "
                                + config.getAllowedMods().size() + " mods autorisés."),
                            true
                        );
                        return Command.SINGLE_SUCCESS;
                    })
                )

                // /modwhitelist status
                .then(Commands.literal("status")
                    .executes(ctx -> {
                        String state = config.isEnabled() ? "§aactivé" : "§cdésactivé";
                        ctx.getSource().sendSuccess(
                            () -> Component.literal("§e[ModWhitelist] " + state
                                + " §7— " + config.getAllowedMods().size() + " mods autorisés"
                                + " — timeout " + (TIMEOUT_MS / 1000) + "s"),
                            false
                        );
                        return Command.SINGLE_SUCCESS;
                    })
                )
            )
        );
    }
}

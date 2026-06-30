package com.zyeon.modwhitelist;

import com.zyeon.modwhitelist.network.ModListPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Côté client : envoie la liste de tous les mods installés au serveur dès la connexion.
 * Ne s'exécute que sur le client — pas de code serveur ici.
 */
@Environment(EnvType.CLIENT)
public class ModWhitelistClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {

            // N'envoie le packet que si le serveur supporte notre payload
            // (i.e. le serveur a aussi ce mod installé)
            if (!ClientPlayNetworking.canSend(ModListPayload.TYPE)) return;

            // Collecte tous les IDs de mods installés côté client
            List<String> modIds = FabricLoader.getInstance().getAllMods().stream()
                    .map(mod -> mod.getMetadata().getId())
                    .sorted()
                    .collect(Collectors.toList());

            ClientPlayNetworking.send(new ModListPayload(modIds));
        });
    }
}

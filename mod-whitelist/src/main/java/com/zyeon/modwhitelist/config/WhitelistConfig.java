package com.zyeon.modwhitelist.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Configuration du mod — fichier : config/mod-whitelist.json
 *
 * Exemple de contenu :
 * {
 *   "enabled": true,
 *   "kick_message": "§cMods non autorisés : §f%mods%",
 *   "allowed_mods": ["minecraft", "fabricloader", "fabric-api", "sodium"]
 * }
 *
 * Variables disponibles dans kick_message :
 *   %mods%  → liste des mods non autorisés détectés
 */
public class WhitelistConfig {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = FabricLoader.getInstance()
            .getConfigDir().resolve("mod-whitelist.json");

    // Champs sérialisés dans le JSON (noms de champs = clés JSON)
    public boolean      enabled      = true;
    public String       kick_message = "§cMods non autorisés détectés :\n§f%mods%\n§7Contactez un administrateur.";
    public List<String> allowed_mods = new ArrayList<>(Arrays.asList(
            "minecraft",
            "fabricloader",
            "fabric-api",
            "java",
            "modwhitelist"     // ce mod lui-même doit être dans la whitelist
    ));

    // ─── Accesseurs ────────────────────────────────────────────────────────────

    public boolean isEnabled() {
        return enabled;
    }

    public String getKickMessage() {
        return kick_message;
    }

    /** Retourne un Set pour des lookups O(1). */
    public Set<String> getAllowedMods() {
        return new HashSet<>(allowed_mods);
    }

    // ─── Chargement / Sauvegarde ───────────────────────────────────────────────

    /**
     * Charge la config depuis le disque.
     * Si le fichier n'existe pas, crée un fichier par défaut et le retourne.
     */
    public static WhitelistConfig load() {
        if (!Files.exists(CONFIG_PATH)) {
            WhitelistConfig defaults = new WhitelistConfig();
            defaults.save();
            System.out.println("[ModWhitelist] Fichier config créé : " + CONFIG_PATH);
            System.out.println("[ModWhitelist] Éditez-le pour définir vos mods autorisés.");
            return defaults;
        }

        try (Reader reader = new InputStreamReader(
                Files.newInputStream(CONFIG_PATH), StandardCharsets.UTF_8)) {

            WhitelistConfig config = GSON.fromJson(reader, WhitelistConfig.class);
            if (config == null) config = new WhitelistConfig();
            if (config.allowed_mods == null) config.allowed_mods = new ArrayList<>();
            return config;

        } catch (IOException e) {
            System.err.println("[ModWhitelist] Erreur de lecture config : " + e.getMessage());
            return new WhitelistConfig();
        }
    }

    /** Écrit la config actuelle sur le disque. */
    public void save() {
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            try (Writer writer = new OutputStreamWriter(
                    Files.newOutputStream(CONFIG_PATH), StandardCharsets.UTF_8)) {
                GSON.toJson(this, writer);
            }
        } catch (IOException e) {
            System.err.println("[ModWhitelist] Erreur d'écriture config : " + e.getMessage());
        }
    }
}

# Mod Whitelist

Mod Fabric qui **expulse automatiquement les joueurs ayant des mods non autorisés** sur le serveur.

## Fonctionnement

```
[Client] Connexion → envoie la liste de ses mods installés
[Serveur] Reçoit la liste → compare à la whitelist → kick si mod non autorisé
```

Si un joueur n'a pas ce mod (client vanilla, etc.), il est expulsé au bout de **10 secondes**.

---

## Installation

Le mod doit être installé **côté serveur ET côté client**.

- **Serveur** : déposer le `.jar` dans `/mods/`
- **Clients** : inclure le `.jar` dans votre modpack

---

## Configuration

Le fichier est créé automatiquement au premier démarrage :

```
config/mod-whitelist.json
```

### Exemple

```json
{
  "enabled": true,
  "kick_message": "§cMods non autorisés :\n§f%mods%\n§7Installez le modpack officiel.",
  "allowed_mods": [
    "minecraft",
    "fabricloader",
    "fabric-api",
    "java",
    "modwhitelist",
    "sodium",
    "iris",
    "lithium",
    "ferrite-core",
    "modmenu"
  ]
}
```

### Champs

| Champ           | Type    | Description                                            |
|-----------------|---------|--------------------------------------------------------|
| `enabled`       | boolean | Active/désactive le mod sans le retirer                |
| `kick_message`  | string  | Message de kick — `%mods%` est remplacé par les mods  |
| `allowed_mods`  | liste   | IDs exacts des mods autorisés                          |

> **Conseil** : pour trouver l'ID exact d'un mod, regardez son `fabric.mod.json` (champ `"id"`), 
> ou utilisez Mod Menu en jeu.

### Codes couleur dans kick_message

`§a` vert · `§c` rouge · `§e` jaune · `§f` blanc · `§7` gris · `\n` saut de ligne

---

## Commandes

| Commande              | Effet                                         | Permission |
|-----------------------|-----------------------------------------------|------------|
| `/modwhitelist reload` | Recharge la config sans redémarrer le serveur | OP 3+      |
| `/modwhitelist status` | Affiche l'état du mod et le nombre de mods    | OP 3+      |

---

## Build

### Prérequis

- JDK 25 (Minecraft 26.1 l'exige)
- Gradle 9.4+ (ou utiliser le wrapper)
- IntelliJ IDEA 2025.3+ si vous l'utilisez

### Étapes

```bash
# 1. Générer le wrapper Gradle (une seule fois)
gradle wrapper --gradle-version 9.4

# 2. Vérifier les versions dans gradle.properties
#    → https://fabricmc.net/develop/

# 3. Compiler
./gradlew build

# 4. Le jar se trouve dans build/libs/mod-whitelist-1.0.0.jar
```

### Adapter les versions (gradle.properties)

Rendez-vous sur https://fabricmc.net/develop/ et copiez les versions correspondant à votre version de Minecraft dans `gradle.properties` :

```properties
minecraft_version=26.1.2
loader_version=0.19.3
loom_version=1.17-SNAPSHOT
fabric_api_version=0.152.1+26.1.2
```

> Depuis 26.1, le jeu n'est plus obfusqué : il n'y a **plus de mappings Yarn** à
> déclarer. Le code Mojang utilise directement des noms lisibles.

---

## Logs serveur

```
[ModWhitelist] Prêt — 12 mods autorisés, timeout=10s
[ModWhitelist] ✔ Steve — 12 mods vérifiés
[ModWhitelist] ✘ Hacker expulsé — mods non autorisés : wurst, xaeroplus
[ModWhitelist] ✘ VanillaPlayer expulsé — ModWhitelist absent (timeout)
```

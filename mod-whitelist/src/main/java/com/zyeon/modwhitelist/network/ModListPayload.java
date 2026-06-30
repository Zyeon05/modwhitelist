package com.zyeon.modwhitelist.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.List;

/**
 * Payload C2S : le client envoie la liste de ses mods au serveur.
 *
 * Depuis Minecraft 26.1 (mappings officiels Mojang), le système de paquets
 * personnalisés utilise CustomPacketPayload + StreamCodec (anciennement
 * CustomPayload + PacketCodec sous Yarn).
 */
public record ModListPayload(List<String> modIds) implements CustomPacketPayload {

    public static final Identifier ID =
            Identifier.fromNamespaceAndPath("modwhitelist", "mod_list");

    public static final CustomPacketPayload.Type<ModListPayload> TYPE =
            new CustomPacketPayload.Type<>(ID);

    public static final StreamCodec<RegistryFriendlyByteBuf, ModListPayload> CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()),
                    ModListPayload::modIds,
                    ModListPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

package ru.itemspeedrun;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;

/**
 * Сервер -> клиент. Сообщает HUD, в каком состоянии забег.
 *
 * @param state       одно из CLEAR / RUNNING / FINISHED / CANCELLED
 * @param item        предмет-цель
 * @param elapsedMs   для FINISHED / CANCELLED - итоговое время в миллисекундах
 * @param challenge   название усложнения или пустая строка
 */
public record TimerPayload(int state, Item item, long elapsedMs, String challenge) implements CustomPacketPayload {
	public static final int CLEAR = 0;
	public static final int RUNNING = 1;
	public static final int FINISHED = 2;
	public static final int CANCELLED = 3;

	public static final Type<TimerPayload> TYPE =
			new Type<>(Identifier.fromNamespaceAndPath(ItemSpeedrunMod.MOD_ID, "timer"));

	public static final StreamCodec<RegistryFriendlyByteBuf, TimerPayload> CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT, TimerPayload::state,
			ByteBufCodecs.registry(Registries.ITEM), TimerPayload::item,
			ByteBufCodecs.VAR_LONG, TimerPayload::elapsedMs,
			ByteBufCodecs.STRING_UTF8, TimerPayload::challenge,
			TimerPayload::new
	);

	@Override
	public Type<TimerPayload> type() {
		return TYPE;
	}
}

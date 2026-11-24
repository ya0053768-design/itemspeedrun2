package ru.itemspeedrun.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.resources.Identifier;
import ru.itemspeedrun.ItemSpeedrunMod;
import ru.itemspeedrun.TimerPayload;

public class ItemSpeedrunClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		// Пакеты от сервера обрабатываются в главном потоке клиента.
		ClientPlayNetworking.registerGlobalReceiver(TimerPayload.TYPE,
				(payload, context) -> TimerHud.apply(payload));

		HudElementRegistry.attachElementBefore(
				VanillaHudElements.CHAT,
				Identifier.fromNamespaceAndPath(ItemSpeedrunMod.MOD_ID, "timer"),
				TimerHud::extract);

		// при выходе из мира секундомер сбрасывается
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> TimerHud.reset());
	}
}

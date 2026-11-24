package ru.itemspeedrun;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;

public class ItemSpeedrunMod implements ModInitializer {
	public static final String MOD_ID = "itemspeedrun";

	@Override
	public void onInitialize() {
		PayloadTypeRegistry.clientboundPlay().register(TimerPayload.TYPE, TimerPayload.CODEC);
		ItemRunManager.init();
	}
}

package ru.itemspeedrun;

import java.util.Locale;

public final class TimeFormat {
	private TimeFormat() {
	}

	/** 83123 -> "01:23.123", а при часе и больше -> "1:01:23.123". */
	public static String format(long ms) {
		if (ms < 0) {
			ms = 0;
		}
		long millis = ms % 1000;
		long totalSeconds = ms / 1000;
		long seconds = totalSeconds % 60;
		long minutes = (totalSeconds / 60) % 60;
		long hours = totalSeconds / 3600;
		if (hours > 0) {
			return String.format(Locale.ROOT, "%d:%02d:%02d.%03d", hours, minutes, seconds, millis);
		}
		return String.format(Locale.ROOT, "%02d:%02d.%03d", minutes, seconds, millis);
	}
}

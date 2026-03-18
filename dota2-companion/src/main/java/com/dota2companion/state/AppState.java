package com.dota2companion.state;

import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Глобальное состояние выбранного игрока.
 * Используется, чтобы вкладка "Матчи" могла обновляться после ввода аккаунта в "Профиль".
 */
public final class AppState {

    private AppState() {}

    private static volatile long currentAccountId = -1;
    private static volatile String currentPersonaName = "";

    private static final CopyOnWriteArrayList<Runnable> accountChangeListeners = new CopyOnWriteArrayList<>();

    public static long getCurrentAccountId() {
        return currentAccountId;
    }

    public static String getCurrentPersonaName() {
        return currentPersonaName;
    }

    public static void setCurrentPlayer(long accountId, String personaName) {
        String nextName = personaName == null ? "" : personaName;
        boolean changed = accountId != currentAccountId || !Objects.equals(nextName, currentPersonaName);
        currentAccountId = accountId;
        currentPersonaName = nextName;

        if (changed) {
            for (Runnable r : accountChangeListeners) {
                try {
                    r.run();
                } catch (Exception ignored) {
                    // Не ломаем приложение из-за обработчика UI.
                }
            }
        }
    }

    public static void addAccountChangeListener(Runnable listener) {
        if (listener != null) {
            accountChangeListeners.add(listener);
        }
    }
}


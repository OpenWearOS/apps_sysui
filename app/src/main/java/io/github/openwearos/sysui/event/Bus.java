package io.github.openwearos.sysui.event;

import android.os.Handler;
import android.os.Looper;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

public final class Bus {
    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private static final Map<Class<?>, Object> LAST = new ConcurrentHashMap<>();
    private static final List<Subscription<?>> SUBSCRIPTIONS = new CopyOnWriteArrayList<>();

    private Bus() {
    }

    public static void post(Object event) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            deliver(event);
        } else {
            MAIN.post(() -> deliver(event));
        }
    }

    private static void deliver(Object event) {
        LAST.put(event.getClass(), event);
        for (Subscription<?> s : SUBSCRIPTIONS) {
            s.offer(event);
        }
    }

    public static <T> T last(Class<T> type) {
        return type.cast(LAST.get(type));
    }

    public static <T> Runnable subscribe(Class<T> type, Consumer<T> handler, boolean replay) {
        Subscription<T> subscription = new Subscription<>(type, handler);
        SUBSCRIPTIONS.add(subscription);
        if (replay) {
            T previous = last(type);
            if (previous != null) {
                handler.accept(previous);
            }
        }
        return () -> SUBSCRIPTIONS.remove(subscription);
    }

    public static <T> Runnable onNext(Class<T> type, Consumer<T> handler) {
        return subscribe(type, handler, false);
    }

    private static final class Subscription<T> {
        private final Class<T> mType;
        private final Consumer<T> mHandler;

        Subscription(Class<T> type, Consumer<T> handler) {
            mType = type;
            mHandler = handler;
        }

        void offer(Object event) {
            if (mType.isInstance(event)) {
                mHandler.accept(mType.cast(event));
            }
        }
    }
}
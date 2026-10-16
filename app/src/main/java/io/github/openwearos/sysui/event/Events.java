package io.github.openwearos.sysui.event;

import io.github.openwearos.sysui.UiMode;

public final class Events {
    private Events() {
    }

    public static final class Battery {
        public final int percent;
        public final boolean charging;

        public Battery(int percent, boolean charging) {
            this.percent = percent;
            this.charging = charging;
        }
    }

    public static final class Ambient {
        public final boolean ambient;

        public Ambient(boolean ambient) {
            this.ambient = ambient;
        }
    }

    public static final class Dnd {
        public final boolean on;

        public Dnd(boolean on) {
            this.on = on;
        }
    }

    public static final class Bedtime {
        public final boolean on;

        public Bedtime(boolean on) {
            this.on = on;
        }
    }

    public static final class Theater {
        public final boolean on;

        public Theater(boolean on) {
            this.on = on;
        }
    }

    public static final class Airplane {
        public final boolean on;

        public Airplane(boolean on) {
            this.on = on;
        }
    }

    public static final class NotificationCount {
        public final int count;

        public NotificationCount(int count) {
            this.count = count;
        }
    }

    public static final class StreamChanged {
    }

    public static final class UiModeRequest {
        public final UiMode mode;

        public UiModeRequest(UiMode mode) {
            this.mode = mode;
        }
    }

    public static final class UiModeChange {
        public final UiMode mode;

        public UiModeChange(UiMode mode) {
            this.mode = mode;
        }
    }
}
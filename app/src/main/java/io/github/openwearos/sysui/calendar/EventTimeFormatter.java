package io.github.openwearos.sysui.calendar;

public interface EventTimeFormatter {

    final class TimeString {
        public final String text;
        public final boolean highlighted;

        private TimeString(String text, boolean highlighted) {
            if (text == null) throw new NullPointerException("Null text");
            this.text = text;
            this.highlighted = highlighted;
        }

        public static TimeString highlighted(String text) {
            return new TimeString(text, true);
        }

        public static TimeString plain(String text) {
            return new TimeString(text, false);
        }
    }

    class EventOutsideDayException extends Exception {
        public EventOutsideDayException(String message) {
            super(message);
        }
    }

    TimeString format(EventInstance event, long dayMillis) throws EventOutsideDayException;
}

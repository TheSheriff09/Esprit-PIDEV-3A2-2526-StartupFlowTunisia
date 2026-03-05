package tn.esprit.events;

import javafx.application.Platform;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

// NEW FILE
public class EventBus {
    private static final EventBus INSTANCE = new EventBus();
    private final Map<Class<?>, List<Consumer<Object>>> listeners = new ConcurrentHashMap<>();

    private EventBus() {
    }

    public static EventBus getInstance() {
        return INSTANCE;
    }

    public <T> void subscribe(Class<T> eventType, Consumer<T> listener) {
        listeners.computeIfAbsent(eventType, k -> new ArrayList<>())
                .add(obj -> listener.accept(eventType.cast(obj)));
    }

    public void publish(Object event) {
        List<Consumer<Object>> eventListeners = listeners.get(event.getClass());
        if (eventListeners != null) {
            for (Consumer<Object> listener : eventListeners) {
                if (Platform.isFxApplicationThread()) {
                    listener.accept(event);
                } else {
                    Platform.runLater(() -> listener.accept(event));
                }
            }
        }
    }
}

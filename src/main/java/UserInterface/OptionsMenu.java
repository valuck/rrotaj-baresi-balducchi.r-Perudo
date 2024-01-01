package UserInterface;

import com.google.gson.internal.LinkedTreeMap;

import java.util.LinkedList;
import java.util.function.Function;

public class OptionsMenu {
    private final LinkedTreeMap<String, Function<String, Void>> options;

    public OptionsMenu() {
        this.options = new LinkedTreeMap<>();
    }

    public void addOption(String text, Function<String, Void> function) {
        this.options.put(text, function);
    }

    public void removeOption(String text) {
        this.options.remove(text);
    }

    public void doOption(String text) {
        if (this.options.containsKey(text))
            this.options.get(text).apply(text);
    }

    public LinkedList<String> getOptions() {
        LinkedList<String> options = new LinkedList<>();

        this.options.forEach((key, value) -> {
            options.add(key);
        });

        return options;
    }
}

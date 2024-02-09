package UserInterface;

import com.google.gson.internal.LinkedTreeMap;

import java.util.LinkedList;
import java.util.function.Function;

public class OptionsMenu {
    private final LinkedTreeMap<String, Function<Void, Void>> options;

    public OptionsMenu() {
        this.options = new LinkedTreeMap<>();
    }

    public void addOption(String text, Function<Void, Void> function) {
        this.options.putIfAbsent(text, function); // add a new option
    }

    public void removeOption(String text) {
        this.options.remove(text); // Remove an option
    }

    public void clearOptions() { this.options.clear(); }

    public void doOption(String text) {
        // Check if the option is available and run its code
        if (this.options.containsKey(text)) {
            Function<Void, Void> function = this.options.get(text);

            if (function != null)
                new Thread(new Runnable() { // Execute in a new thread to prevent options from yielding the console on readln
                    @Override
                    public void run() {
                        function.apply(null);
                    }
                }).start();
        }
    }

    public LinkedList<String> getOptions() {
        // Generate the list of options
        LinkedList<String> options = new LinkedList<>();
        this.options.forEach((key, value) -> {
            options.add(key);
        });

        return options;
    }
}

package Storage;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.internal.LinkedTreeMap;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Paths;

public class ClientStorage {
    private static LinkedTreeMap<String, Object> settings;
    private static final String settingsPath = "settings.json";

    private static boolean loadCheck() { // If false, data cannot be loaded or initialized
        if (settings == null)
            return loadSettings() == null;

        return false;
    }

    public static boolean saveSettings() {
        if (loadCheck())
            return false;

        try {
            // write the settings to a Json file
            FileWriter writer = new FileWriter(settingsPath);
            writer.write(new Gson().toJson(settings));
            writer.close();

        } catch (IOException e) {
            System.err.println("Error while saving settings:");
            e.printStackTrace();
            return false;
        }

        return true;
    }

    public static boolean overwriteSettings(LinkedTreeMap<String, Object> newSettings) {
        // overwrite the settings and save them
        settings = newSettings;
        return saveSettings();
    }

    public static LinkedTreeMap<String, Object> loadSettings() {
        LinkedTreeMap<String, Object> loaded;

        try {
            // reads data from the saved json file
            byte[] fileBytes = Files.readAllBytes(Paths.get(settingsPath));
            String jsonData = new String(fileBytes);

            // if empty creates new settings
            if (jsonData.isEmpty())
                loaded = new LinkedTreeMap<>();
            else
                loaded = new Gson().fromJson(jsonData, LinkedTreeMap.class);

            // set the achieved data as settings
            settings = loaded;
            return loaded;
        } catch (NoSuchFileException | JsonSyntaxException e) {
            // create new settings if the file is not found or the Json syntax is malformed
            loaded = new LinkedTreeMap<>();
            overwriteSettings(loaded);
            return loaded;

        } catch (IOException e) {
            System.err.println("Error while loading settings:");
            e.printStackTrace();
        }

        return null;
    }

    public static boolean updateSetting(String key, Object value, boolean save) {
        if (loadCheck())
            return false;

        // update or add the value to the settings by its key
        if (settings.containsKey(key))
            settings.replace(key, value);
        else
            settings.put(key, value);

        if (save) // save to file if required
            return saveSettings();

        return true;
    }

    public static Object getSetting(String key) {
        if (loadCheck())
            return false;

        // retrieve the setting by the key, if present
        if (settings.containsKey(key)) {
            return settings.get(key);
        }

        return null;
    }

    public static boolean removeSetting(String key, boolean save) {
        if (loadCheck())
            return false;

        // remove the setting under the specified key
        if (settings.containsKey(key)) {
            settings.remove(key);

            if (save) // save to file if required
                return saveSettings();

            return true;
        }

        return false;
    }
}

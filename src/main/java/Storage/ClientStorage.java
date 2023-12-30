package Storage;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.internal.LinkedTreeMap;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Paths;

public class ClientStorage {
    private static LinkedTreeMap<String, Object> settings;
    private static String settingsPath = "settings.json";

    private static boolean loadCheck() {
        if (settings == null)
            return loadSettings() != null;

        return true;
    }

    public static boolean saveSettings() {
        if (!loadCheck())
            return false;

        try {
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
        settings = newSettings;
        return saveSettings();
    }

    public static LinkedTreeMap<String, Object> loadSettings() {
        LinkedTreeMap<String, Object> loaded;

        try {
            byte[] fileBytes = Files.readAllBytes(Paths.get(settingsPath));
            String jsonData = new String(fileBytes);

            if (jsonData.isEmpty())
                loaded = new LinkedTreeMap<>();
            else
                loaded = new Gson().fromJson(jsonData, LinkedTreeMap.class);

            settings = loaded;
            return loaded;
        } catch (NoSuchFileException | JsonSyntaxException e) {
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
        if (!loadCheck())
            return false;

        if (settings.containsKey(key))
            settings.replace(key, value);
        else
            settings.put(key, value);

        if (save)
            return saveSettings();

        return true;
    }

    public static Object getSetting(String key) {
        if (!loadCheck())
            return false;

        if (settings.containsKey(key)) {
            return settings.get(key);
        }

        return null;
    }

    public static boolean removeSetting(String key, boolean save) {
        if (!loadCheck())
            return false;

        if (settings.containsKey(key)) {
            settings.remove(key);

            if (save)
                return saveSettings();

            return true;
        }

        return false;
    }
}

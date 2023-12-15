package Messaging;

import com.google.gson.internal.LinkedTreeMap;

public class DataUnion {
    private int integer;
    private String string;
    private boolean bool;
    private double doub;
    private float flo;

    private void fromMap(LinkedTreeMap<String, Object> map) {
        map.forEach((key, value) -> {
            System.out.println(key + " " + value);
        });

        if (map.containsKey("bool")) {
            this.bool = (boolean) map.get("bool");
        }
        if (map.containsKey("doub")) {
            Object value = map.get("doub");
            if (value instanceof Double) {
                this.doub = ((Double) value).doubleValue();
            } else if (value instanceof Float) {
                this.doub = ((Float) value).doubleValue();
            } else {
                // Handle other types as needed
            }
        }
        if (map.containsKey("flo")) {
            Object value = map.get("flo");
            if (value instanceof Double) {
                this.flo = ((Double) value).floatValue();
            } else if (value instanceof Float) {
                this.flo = (float) value;
            } else {
                // Handle other types as needed
            }
        }
        if (map.containsKey("integer")) {
            Object value = map.get("integer");
            if (value instanceof Integer) {
                this.integer = (int) value;
            } else if (value instanceof Double) {
                this.integer = ((Double) value).intValue();
            } else if (value instanceof Float) {
                this.integer = ((Float) value).intValue();
            } else {
                // Handle other types as needed
            }
        }
        if (map.containsKey("string")) {
            this.string = (String) map.get("string");
        }
    }

    public<T> DataUnion(T data) {
        Class<T> type = (Class<T>) data.getClass();

        if (type.equals(Boolean.class))
            this.bool = (boolean) data;
        else if (type.equals(Double.class))
            this.doub = (double) data;
        else if (type.equals(Float.class))
            this.flo = (float) data;
        else if (type.equals(Integer.class))
            this.integer = (Integer) data;
        else if (type.equals(String.class))
            this.string = (String) data;
        else if (type.equals(LinkedTreeMap.class))
            fromMap((LinkedTreeMap) data);
        else
            throw new IllegalArgumentException("Unsupported type: " + type.getSimpleName());
    }

    public<T> Object getValue(Class<T> type) {
        if (type.equals(Boolean.class))
            return this.bool;
        else if (type.equals(Double.class))
            return this.doub;
        else if (type.equals(Float.class))
            return this.flo;
        else if (type.equals(Integer.class))
            return this.integer;
        else if (type.equals(String.class))
            return this.string;
        else
            throw new IllegalArgumentException("Unsupported type: " + type.getSimpleName());
    }
}

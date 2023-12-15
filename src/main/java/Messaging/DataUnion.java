package Messaging;

import com.sun.jdi.ClassType;

public class DataUnion {
    private Integer integer;
    private String string;
    private boolean bool;

    public<T> DataUnion(T data) {
        Class<T> type = (Class<T>) data.getClass();

        if (type.equals(Boolean.class))
            this.bool = (boolean) data;
        else if (type.equals(Integer.class))
            this.integer = (Integer) data;
        else if (type.equals(String.class))
            this.string = (String) data;
        else
            throw new IllegalArgumentException("Unsupported type: " + type.getSimpleName());
    }

    public<T> Object getValue(Class<T> type) {
        if (type.equals(Boolean.class))
            return this.bool;
        else if (type.equals(Integer.class))
            return this.integer;
        else if (type.equals(String.class))
            return this.string;
        else
            throw new IllegalArgumentException("Unsupported type: " + type.getSimpleName());
    }
}

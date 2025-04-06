package net.vg.lootexplorer.util;

import java.lang.reflect.Field;

public class ReflectionUtils {

    /**
     * Gets the value of a private field in a given class instance using reflection.
     *
     * @param clazz The class to access.
     * @param instance The instance of the class from which to retrieve the field.
     * @param fieldName The name of the field.
     * @param <T> The type of the field's value.
     * @param <C> The type of the class instance.
     * @return The value of the field.
     * @throws IllegalStateException if the field cannot be found or accessed.
     */
    public static <T, C> T getPrivateFieldValue(Class<C> clazz, C instance, String fieldName) {
        try {
            Field field = clazz.getDeclaredField(fieldName);
            field.setAccessible(true);  // Make the private field accessible
            return (T) field.get(instance);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new IllegalStateException("Failed to access field '" + fieldName + "' in class " + clazz.getName(), e);
        }
    }
}

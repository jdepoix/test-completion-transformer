package org.jdepoix.ast.serialization;

import java.lang.reflect.Field;

class ReflectionUtils {
    public static Field findDeclaredFieldInSuperClasses(Class<?> clazz, String fieldName) throws NoSuchFieldException {
        try {
            return clazz.getDeclaredField(fieldName);
        } catch (NoSuchFieldException e) {
            final Class<?> superclass = clazz.getSuperclass();
            if (superclass != null) {
                return ReflectionUtils.findDeclaredFieldInSuperClasses(superclass, fieldName);
            }
            throw e;
        }
    }
}

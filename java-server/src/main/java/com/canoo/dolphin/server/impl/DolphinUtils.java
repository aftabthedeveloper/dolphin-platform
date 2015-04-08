package com.canoo.dolphin.server.impl;

import com.canoo.dolphin.collections.ObservableList;
import com.canoo.dolphin.mapping.DolphinBean;
import com.canoo.dolphin.mapping.DolphinProperty;
import com.canoo.dolphin.mapping.Property;

import java.lang.reflect.Field;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by hendrikebbers on 26.03.15.
 */
public class DolphinUtils {

    public static Object getPrivileged(final Field field, final Object bean) {
        return AccessController.doPrivileged(new PrivilegedAction<Object>() {
            @Override
            public Object run() {
                boolean wasAccessible = field.isAccessible();
                try {
                    field.setAccessible(true);
                    return field.get(bean);
                } catch (IllegalArgumentException | IllegalAccessException ex) {
                    throw new IllegalStateException("Cannot set field: "
                            + field, ex);
                } finally {
                    field.setAccessible(wasAccessible);
                }
            }
        });
    }

    public static void setPrivileged(final Field field, final Object bean,
                                     final Object value) {
        AccessController.doPrivileged(new PrivilegedAction<Void>() {
            @Override
            public Void run() {
                boolean wasAccessible = field.isAccessible();
                try {
                    field.setAccessible(true);
                    field.set(bean, value);
                    return null; // return nothing...
                } catch (IllegalArgumentException | IllegalAccessException ex) {
                    throw new IllegalStateException("Cannot set field: "
                            + field, ex);
                } finally {
                    field.setAccessible(wasAccessible);
                }
            }
        });
    }

    public static List<Field> getInheritedDeclaredFields(Class<?> type) {
        List<Field> result = new ArrayList<>();
        Class<?> i = type;
        while (i != null && i != Object.class) {
            result.addAll(Arrays.asList(i.getDeclaredFields()));
            i = i.getSuperclass();
        }
        return result;
    }

    public static boolean isBasicType(Class<?> cls) {
        if(cls.isPrimitive()) {
            return true;
        }
        if(cls.equals(String.class)) {
            return true;
        }
        if(cls.equals(Boolean.class)) {
            return true;
        }
        if(cls.equals(Integer.class)) {
            return true;
        }
        if(cls.equals(Double.class)) {
            return true;
        }
        if(cls.equals(Float.class)) {
            return true;
        }
        if(cls.equals(Long.class)) {
            return true;
        }
        if(cls.equals(Byte.class)) {
            return true;
        }
        if(cls.equals(Short.class)) {
            return true;
        }
        return false;
    }

    public static <T> void forAllProperties(Class<T> beanClass, FieldIterator fieldIterator) {
        for (Field field : DolphinUtils.getInheritedDeclaredFields(beanClass)) {
            if (Property.class.isAssignableFrom(field.getType())) {

                String attributeName = getDolphinAttributePropertyNameForField(field);
                fieldIterator.run(field, attributeName);
            }
        }
    }

    public static <T> void forAllObservableLists(Class<T> beanClass, FieldIterator fieldIterator) {
        for (Field field : DolphinUtils.getInheritedDeclaredFields(beanClass)) {
            if (ObservableList.class.isAssignableFrom(field.getType())) {

                String attributeName = getDolphinAttributePropertyNameForField(field);
                fieldIterator.run(field, attributeName);
            }
        }
    }

    public static String getDolphinAttributePropertyNameForField(Field propertyField) {
        String attributeName = propertyField.getName();
        DolphinProperty propertyAnnotation = propertyField.getAnnotation(DolphinProperty.class);
        if (propertyAnnotation != null && !propertyAnnotation.value().isEmpty()) {
            attributeName = propertyAnnotation.value();
        }
        return attributeName;
    }

    public static String getDolphinPresentationModelTypeForClass(Class<?> beanClass) {
        final DolphinBean beanAnnotation = beanClass.getAnnotation(DolphinBean.class);
        return beanAnnotation == null || beanAnnotation.value().isEmpty()? beanClass.getName() : beanAnnotation.value();
    }

    public interface FieldIterator {
        public abstract void run(Field field, String attributeName);
    }
}
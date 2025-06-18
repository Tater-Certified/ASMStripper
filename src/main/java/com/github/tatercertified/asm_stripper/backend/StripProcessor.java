package com.github.tatercertified.asm_stripper.backend;

import com.github.tatercertified.asm_stripper.api.annotation.Strip;
import com.github.tatercertified.asm_stripper.backend.stripper.FieldStripper;
import org.objectweb.asm.tree.ClassNode;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

public final class StripProcessor {
    private final List<Field> PROCESSED = new ArrayList<>();

    public void processStrips(String clazzName, ClassNode node) {
        for (Field field : Cleanup.class.getDeclaredFields()) {
            if (field.isAnnotationPresent(Strip.class) && !PROCESSED.contains(field)) {
                field.setAccessible(true);
                Strip annotation = field.getAnnotation(Strip.class);
                if (clazzName.equals(annotation.className())) {
                    try {
                        Object fieldObj = field.get(null);
                        if (fieldObj instanceof String[][] fields) {
                            FieldStripper.stripFieldsWithClassNode(annotation.className(), node, fields);
                            PROCESSED.add(field);
                        }
                    } catch (IllegalAccessException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }
    }
}

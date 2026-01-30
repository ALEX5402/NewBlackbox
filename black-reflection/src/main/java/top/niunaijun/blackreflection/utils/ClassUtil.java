package top.niunaijun.blackreflection.utils;

import top.niunaijun.blackreflection.annotation.BClass;
import top.niunaijun.blackreflection.annotation.BClassName;
import top.niunaijun.blackreflection.annotation.BClassNameNotProcess;

/**
 *  on 2022/2/18.
 * * ∧＿∧
 * (`･ω･∥
 * 丶　つ０
 * しーＪ
 * 此处无Bug
 */
public class ClassUtil {
    public static Class<?> classReady(Class<?> clazz) {
        BClassNameNotProcess bClassNameNotProcess = clazz.getAnnotation(BClassNameNotProcess.class);
        if (bClassNameNotProcess != null) {
            return classReady(bClassNameNotProcess.value());
        }
        BClass annotation = clazz.getAnnotation(BClass.class);
        if (annotation != null) {
            return annotation.value();
        }
        BClassName bClassName = clazz.getAnnotation(BClassName.class);
        if (bClassName != null) {
            return classReady(bClassName.value());
        }
        return null;
    }

    private static Class<?> classReady(String clazz) {
        try {
            return Class.forName(clazz);
        } catch (ClassNotFoundException ignored) {
        }
        return null;
    }
}

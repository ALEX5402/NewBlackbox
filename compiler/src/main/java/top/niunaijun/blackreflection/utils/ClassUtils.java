package top.niunaijun.blackreflection.utils;


/**
 * Created by sunwanquan on 2020/1/8.
 * * ∧＿∧
 * (`･ω･∥
 * 丶　つ０
 * しーＪ
 * 此处无Bug
 */
public class ClassUtils {

    public static String getPackage(String className) {
        String[] sp = className.split("[.]");
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < sp.length - 1; i++) {
            builder.append(sp[i]);
            if (i != sp.length - 2) {
                builder.append(".");
            }
        }
        return builder.toString();
    }

    public static String getName(String className) {
        String[] sp = className.split("[.]");
        return sp[sp.length - 1];
    }
}

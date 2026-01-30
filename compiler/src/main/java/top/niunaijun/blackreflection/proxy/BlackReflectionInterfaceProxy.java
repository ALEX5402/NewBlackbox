package top.niunaijun.blackreflection.proxy;

import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.lang.model.element.Modifier;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.MirroredTypeException;
import javax.lang.model.type.TypeMirror;

import top.niunaijun.blackreflection.BlackReflectionInterfaceInfo;
import top.niunaijun.blackreflection.annotation.BConstructor;
import top.niunaijun.blackreflection.annotation.BConstructorNotProcess;
import top.niunaijun.blackreflection.annotation.BFieldCheckNotProcess;
import top.niunaijun.blackreflection.annotation.BFieldNotProcess;
import top.niunaijun.blackreflection.annotation.BFieldSetNotProcess;
import top.niunaijun.blackreflection.annotation.BMethodCheckNotProcess;
import top.niunaijun.blackreflection.annotation.BParamClass;
import top.niunaijun.blackreflection.annotation.BClassNameNotProcess;
import top.niunaijun.blackreflection.annotation.BParamClassName;

/**
 * Created by sunwanquan on 2020/1/8.
 * * ∧＿∧
 * (`･ω･∥
 * 丶　つ０
 * しーＪ
 * 此处无Bug
 */
public class BlackReflectionInterfaceProxy {

    private final List<BlackReflectionInterfaceInfo> mReflections = new ArrayList<>();
    // fake.android.app.ActivityThreadStatic or ActivityThreadContext
    private final String mClassName;
    // fake.android.app
    private final String mPackageName;
    // fake.android.app.ActivityThread
    private final String mOrigClassName;
    private Map<String, String> realMaps;

    public BlackReflectionInterfaceProxy(String packageName, String className, String origClassName) {
        mPackageName = packageName;
        mClassName = className;
        mOrigClassName = origClassName;
    }

    public JavaFile generateInterfaceCode() {
        String finalClass = mClassName
                .replace(mPackageName + ".", "")
                .replace(".", "");
        AnnotationSpec annotationSpec = AnnotationSpec.builder(BClassNameNotProcess.class)
                .addMember("value","$S", realMaps.get(mOrigClassName))
                .build();

        // generaClass
        TypeSpec.Builder interfaceBuilder = TypeSpec.interfaceBuilder(finalClass)
                .addAnnotation(annotationSpec)
                .addModifiers(Modifier.PUBLIC);

        for (BlackReflectionInterfaceInfo reflection : mReflections) {
            MethodSpec.Builder method = MethodSpec.methodBuilder(reflection.getExecutableElement().getSimpleName().toString())
                    .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT);

            List<ParameterSpec> parameterSpecs = new ArrayList<>();
            for (VariableElement typeParameter : reflection.getExecutableElement().getParameters()) {
                ParameterSpec.Builder builder = ParameterSpec.builder(ClassName.get(typeParameter.asType()), typeParameter.getSimpleName().toString());
                if (typeParameter.getAnnotation(BParamClassName.class) != null) {
                    BParamClassName annotation = typeParameter.getAnnotation(BParamClassName.class);
                    builder.addAnnotation(AnnotationSpec.get(annotation));
                }
                if (typeParameter.getAnnotation(BParamClass.class) != null) {
                    BParamClass annotation = typeParameter.getAnnotation(BParamClass.class);
                    String annotationValue = getClass(annotation).toString();
                    Class<?> aClass = parseBaseClass(annotationValue);
                    if (aClass != null) {
                        builder.addAnnotation(AnnotationSpec.builder(BParamClass.class)
                                .addMember("value", "$T.class", aClass).build());
                    } else {
                        builder.addAnnotation(AnnotationSpec.builder(BParamClass.class)
                                .addMember("value", annotationValue + ".class").build());
                    }
                }

                ParameterSpec build = builder.build();
                parameterSpecs.add(build);
                method.addParameter(build);
            }
            TypeName typeName = TypeName.get(reflection.getExecutableElement().getReturnType());
            method.returns(typeName.box());
            if (reflection.isField()) {
                method.addAnnotation(AnnotationSpec.builder(BFieldNotProcess.class).build());
                // set field
                interfaceBuilder.addMethod(generateFieldSet(reflection));
                // check field
                interfaceBuilder.addMethod(generateFieldCheck(reflection));
            } else {
                BConstructor annotation = reflection.getExecutableElement().getAnnotation(BConstructor.class);
                if (annotation != null) {
                    method.addAnnotation(AnnotationSpec.builder(BConstructorNotProcess.class).build());
                } else {
                    // check method
                    interfaceBuilder.addMethod(generateMethodCheck(reflection, parameterSpecs));
                }
            }
            interfaceBuilder.addMethod(method.build());
        }
        return JavaFile.builder(mPackageName, interfaceBuilder.build()).build();
    }

    private MethodSpec generateFieldSet(BlackReflectionInterfaceInfo reflection) {
        MethodSpec.Builder method = MethodSpec.methodBuilder("_set_" + reflection.getExecutableElement().getSimpleName().toString())
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .addParameter(ClassName.get("java.lang", "Object"), "value", Modifier.FINAL)
                .addAnnotation(AnnotationSpec.builder(BFieldSetNotProcess.class).build());
        return method.build();
    }

    private MethodSpec generateFieldCheck(BlackReflectionInterfaceInfo reflection) {
        MethodSpec.Builder method = MethodSpec.methodBuilder("_check_" + reflection.getExecutableElement().getSimpleName().toString())
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .addAnnotation(AnnotationSpec.builder(BFieldCheckNotProcess.class).build())
                .returns(Field.class);
        return method.build();
    }

    private MethodSpec generateMethodCheck(BlackReflectionInterfaceInfo reflection, List<ParameterSpec> parameterSpecs) {
        MethodSpec.Builder method = MethodSpec.methodBuilder("_check_" + reflection.getExecutableElement().getSimpleName().toString())
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .addAnnotation(AnnotationSpec.builder(BMethodCheckNotProcess.class).build())
                .returns(Method.class);
        for (ParameterSpec parameterSpec : parameterSpecs) {
            method.addParameter(parameterSpec);
        }
        return method.build();
    }

    public void add(BlackReflectionInterfaceInfo interfaceInfo) {
        mReflections.add(interfaceInfo);
    }

    public void setRealMap(Map<String, String> realMaps) {
        this.realMaps = realMaps;
    }

    private static TypeMirror getClass(BParamClass annotation) {
        try {
            annotation.value(); // this should throw
        } catch (MirroredTypeException mte) {
            return mte.getTypeMirror();
        }
        return null; // can this ever happen ??
    }

    private static Class<?> parseBaseClass(String className) {
        switch (className) {
            case "int":
                return int.class;
            case "byte":
                return byte.class;
            case "short":
                return short.class;
            case "long":
                return long.class;
            case "float":
                return float.class;
            case "double":
                return double.class;
            case "boolean":
                return boolean.class;
            case "char":
                return char.class;
            case "int[]":
                return int[].class;
            case "byte[]":
                return byte[].class;
            case "short[]":
                return short[].class;
            case "long[]":
                return long[].class;
            case "float[]":
                return float[].class;
            case "double[]":
                return double[].class;
            case "boolean[]":
                return boolean[].class;
            case "char[]":
                return char[].class;
        }
        return null;
    }
}

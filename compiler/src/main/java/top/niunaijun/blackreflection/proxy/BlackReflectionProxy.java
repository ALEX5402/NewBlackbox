package top.niunaijun.blackreflection.proxy;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;

import javax.lang.model.element.Modifier;

import top.niunaijun.blackreflection.BlackReflectionInfo;
import top.niunaijun.blackreflection.utils.ClassUtils;

/**
 * Created by sunwanquan on 2020/1/8.
 * * ∧＿∧
 * (`･ω･∥
 * 丶　つ０
 * しーＪ
 * 此处无Bug
 */
public class BlackReflectionProxy {

    private static final ClassName BR = ClassName.get("top.niunaijun.blackreflection", "BlackReflection");
    private final BlackReflectionInfo mReflection;

    private final ClassName mContextInterface;
    private final ClassName mStaticInterface;
    private final String mPackageName;

    public BlackReflectionProxy(String packageName, BlackReflectionInfo reflection) {
        mReflection = reflection;
        mPackageName = packageName;
        String finalClass = mReflection.getClassName()
                .replace(packageName + ".", "")
                .replace(".", "");

        mContextInterface = ClassName.get(ClassUtils.getPackage(finalClass), ClassUtils.getName(finalClass + "Context"));
        mStaticInterface = ClassName.get(ClassUtils.getPackage(finalClass), ClassUtils.getName(finalClass + "Static"));
    }

    public JavaFile generateJavaCode() {
        String finalClass = "BR" + mReflection.getClassName()
                .replace(mPackageName + ".", "")
                .replace(".", "");

        // generaClass
        TypeSpec reflection = TypeSpec.classBuilder(finalClass)
                .addModifiers(Modifier.PUBLIC)
                .addMethod(generaNotCallerMethod(true))
                .addMethod(generaNotCallerMethod(false))
                .addMethod(generaCallerMethod(true))
                .addMethod(generaCallerMethod(false))
                .addMethod(generaIsLoadMethod())
                .build();
        return JavaFile.builder(mPackageName, reflection).build();
    }

    private MethodSpec generaNotCallerMethod(boolean withException) {
        MethodSpec.Builder builder = MethodSpec.methodBuilder("get" + (withException ? "WithException" : ""))
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .returns(mStaticInterface);
        String statement = "return $T.create($T.class, null, $L)";
        builder.addStatement(statement,
                BR,
                mStaticInterface,
                withException
        );
        return builder.build();
    }

    private MethodSpec generaCallerMethod(boolean withException) {
        MethodSpec.Builder builder = MethodSpec.methodBuilder("get" + (withException ? "WithException" : ""))
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .addParameter(ClassName.get("java.lang", "Object"), "caller", Modifier.FINAL)
                .returns(mContextInterface);

        String statement = "return $T.create($T.class, caller, $L)";
        builder.addStatement(statement,
                BR,
                mContextInterface,
                withException
        );
        return builder.build();
    }

    private MethodSpec generaIsLoadMethod() {
        MethodSpec.Builder builder = MethodSpec.methodBuilder("getRealClass")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .returns(ClassName.get(Class.class));

        String statement = "return top.niunaijun.blackreflection.utils.ClassUtil.classReady($T.class)";
        builder.addStatement(statement,
                mContextInterface
        );
        return builder.build();
    }
}

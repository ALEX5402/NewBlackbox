package top.niunaijun.blackreflection;

import com.google.auto.service.AutoService;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.MirroredTypeException;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;

import top.niunaijun.blackreflection.annotation.BClass;
import top.niunaijun.blackreflection.annotation.BConstructor;
import top.niunaijun.blackreflection.annotation.BField;
import top.niunaijun.blackreflection.annotation.BMethod;
import top.niunaijun.blackreflection.annotation.BStaticField;
import top.niunaijun.blackreflection.annotation.BStaticMethod;
import top.niunaijun.blackreflection.annotation.BClassName;
import top.niunaijun.blackreflection.proxy.BlackReflectionInterfaceProxy;
import top.niunaijun.blackreflection.proxy.BlackReflectionProxy;


@AutoService(Processor.class)
public class BlackReflectionProcessor extends AbstractProcessor {

    private Map<String, BlackReflectionProxy> mBlackReflectionProxies;
    private Map<String, BlackReflectionInterfaceProxy> mBlackReflectionInterfaceProxies;
    private Map<String, String> mRealMaps = new HashMap<>();

    private Messager mMessager;
    private Elements mElementUtils; //元素相关的辅助类
    private Filer mFiler;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        mMessager = processingEnv.getMessager();
        mElementUtils = processingEnv.getElementUtils();
        mFiler = processingEnv.getFiler();
        mBlackReflectionProxies = new Hashtable<>();
        mBlackReflectionInterfaceProxies = new Hashtable<>();
        mRealMaps = new Hashtable<>();
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        HashSet<String> supportTypes = new LinkedHashSet<>();
        supportTypes.add(BClass.class.getCanonicalName());
        supportTypes.add(BClassName.class.getCanonicalName());

        supportTypes.add(BField.class.getCanonicalName());
        supportTypes.add(BStaticField.class.getCanonicalName());

        supportTypes.add(BMethod.class.getCanonicalName());
        supportTypes.add(BStaticMethod.class.getCanonicalName());

        supportTypes.add(BConstructor.class.getCanonicalName());
        return supportTypes;
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.RELEASE_8;
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        mBlackReflectionProxies.clear();
        mBlackReflectionInterfaceProxies.clear();
        mRealMaps.clear();

        for (Element element : roundEnv.getElementsAnnotatedWith(BClassName.class)) {
            BClassName annotation = element.getAnnotation(BClassName.class);
            doProcess(element, annotation.value());
        }

        for (Element element : roundEnv.getElementsAnnotatedWith(BClass.class)) {
            BClass annotation = element.getAnnotation(BClass.class);
            String aClass = getClass(annotation).toString();
            doProcess(element, aClass);
        }

        for (Element element : roundEnv.getElementsAnnotatedWith(BStaticMethod.class)) {
            doInterfaceProcess(element, true, false);
        }
        for (Element element : roundEnv.getElementsAnnotatedWith(BMethod.class)) {
            doInterfaceProcess(element, false, false);
        }
        for (Element element : roundEnv.getElementsAnnotatedWith(BStaticField.class)) {
            doInterfaceProcess(element, true, true);
        }
        for (Element element : roundEnv.getElementsAnnotatedWith(BField.class)) {
            doInterfaceProcess(element, false, true);
        }
        for (Element element : roundEnv.getElementsAnnotatedWith(BConstructor.class)) {
            doInterfaceProcess(element, true, false);
        }

        for (BlackReflectionInterfaceProxy value : mBlackReflectionInterfaceProxies.values()) {
            try {
                value.setRealMap(mRealMaps);
                value.generateInterfaceCode().writeTo(mFiler);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        for (BlackReflectionProxy value : mBlackReflectionProxies.values()) {
            try {
                value.generateJavaCode().writeTo(mFiler);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return true;
    }

    private void doInterfaceProcess(Element element, boolean isStatic, boolean isField) {
        String className = element.getEnclosingElement().asType().toString();
        ExecutableElement executableElement = (ExecutableElement) element;
        String packageName = mElementUtils.getPackageOf(executableElement).getQualifiedName().toString();

        BlackReflectionInterfaceInfo interfaceInfo = new BlackReflectionInterfaceInfo();
        interfaceInfo.setExecutableElement(executableElement);
        interfaceInfo.setField(isField);

        BlackReflectionInterfaceProxy reflectionInterfaceProxy = getReflectionInterfaceProxy(packageName,
                className + (isStatic ? "Static" : "Context"),
                className);
        reflectionInterfaceProxy.add(interfaceInfo);
    }

    private void doProcess(Element element, String realClassName) {
        String packageName = mElementUtils.getPackageOf(element).getQualifiedName().toString();
        String className = element.asType().toString();
        BlackReflectionInfo info = new BlackReflectionInfo();
        info.setRealClass(realClassName);
        info.setClassName(className);

        getReflectionProxy(packageName, className, info);

        // 创建两个基本类
        getReflectionInterfaceProxy(packageName, className + "Context",
                className);
        getReflectionInterfaceProxy(packageName, className + "Static",
                className);
        mRealMaps.put(className, realClassName);
    }

    private static TypeMirror getClass(BClass annotation) {
        try {
            annotation.value(); // this should throw
        } catch (MirroredTypeException mte) {
            return mte.getTypeMirror();
        }
        return null; // can this ever happen ??
    }

    public BlackReflectionProxy getReflectionProxy(String packageName, String className, BlackReflectionInfo info) {
        BlackReflectionProxy blackReflectionProxy = mBlackReflectionProxies.get(className);
        if (blackReflectionProxy == null) {
            blackReflectionProxy = new BlackReflectionProxy(packageName, info);
            mBlackReflectionProxies.put(className, blackReflectionProxy);
        }
        return blackReflectionProxy;
    }

    public BlackReflectionInterfaceProxy getReflectionInterfaceProxy(String packageName, String className, String origClassName) {
        BlackReflectionInterfaceProxy blackReflectionProxy = mBlackReflectionInterfaceProxies.get(className);
        if (blackReflectionProxy == null) {
            blackReflectionProxy = new BlackReflectionInterfaceProxy(packageName, className, origClassName);
            mBlackReflectionInterfaceProxies.put(className, blackReflectionProxy);
        }
        return blackReflectionProxy;
    }
}

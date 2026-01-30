package top.niunaijun.blackreflection;


public class BlackReflectionInfo {
    // 反射指向的类
    private String realClass;
    // 当前注解的className
    private String className;

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public String getRealClass() {
        return realClass;
    }

    public void setRealClass(String realClass) {
        this.realClass = realClass;
    }
}

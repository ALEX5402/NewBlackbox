package top.niunaijun.blackreflection;

import javax.lang.model.element.ExecutableElement;


public class BlackReflectionInterfaceInfo {
    
    private ExecutableElement executableElement;
    
    private boolean isField;

    public boolean isField() {
        return isField;
    }

    public void setField(boolean field) {
        isField = field;
    }

    public ExecutableElement getExecutableElement() {
        return executableElement;
    }

    public void setExecutableElement(ExecutableElement executableElement) {
        this.executableElement = executableElement;
    }
}

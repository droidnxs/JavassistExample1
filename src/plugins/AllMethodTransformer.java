/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package plugins;

import java.lang.reflect.Modifier;
import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.NotFoundException;
import javassist.expr.ExprEditor;
import javassist.expr.MethodCall;

/**
 *
 * @author css102082
 */
public class AllMethodTransformer {

    public static void transformMethods(CtClass cl) throws NotFoundException, CannotCompileException, InterruptedException {
        CtMethod[] methods = cl.getDeclaredMethods();
        String className = cl.getName();
        for (CtMethod method : methods) {
            if (!Modifier.isNative(method.getModifiers()) && !method.isEmpty()) {
                String name = className.substring(className.lastIndexOf('.') + 1, className.length());
                String methodName = method.getName();

                if (method.getName().equals(name)) {
                    methodName = "<init>";
                }

                method.insertBefore("MethodStack.push(\"" + className
                        + ":" + methodName + "\");");
                method.insertAfter("MethodStack.pop();");
                /*m.instrument(
                 new ExprEditor() {
                 public void edit(MethodCall m)
                 throws CannotCompileException {
                 System.out.println(m.getClassName() + "." + m.getMethodName() + " " + m.getSignature());
                 }
                 });*/
            }
        }
    }
}

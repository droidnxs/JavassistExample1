package agent;

import collector.GraphiteWriter;
import java.io.IOException;
import java.lang.instrument.*;
import java.security.*;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;
import javassist.*;
import static plugins.JDBC.MySQLTransformer.transformStatementExecuteQuery;
import plugins.JMX.JMXTransformer;
import plugins.Profiler.ThreadProfiler;

public class SimpleTransformer implements ClassFileTransformer {

    private static Instrumentation ins;

    public static String agentArgs = null;
    public static LinkedBlockingQueue methodTraceQueue = new LinkedBlockingQueue();

    public static String mainClassName = "javaagent." + System.getProperty("cssagent.appname");
    public static String graphite_host = System.getProperty("cssagent.graphitehost");

    public SimpleTransformer() throws ClassNotFoundException {
        super();
    }

    public static void premain(String agentArguments, Instrumentation instrumentation) throws ClassNotFoundException, InterruptedException {
        System.out.println("PREMAIN LOADED");
        if (agentArguments != null) {
            agentArgs = agentArguments;
        }
        instrumentation.addTransformer(new SimpleTransformer());
        ins = instrumentation;
        /*Thread dataPoller = new Thread(new QueuePoller());
        dataPoller.setName("CSSAPM-dataPollerThread");
        dataPoller.start();*/
        Thread dataWriter = new Thread(new GraphiteWriter(graphite_host));
        dataWriter.setName("CSSAPM-dataWriterThread");
        dataWriter.start();
    }

    public byte[] transform(ClassLoader loader, String className, Class redefiningClass, ProtectionDomain domain, byte[] bytes) throws IllegalClassFormatException {
        return transformClass(redefiningClass, bytes);
    }

    private byte[] transformClass(Class classToTransform, byte[] b) {
        ClassPool pool = ClassPool.getDefault();
        CtClass cl = null;
        try {
            cl = pool.makeClass(new java.io.ByteArrayInputStream(b));
            if (!cl.isInterface()) {
                CtMethod[] methods = cl.getDeclaredMethods();
                String mname;
                for (CtMethod method : methods) {                
                    mname = method.getLongName();
                    //System.out.println(mname);
                    if (!Modifier.isNative(method.getModifiers())) {
                        if (mname.endsWith("prepareStatement(java.lang.String)")) {
                            new Throwable().printStackTrace(); 
                            transformJDBCPrepareStatement(method);
                        } else if (mname.equals("com.mysql.jdbc.StatementImpl.executeQuery(java.lang.String)")) {
                            new Throwable().printStackTrace(); 
                            transformJDBCExecuteQuery(method);
                        } else if (method.isEmpty() == true || mname.startsWith("com.mysql.jdbc") || mname.startsWith("java") || mname.startsWith("sun") || mname.startsWith("QueuePoller") || mname.contains("main(java.lang.String[]")) {
                            //do nothing
                        } else {
                        }
                    }
                }
                b = cl.toBytecode();
            }
        } catch (IOException | RuntimeException ex) {
            Logger.getLogger(SimpleTransformer.class.getName()).log(Level.SEVERE, null, ex);
        } catch (NotFoundException ex) {
            Logger.getLogger(SimpleTransformer.class.getName()).log(Level.SEVERE, null, ex);
        } catch (CannotCompileException ex) {
            Logger.getLogger(SimpleTransformer.class.getName()).log(Level.SEVERE, null, ex);
        } catch (InterruptedException ex) {
            Logger.getLogger(SimpleTransformer.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            if (cl != null) {
                cl.detach();
            }
        }
        return b;
    }

    private void transformMethod(CtMethod method) throws NotFoundException, CannotCompileException, InterruptedException {
        method.addLocalVariable("startTime", CtClass.longType);
        method.insertBefore("startTime = System.nanoTime();");
        //method.insertAfter("System.out.println(\"" + method.getLongName() + "\" + \" took: \" + ((System.nanoTime() - startTime)/1000000) + \"ms\");");        
        method.insertAfter("agent.SimpleTransformer.methodTraceQueue.add(\"" + method.getLongName() + "\" + \" took: \" + ((System.nanoTime() - startTime)/1000f) + \"ms\");");
        /*method.instrument(
         new ExprEditor() {
         public void edit(MethodCall m)
         throws CannotCompileException {
         System.out.println(m.getClassName() + "." + m.getMethodName() + " " + m.getSignature());
         }
         });*/
    }

    public void transformJDBCPrepareStatement(CtMethod method) throws NotFoundException, CannotCompileException, InterruptedException {
        //System.out.println("INSIDE CLASS: " + method.getLongName());
        //method.insertBefore("SimpleTransformer.methodTraceQueue.add($1);");
        method.addLocalVariable("startTime", CtClass.longType);
        method.insertBefore("startTime = System.nanoTime();");
        method.insertAfter("agent.SimpleTransformer.methodTraceQueue.add(\"" + method.getName() + "\" + \" took: \" + ((System.nanoTime() - startTime)/1000000f) + \"ms\");");
        /*System.out.println("INSIDE TRANSFORMJDBC: " + method.getLongName());
         method.insertBefore("SimpleTransformer.methodTraceQueue.add(Thread.currentThread().getStackTrace());");*/
    }

    public void transformJDBCExecuteQuery(CtMethod method) throws NotFoundException, CannotCompileException, InterruptedException {
        /*method.instrument(new ExprEditor() {
         public void edit(Handler h) throws CannotCompileException {
         h.insertBefore("SimpleTransformer.methodTraceQueue.add($1 + \" at \" + h.where().getDeclaringClass() + \" line-\" + h.getLineNumber());");
         }
         });*/
        method.addLocalVariable("startTime", CtClass.longType);
        //method.insertBefore("System.out.println(\"###\" + $1);");
        method.insertBefore("startTime = System.nanoTime();");
        //method.insertAfter("SimpleTransformer.methodTraceQueue.add($1 + \" took: \" + ((System.nanoTime() - startTime)/1000000f) + \"ms\");");
        method.insertAfter("System.out.println($1 + \" took: \" + ((System.nanoTime() - startTime)/1000000f) + \"ms\");");
    }

    static {
        Thread JMXListener = new Thread(new JMXTransformer());
        JMXListener.setName("CSSAPM-JMXListenerThread");
        JMXListener.start();
        /*Thread Profiler = new Thread(new ThreadProfiler());
         Profiler.setName("CSSAPM-ProfilerThread");
         Profiler.start();*/
    }

}

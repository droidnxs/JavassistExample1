/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package plugins.JDBC;

import java.util.concurrent.LinkedBlockingQueue;
import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.NotFoundException;

/**
 *
 * @author css102082
 */
public class MySQLTransformer {

    public static LinkedBlockingQueue methodTraceQueue = new LinkedBlockingQueue();
    static final private String MYSQL_STATEMENT_CLASS = "com.mysql.jdbc.StatementImpl";

    public static void transformStatementExecuteQuery(ClassPool pool) throws NotFoundException, CannotCompileException, InterruptedException {
        CtMethod method = pool.getMethod(MYSQL_STATEMENT_CLASS, "executeQuery");
        method.addLocalVariable("startTime", CtClass.longType);
        method.insertBefore("startTime = System.nanoTime();");
        //method.insertBefore("Plugins.JDBC.Util.filterAndTransformJDBCTransactions($0.getClass().getName()," + true + ");");
        method.insertAfter("plugins.JDBC.MySQLTransformer.methodTraceQueue.add(\"" + method.getLongName() + "\" + $1 + \" took: \" + ((System.nanoTime() - startTime)/1000000f) + \"ms\");");
    }

}

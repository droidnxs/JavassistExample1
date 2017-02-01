package agent;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import javafx.util.Pair;

public class MethodStack {

    private static Stack<String> stack = new Stack<String>();
    private static Map<Pair<String, String>, Integer> callgraph = new HashMap<Pair<String,String>, Integer>();
    static FileWriter fw; 
    static StringBuffer sb;
    static long threadid = -1L;
    
    static {
        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                try {
                    fw.close();
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                //Sort by number of calls
                List<Pair<String, String>> keys = new ArrayList<Pair<String, String>>();
                keys.addAll(callgraph.keySet());
                Collections.sort(keys, new Comparator<Object>() {
                    public int compare(Object o1, Object o2) {
                        Integer v1 = callgraph.get(o1);
                        Integer v2 = callgraph.get(o2);
                        return v1.compareTo(v2);
                    }
                });
                
                for (Pair<String, String> key : keys) {
                    System.out.println(key + " " + callgraph.get(key));
                }
            }
        });
        File log = new File("D:\\calltrace.txt");
        try {
            fw = new FileWriter(log);
        } catch (Exception e) {
            e.printStackTrace();
        }
        sb = new StringBuffer();
    }

    public static void push(String callname) throws IOException {
        if (threadid == -1)
            threadid = Thread.currentThread().getId();
        
        if (Thread.currentThread().getId() != threadid)
            return;
        
        if (!stack.isEmpty()) {
            Pair<String, String> p = new Pair<String, String>(stack.peek(), callname); 
            if (callgraph.containsKey(p))
                callgraph.put(p, callgraph.get(p) + 1);
            else
                callgraph.put(p, 1);
        }
        sb.setLength(0);
        sb.append(">[").append(stack.size()).append("]");
        sb.append("[").append(Thread.currentThread().getId()).append("]");
        sb.append(callname).append("=").append(System.nanoTime()).append("\n");
        //fw.write(sb.toString());        
        System.out.println("PUSH: " + sb.toString());
        stack.push(callname);
    }

    public static void pop() throws IOException {
        if (threadid == -1)
            threadid = Thread.currentThread().getId();

        if (Thread.currentThread().getId() != threadid)
            return;

        String returnFrom = stack.pop();
        sb.setLength(0);
        sb.append("<[").append(stack.size()).append("]");
        sb.append("[").append(Thread.currentThread().getId()).append("]");
        sb.append(returnFrom).append("=").append(System.nanoTime()).append("\n");
        //fw.write(sb.toString());
        System.out.println("POP: " + sb.toString());
    }
}
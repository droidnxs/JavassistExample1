/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package plugins.JMX;

import static agent.SimpleTransformer.mainClassName;
import collector.GraphiteWriter;
import static collector.GraphiteWriter.currentTimeMillisWithLinefeed;
import static collector.GraphiteWriter.removeWhitespacesAndSpecialChars;
import java.io.IOException;
import java.lang.management.ClassLoadingMXBean;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.RuntimeMXBean;
import java.lang.management.ThreadMXBean;
import java.net.MalformedURLException;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.management.AttributeNotFoundException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanException;
import javax.management.MBeanServer;
import javax.management.MBeanServerConnection;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.ReflectionException;
import javax.management.openmbean.CompositeData;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import org.apache.commons.modeler.Registry;
import static plugins.JMX.JMXRemoteUtils.getCommitted;
import static plugins.JMX.JMXRemoteUtils.getInit;
import static plugins.JMX.JMXRemoteUtils.getMax;
import static plugins.JMX.JMXRemoteUtils.getUsed;

/**
 *
 * @author css102082
 */
public class JMXTransformer implements Runnable {

    static Thread remoteJMXListener;

    static boolean remoteMode = false;

    private static String JMX_REMOTE_HOST = null;
    private static String JMX_REMOTE_PORT = null;
    private static String[] JMX_REMOTE_AUTH = null;

    static JMXServiceURL url = null;
    static JMXConnector jmxc = null;
    static MBeanServerConnection conn = null;

    public static long JMX_COLLECTION_INTERVAL_MS = 5000;
    public static long JMX_LISTENER_START_DELAY_MS = 10000;

    public float BYTE_TO_KB_FACTOR = 1 / 1024f;
    public float BYTE_TO_MB_FACTOR = 1 / 1048576f;

    public static LinkedBlockingQueue jmxMetricsQueue = new LinkedBlockingQueue();
    static ClassLoadingMXBean classLoadingMXBean = ManagementFactory.getClassLoadingMXBean();
    static List<MemoryPoolMXBean> memoryPoolMXBean = ManagementFactory.getMemoryPoolMXBeans();
    static MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
    static ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
    static RuntimeMXBean runtimeMxBean = ManagementFactory.getRuntimeMXBean();
    static List<GarbageCollectorMXBean> gcMXBean = ManagementFactory.getGarbageCollectorMXBeans();
    Map<String, String[]> requiredBeans = new ConcurrentHashMap<>();
    private static StringBuilder metricSet = null;

    @Override
    public void run() {
        try {
            Thread.sleep(JMX_LISTENER_START_DELAY_MS);

            url = new JMXServiceURL("service:jmx:rmi:///jndi/rmi://10.8.157.42:7003/jmxrmi");
            System.out.println("VM Name: " + runtimeMxBean.getName());
            System.out.println("VM Args: " + runtimeMxBean.getInputArguments());
            String metricFooter = null;
            long[][] gcBeans = {{0L, 0L, 0L, 0L}, {0L, 0L, 0L, 0L}};
            int counter = 0;

            while (true) {
                metricSet = new StringBuilder();
                metricFooter = currentTimeMillisWithLinefeed();
                //remote mode detected
                if (remoteMode) {
                    if (JMX_REMOTE_AUTH != null) {
                        Map<String, String[]> env = new Hashtable<>();
                        env.put(JMXConnector.CREDENTIALS, JMX_REMOTE_AUTH);
                        jmxc = JMXConnectorFactory.connect(url, env);
                    } else {
                        jmxc = JMXConnectorFactory.connect(url, null);
                    }
                    if (jmxc != null) {
                        conn = jmxc.getMBeanServerConnection();
                    }
                    /*Set dsNames = conn.queryNames(new ObjectName("Catalina:type=DataSource,*"), null);
                     System.out.println(dsNames.size());
                     Iterator it = dsNames.iterator();
                     while(it.hasNext())
                     {
                     ObjectName objectname = (ObjectName) it.next();
                     System.out.println(objectname.toString() + " NUMACTIVE: " + conn.getAttribute(objectname, "numActive"));
                     }*/

                    Set dsNames = conn.queryNames(new ObjectName(ManagementFactory.MEMORY_MXBEAN_NAME), null);
                    //System.out.println(dsNames.size());
                    Iterator it = dsNames.iterator();
                    while (it.hasNext()) {
                        ObjectName objectname = (ObjectName) it.next();
                        CompositeData HeapMemoryUsage = (CompositeData) conn.getAttribute(objectname, "HeapMemoryUsage");
                        CompositeData NonHeapMemoryUsage = (CompositeData) conn.getAttribute(objectname, "NonHeapMemoryUsage");
                        long init = (long) HeapMemoryUsage.get("init");
                        //System.out.println(init);

                        metricSet
                                .append(mainClassName).append(".JMX.Memory.Heap.Usage.Committed ").append(getCommitted(HeapMemoryUsage) * BYTE_TO_KB_FACTOR).append(" ").append(metricFooter)
                                .append(mainClassName).append(".JMX.Memory.Heap.Usage.Used ").append(getUsed(HeapMemoryUsage) * BYTE_TO_KB_FACTOR).append(" ").append(metricFooter)
                                .append(mainClassName).append(".JMX.Memory.Heap.Usage.Init ").append(getInit(HeapMemoryUsage) * BYTE_TO_KB_FACTOR).append(" ").append(metricFooter)
                                .append(mainClassName).append(".JMX.Memory.Heap.Usage.Max ").append(getMax(HeapMemoryUsage) * BYTE_TO_KB_FACTOR).append(" ").append(metricFooter)
                                .append(mainClassName).append(".JMX.Memory.Non_heap.Usage.Committed ").append(getCommitted(NonHeapMemoryUsage) * BYTE_TO_KB_FACTOR).append(" ").append(metricFooter)
                                .append(mainClassName).append(".JMX.Memory.Non_heap.Usage.Used ").append(getUsed(NonHeapMemoryUsage) * BYTE_TO_KB_FACTOR).append(" ").append(metricFooter)
                                .append(mainClassName).append(".JMX.Memory.Non_heap.Usage.Init ").append(getInit(NonHeapMemoryUsage) * BYTE_TO_KB_FACTOR).append(" ").append(metricFooter)
                                .append(mainClassName).append(".JMX.Memory.Non_heap.Usage.Max ").append(getMax(NonHeapMemoryUsage) * BYTE_TO_KB_FACTOR).append(" ").append(metricFooter);
                        */System.out.println(objectname.toString() + " ObjectPendingFinalizationCount: " + conn.getAttribute(objectname, "ObjectPendingFinalizationCount"));

                        jmxMetricsQueue.add(metricSet.toString());
                    }

                    //System.out.println(conn.getAttribute(new ObjectName("Catalina:type=ThreadPool,*"), "sessionTimeout"));
                    //agent mode detected
                } else {
                    MBeanServer server = new Registry().getMBeanServer();
                    Set dsNames = server.queryNames(new ObjectName("Catalina:type=DataSource,*"), null);
                    String objName, dataSourceContext, dataSourceName;
                    for (Iterator it = dsNames.iterator(); it.hasNext();) {
                        ObjectName objectname = (ObjectName) it.next();
                        //if(!objectname.toString().contains("context=/," || !))
                        objName = objectname.toString();
                        dataSourceContext = objName.substring(objName.indexOf("context=/") + 8, objName.indexOf(",host")).replace("/", "_");
                        dataSourceName = objName.substring(objName.indexOf("name=\"") + 6, objName.lastIndexOf("\""));
                        StringBuilder append = metricSet.append(mainClassName).append(".JMX.DataSource." + dataSourceContext + "." + dataSourceName + ".numActive ").append(server.getAttribute(objectname, "numActive")).append(" ").append(metricFooter)
                                .append(mainClassName).append(".JMX.DataSource." + dataSourceContext + "." + dataSourceName + ".numIdle ").append(server.getAttribute(objectname, "numIdle")).append(" ").append(metricFooter);
                        /*System.out.println("Context: " + dataSourceContext + " | Name: " + dataSourceName);
                         System.out.println("MBEAN_VALUE: " + server.getAttribute(objectname, "numActive"));*/
                    }
                    /*Set dsNames = server.queryNames(new ObjectName("java.lang:type=MemoryPool,*"), null);
                     for (Iterator it = dsNames.iterator(); it.hasNext();) {
                     ObjectName objectname = (ObjectName) it.next();
                     System.out.println(objectname.toString());
                     System.out.println(server.getAttribute(objectname, "Usage") instanceof CompositeData);
                     CompositeData usage = (CompositeData) server.getAttribute(objectname, "Usage");
                     long init = (long) usage.get("init");
                     System.out.println(init);
                     }*/
                    metricSet
                            .append(mainClassName).append(".JMX.Memory.Heap.Usage.Committed ").append(memoryMXBean.getHeapMemoryUsage().getCommitted() * BYTE_TO_KB_FACTOR).append(" ").append(metricFooter)
                            .append(mainClassName).append(".JMX.Memory.Heap.Usage.Used ").append(memoryMXBean.getHeapMemoryUsage().getUsed() * BYTE_TO_KB_FACTOR).append(" ").append(metricFooter)
                            .append(mainClassName).append(".JMX.Memory.Heap.Usage.Init ").append(memoryMXBean.getHeapMemoryUsage().getInit() * BYTE_TO_KB_FACTOR).append(" ").append(metricFooter)
                            .append(mainClassName).append(".JMX.Memory.Heap.Usage.Max ").append(memoryMXBean.getHeapMemoryUsage().getMax() * BYTE_TO_KB_FACTOR).append(" ").append(metricFooter)
                            .append(mainClassName).append(".JMX.Memory.Non_heap.Usage.Committed ").append(memoryMXBean.getNonHeapMemoryUsage().getCommitted() * BYTE_TO_KB_FACTOR).append(" ").append(metricFooter)
                            .append(mainClassName).append(".JMX.Memory.Non_heap.Usage.Used ").append(memoryMXBean.getNonHeapMemoryUsage().getUsed() * BYTE_TO_KB_FACTOR).append(" ").append(metricFooter)
                            .append(mainClassName).append(".JMX.Memory.Non_heap.Usage.Init ").append(memoryMXBean.getNonHeapMemoryUsage().getInit() * BYTE_TO_KB_FACTOR).append(" ").append(metricFooter)
                            .append(mainClassName).append(".JMX.Memory.Non_heap.Usage.Max ").append(memoryMXBean.getNonHeapMemoryUsage().getMax() * BYTE_TO_KB_FACTOR).append(" ").append(metricFooter);

                    //jmxMetricsQueue.add(metricSet.toString());
                    for (MemoryPoolMXBean m : memoryPoolMXBean) {
                        metricSet
                                .append(mainClassName).append(".JMX.MemoryPools.").append(removeWhitespacesAndSpecialChars(m.getType().toString())).append(".").append(removeWhitespacesAndSpecialChars(m.getName())).append(".Committed").append(" ").append(m.getUsage().getCommitted() * BYTE_TO_KB_FACTOR).append(" ").append(metricFooter)
                                .append(mainClassName).append(".JMX.MemoryPools.").append(removeWhitespacesAndSpecialChars(m.getType().toString())).append(".").append(removeWhitespacesAndSpecialChars(m.getName())).append(".Used").append(" ").append(m.getUsage().getUsed() * BYTE_TO_KB_FACTOR).append(" ").append(metricFooter)
                                .append(mainClassName).append(".JMX.MemoryPools.").append(removeWhitespacesAndSpecialChars(m.getType().toString())).append(".").append(removeWhitespacesAndSpecialChars(m.getName())).append(".Init").append(" ").append(m.getUsage().getInit() * BYTE_TO_KB_FACTOR).append(" ").append(metricFooter)
                                .append(mainClassName).append(".JMX.MemoryPools.").append(removeWhitespacesAndSpecialChars(m.getType().toString())).append(".").append(removeWhitespacesAndSpecialChars(m.getName())).append(".Max").append(" ").append(m.getUsage().getMax() * BYTE_TO_KB_FACTOR).append(" ").append(metricFooter);

                    }
                    //for (GarbageCollectorMXBean g : gcMXBean) {
                    for (int i = 0; i < gcMXBean.size(); i++) {
                        gcBeans[i][0] = gcMXBean.get(i).getCollectionCount();
                        gcBeans[i][2] = gcMXBean.get(i).getCollectionTime();
                        /*System.out.println("Count: " + (gcBeans[i][0] - gcBeans[i][1]));
                         System.out.println("Time: " + (gcBeans[i][2] - gcBeans[i][3]));*/
                        if (counter > 0) {
                            metricSet
                                    .append(mainClassName).append(".JMX.GC.").append(removeWhitespacesAndSpecialChars(gcMXBean.get(i).getName())).append(".CollectionCount ").append(gcBeans[i][0] - gcBeans[i][1]).append(" ").append(metricFooter)
                                    .append(mainClassName).append(".JMX.GC.").append(removeWhitespacesAndSpecialChars(gcMXBean.get(i).getName())).append(".CollectionTime ").append(gcBeans[i][2] - gcBeans[i][3]).append(" ").append(metricFooter);
                        }
                        gcBeans[i][1] = gcBeans[i][0];
                        gcBeans[i][3] = gcBeans[i][2];

                        /* metricSet
                         .append(mainClassName).append(".JMX.GC.").append(removeWhitespacesAndSpecialChars(g.getName())).append(".CollectionCount ").append(g.getCollectionCount()).append(" ").append(metricFooter)
                         .append(mainClassName).append(".JMX.GC.").append(removeWhitespacesAndSpecialChars(g.getName())).append(".CollectionTime ").append(g.getCollectionTime()).append(" ").append(metricFooter);*/
                    }
                    metricSet
                            .append(mainClassName).append(".JMX.Threads.ThreadCount ").append(threadMXBean.getThreadCount()).append(" ").append(metricFooter)
                            .append(mainClassName).append(".JMX.Threads.DaemonThreadCount ").append(threadMXBean.getDaemonThreadCount()).append(" ").append(metricFooter)
                            .append(mainClassName).append(".JMX.Threads.TotalStartedCount ").append(threadMXBean.getTotalStartedThreadCount()).append(" ").append(metricFooter);

                    //System.out.println(metricSet.toString());
                    jmxMetricsQueue.add(metricSet.toString());
                }
                counter++;
                Thread.sleep(JMX_COLLECTION_INTERVAL_MS);
            }
        } catch (Exception ex) {
            Logger.getLogger(JMXTransformer.class.getName()).log(Level.SEVERE, null, ex);
            System.out.println(System.currentTimeMillis() / 1000 + "\n");
            ex.printStackTrace();
        }
    }

    public static void main(String args[]) throws MalformedURLException, IOException, MBeanException, AttributeNotFoundException, InstanceNotFoundException, ReflectionException, MalformedObjectNameException {
        remoteMode = true;
        mainClassName = 
        if (args.length > 0) {
            JMX_REMOTE_HOST = args[0];
            JMX_REMOTE_PORT = args[1];
            if (args.length > 2) {
                JMX_REMOTE_AUTH = args[2].split(",");
            }
            remoteJMXListener = new Thread(new JMXTransformer());
            remoteJMXListener.setName("CSSAPM-JMXListenerThread");
            remoteJMXListener.start();
        } else {
            System.out.println("ERROR: No arguments provided");
            System.out.println("Usage: java -cp .;./myAgent.jar plugins.JMX.JMXTransformer <REMOTE_IP> <PORT>");
        }
    }
}

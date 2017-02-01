/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package plugins.Profiler;

import com.sun.management.OperatingSystemMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author css102082
 */
public class ThreadProfiler implements Runnable {

    OperatingSystemMXBean osMXBean = ManagementFactory.getPlatformMXBean(OperatingSystemMXBean.class);
    ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();

    @Override
    public void run() {
        try {
            Thread.sleep(10000);
            int numCores = osMXBean.getAvailableProcessors();
            long[] threadIds = threadBean.getAllThreadIds();
            ThreadInfo[] threadInfos = threadBean.getThreadInfo(threadIds, 50);
            //System.out.println("Enabled: " + threadBean.isThreadCpuTimeEnabled() + " Supported: " + threadBean.isThreadCpuTimeSupported());
            Map<Long, Long> threadCPUStates_bef = new HashMap<>();
            Map<Long, Long> threadCPUStates_aft = new HashMap<>();
            Iterator iter = null;
            Entry entry = null;
            Object key = null, value = null;
            //System.out.println("BEFORE");
            for (int i = 0; i < threadIds.length; i++) {
                threadCPUStates_bef.put(threadIds[i], threadBean.getThreadCpuTime(threadIds[i]));
                //System.out.println(threadIds[i] + "," + threadBean.getThreadCpuTime(threadIds[i]) + "\n" + threadInfos[i]);
            }

            while (true) {
                threadIds = threadBean.getAllThreadIds();
                threadInfos = threadBean.getThreadInfo(threadIds, 50);
                //System.out.println("AFTER");
                //System.out.println(osMXBean.getProcessCpuLoad());
                for (int i = 0; i < threadIds.length; i++) {
                    threadCPUStates_bef.put(threadIds[i], threadBean.getThreadCpuTime(threadIds[i]));
                    //System.out.println(threadIds[i] + "," + threadBean.getThreadCpuTime(threadIds[i]) + "\n" + threadInfos[i]);
                }
                for (Long id : threadIds) {
                    threadCPUStates_aft.put(id, threadBean.getThreadCpuTime(id));
                }
                /*iter = threadCPUStates_aft.entrySet().iterator();
                 while (iter.hasNext()) {
                 entry = (Entry) iter.next();
                 key = entry.getKey();
                 value = entry.getValue();
                 if ((long) value > 100) {

                 }
                 System.out.println(key + " " + ((long) value - threadCPUStates_bef.get(key)));
                 }*/
                threadCPUStates_bef = threadCPUStates_aft;
                Thread.sleep(1000);
            }
        } catch (InterruptedException ex) {
            Logger.getLogger(ThreadProfiler.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package plugins.JMX;

import javax.management.MBeanServerConnection;
import javax.management.openmbean.CompositeData;

/**
 *
 * @author css102082
 */
public class JMXRemoteUtils {
    public static long getCommitted(CompositeData data)
    {
        return (long)data.get("committed");
    }
    
    public static long getInit(CompositeData data)
    {
        return (long)data.get("init");
    }
    
    public static long getMax(CompositeData data)
    {
        return (long)data.get("max");
    }
    
    public static long getUsed(CompositeData data)
    {
        return (long)data.get("used");
    }
}

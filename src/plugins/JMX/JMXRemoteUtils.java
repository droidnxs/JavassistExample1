/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package plugins.JMX;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import javax.management.MBeanServerConnection;
import javax.management.openmbean.CompositeData;

/**
 *
 * @author css102082
 */
public class JMXRemoteUtils {
    
    static SimpleDateFormat sdf = null;
    
    public static SimpleDateFormat getDateFormatter()
    {
        if(sdf == null)
        {
            sdf = new SimpleDateFormat("dd/MM/YYYY hh:mm:ss");
        }
        return sdf;
    }
    
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
    
    public static StringBuilder getLineFormat(StringBuilder srcString, String appname, String metric, long value)
    {            
        return srcString.append(appname).append(metric).append(" ").append(value).append(" ").append(currentTimeMillisWithLinefeed());
    }
    
    public static String getCSVFormat(List<String> values, boolean isHeader)
    {
        String line = null;
        if(!isHeader)
        {
            line = currentDateTime() + ",";
        }
        for(int i=0; i<values.size(); i++)
        {
            line = line + values.get(i);
            if(i!=values.size() - 1) {
                line = line + ",";
            }          
        }
        return line;
    }
    
    public static String currentDateTime() {
        return getDateFormatter().format(new Date());
    }
    
    public static String currentTimeMillisWithLinefeed() {
        return (System.currentTimeMillis() / 1000) + "\n";
    }
}

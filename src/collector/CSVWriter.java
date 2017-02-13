/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package collector;

import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import plugins.JMX.JMXTransformer;

/**
 *
 * @author css102082
 */
public class CSVWriter implements Runnable {

    static Socket s = null;
    static Writer writer = null;

    public void run() {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("dd_MM_YYYY_hh_mm_ss");
            String filename = JMXTransformer.appName + "_" + sdf.format(new Date()) + ".csv" ;
            File f = new File(filename);
            if(!f.exists())
            {
                f.createNewFile();
            }
            String line = null;
            while ((line = (JMXTransformer.csvMetricsQueue.take() + "\n")) != null) {
                //System.out.println("CSVWRITER: " + line);
            }
        } catch (InterruptedException ex) {
            Logger.getLogger(GraphiteWriter.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(CSVWriter.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public static String removeWhitespacesAndSpecialChars(String source) {
        return source.replaceAll(" ", "_").replaceAll("\\.", "_").replaceAll("-", "_");
    }

    public static String currentTimeMillisWithLinefeed() {
        return (System.currentTimeMillis() / 1000) + "\n";
    }
}

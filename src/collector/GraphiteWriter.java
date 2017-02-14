/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package collector;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;
import plugins.JMX.JMXTransformer;

/**
 *
 * @author css102082
 */
public class GraphiteWriter implements Runnable {
    String graphite_host = "10.8.157.30";
    static Socket s = null;
    static Writer writer = null;

    public GraphiteWriter(String graphite_host)
    {
        this.graphite_host = graphite_host;
    }
    
    public void run() {
        try {
            s = new Socket(this.graphite_host, 2003);
            Writer writer = new OutputStreamWriter(s.getOutputStream());
            String line = null;
            while ((line = (JMXTransformer.jmxMetricsQueue.take() + "\n")) != null) {
                writer.write(line);
                writer.flush();
                //System.out.println("GRAPHITE WRITER WROTE: " + line);
            }
        } catch (IOException ex) {
        } catch (InterruptedException ex) {
            Logger.getLogger(GraphiteWriter.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public static void writeData(String metric, Object value, long timestamp) throws IOException {
        if (writer != null) {
            writer.write(metric + " " + value + " " + timestamp);
            writer.flush();
        }
    }

    public static String removeWhitespacesAndSpecialChars(String source) {
        return source.replaceAll(" ", "_").replaceAll("\\.", "_").replaceAll("-", "_");
    }

    public static String currentTimeMillisWithLinefeed() {
        return (System.currentTimeMillis() / 1000) + "\n";
    }
}

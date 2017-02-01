package agent;


import plugins.JDBC.MySQLTransformer;
import java.util.Queue;
import java.util.logging.Level;
import java.util.logging.Logger;

public class QueuePoller implements Runnable {

    public void run() {
        while (true) {
            try {
                /*try {
                if (SimpleTransformer.methodTraceQueue.peek() != null) {
                poll(SimpleTransformer.methodTraceQueue);
                } else {
                Thread.sleep(1000);
                }
                } catch (InterruptedException ex) {
                Logger.getLogger(QueuePoller.class.getName()).log(Level.SEVERE, null, ex);
                }*/
                System.out.println(MySQLTransformer.methodTraceQueue.take());
            } catch (InterruptedException ex) {
                Logger.getLogger(QueuePoller.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    public static void poll(Queue queue) throws InterruptedException {
        //System.out.println(MySQLTransformer.methodTraceQueue.poll());
    }
}

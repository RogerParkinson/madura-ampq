/**
 * 
 */
package nz.co.senanque.madura.ampq;

import java.util.concurrent.CountDownLatch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * @author Roger Parkinson
 *
 */
@Component
public class Receiver {

    private static final Logger log = LoggerFactory.getLogger(Receiver.class);
    
    private CountDownLatch latch = new CountDownLatch(1);

    @AMPQReceiver(queueName="transaction-queue")
    public void receiveMessage(String message) {
        log.debug("Received <{}>", message);
        latch.countDown();
    }

    public CountDownLatch getLatch() {
        return latch;
    }

}
/**
 * 
 */
package nz.co.senanque.madura.ampq;

import java.util.Collection;

import javax.annotation.PostConstruct;

import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

/**
 * @author Roger Parkinson
 *
 */
public class QueueController implements ApplicationContextAware {

	private Collection<SimpleMessageListenerContainer> rabbitContainers;
	private ApplicationContext applicationContext;

	@PostConstruct
	public void init() {
		rabbitContainers =  applicationContext.getBeansOfType(SimpleMessageListenerContainer.class).values();
	}
	public void startAllQueues() {
		for (SimpleMessageListenerContainer smlc: rabbitContainers) {
			if (!smlc.isRunning()) {
				smlc.start();
			}
		}		
	}
	public void stopAllQueues() {
		for (SimpleMessageListenerContainer smlc: rabbitContainers) {
			if (!smlc.isRunning()) {
				smlc.stop();
			}
		}		
	}
	public void setApplicationContext(ApplicationContext applicationContext)
			throws BeansException {
		this.applicationContext = applicationContext;
		
	}

}

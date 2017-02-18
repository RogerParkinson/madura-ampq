package nz.co.senanque.madura.ampq;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Configuration
@EnableAMPQ
@ComponentScan("nz.co.senanque.madura.ampq")
@PropertySource(value = { "classpath:test.properties" }, ignoreResourceNotFound = true)
public class QueueHandlerSpringConfig {

	 @Value("${rabbitmq.host:localhost}")
	 public String rabbitMQHost;
	 @Value("${rabbitmq.vhost:harmoney}")
	 public String rabbitMQvHost;
	 @Value("${rabbitmq.port:31761}")
	 public int rabbitmqPort;
	 @Value("${rabbitmq.username:harmoney}")
	 public String rabbitmqUsername;
	 @Value("${rabbitmq.password:harmoney}")
	 public String rabbitmqPassword;
	 @Value("${rabbitmq.exchange:transaction-exchange}")
	 public String exchangeName;
	 @Value("${rabbitmq.queue:transaction-queue}")
	 public String queueName;

	/**
	 * Bean is our direct connection to RabbitMQ
	 * @return CachingConnectionFactory
	 */
	@Bean(destroyMethod = "destroy")
	public ConnectionFactory rabbitConnectionFactory() {
	    CachingConnectionFactory factory = new CachingConnectionFactory(rabbitMQHost);
	    factory.setUsername(rabbitmqUsername);
	    factory.setPassword(rabbitmqPassword);
	    factory.setVirtualHost(rabbitMQvHost);

	    return factory;
	}
// Only need this if we want to auto-create the queue and/or exchange
//	@Bean
//	RabbitAdmin rabbitAdmin(ConnectionFactory connectionFactory) {
//		return new RabbitAdmin(connectionFactory);
//	}
	@Bean
	Queue queue() {
		return new Queue(queueName, true);
	}

	@Bean
	TopicExchange exchange() {
		return new TopicExchange(exchangeName);
	}

	@Bean
	Binding binding(Queue queue, TopicExchange exchange) {
		return BindingBuilder.bind(queue).to(exchange).with(queueName);
	}

// The following are added by annotations

//	@Bean
//	SimpleMessageListenerContainer container(
//			ConnectionFactory connectionFactory,
//			MessageListenerAdapter listenerAdapter) {
//		SimpleMessageListenerContainer container = new SimpleMessageListenerContainer();
//		container.setConnectionFactory(connectionFactory);
//		container.setQueueNames(queueName);
//		container.setMessageListener(listenerAdapter);
//		return container;
//	}
//
//	@Bean
//	MessageListenerAdapter listenerAdapter(Receiver receiver) {
//		return new MessageListenerAdapter(receiver, "receiveMessage");
//	}

	@Bean
	RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
		RabbitTemplate ret = new RabbitTemplate(connectionFactory);
		ret.setExchange(exchangeName);
		return ret;
	}
}

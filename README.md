madura-ampq
===

[![Maven Central](https://maven-badges.herokuapp.com/maven-central/nz.co.senanque/madura-ampq/badge.svg)](http://mvnrepository.com/artifact/nz.co.senanque/madura-ampq)
[![build_status](https://travis-ci.org/phillip-kruger/apiee.svg?branch=master)](https://travis-ci.org/phillip-kruger/apiee)

Background
----
[AMPQ](https://en.wikipedia.org/wiki/Advanced_Message_Queuing_Protocol) is a middleware protocol supported by [Spring Framework](https://spring.io/understanding/AMQP) in a similar way to JMS. Except that configuring an AMPQ application is more complex than JMS. This project aims to simplify it a little. The specific AMPQ engine used here is RabbitMQ but you can configure it to use others.

Example Configuration
---
This is using Spring Config to configure an application that uses AMPQ:

```
@Configuration
@EnableAMPQ
@ComponentScan("nz.co.senanque.madura.ampq")
public class QueueHandlerSpringConfig {

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
	@Bean
	Queue queue() {
		return new Queue("transaction-queue", true);
	}

	@Bean
	TopicExchange exchange() {
		return new TopicExchange(exchangeName);
	}

	@Bean
	Binding binding(Queue queue, TopicExchange exchange) {
		return BindingBuilder.bind(queue).to(exchange).with("transaction-queue");
	}

// The following are added by annotations

//	@Bean
//	SimpleMessageListenerContainer container(
//			ConnectionFactory connectionFactory,
//			MessageListenerAdapter listenerAdapter) {
//		SimpleMessageListenerContainer container = new SimpleMessageListenerContainer();
//		container.setConnectionFactory(connectionFactory);
//		container.setQueueNames("transaction-queue");
//		container.setMessageListener(listenerAdapter);
//		return container;
//	}
//
//	@Bean
//	MessageListenerAdapter listenerAdapter(Receiver receiver) {
//		return new MessageListenerAdapter(receiver, "receiveMessage");
//	}

}
```

The commented out section is normally needed but with the madura-ampq it is generated from the annotations. When you have just one receiver, as in this example, there is not a lot of value, but with multiple receivers you would have to replicate the commented out section for each one and the value becomes apparent. You also get to put the information where it is most useful, in the receiver bean. You only need to annotate the class with @EnableAMPQ and ensure your receiver beans are on the component scan.

This is what the receiver bean looks like:

```
@Component
public class Receiver {
  
	@AMPQReceiver(queueName="transaction-queue")
    public void receiveMessage(String message) {
        System.out.println("Received "+ message);
    }
}
```

It has an annotation on the receiver method 'receiveMessage' which refers to the queue to listen to. You can have multiple methods on one class or spread them over multiple classes, as long as they are picked up by the component scan. And you can have as many as you like. You do have to make sure your queue is defined in in the configuration file and connected to the exchange etc. AMPQ has a lot of options in that area you need to work through.

Advanced
--
The examples here assume RabbitMQ, that is because the ConnectionFactory is the RabbitMQ one etc. The annotations will generate RabbitMQ support classes, specifically org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer, org.springframework.amqp.rabbit.listener.adapter.MessageListenerAdapter and org.springframework.amqp.rabbit.connection.ConnectionFactory. But you can override these by specifying alternate ones in the @EnableAMPQ eg

```
@EnableAMPQ(connectionFactory="abc",listenerContainer="def",listenerAdapter="ghi")
```

Just replace the 'abc' etc with the class names you want.

You can also add the listenerContainer and/or listenerAdapter to the @AMPQReceiver annotation which will override the classes for just that receiver. Though be aware that if your replacement classes need extra properties injected then they won't work (though let us know what you need and we'll try and add it).

The @AMPQReceiver also takes `autoStartup="true"` which determines it's startup behaviour. To start/stop queues manually use the QueueController bean.


Release notes:
--
0.0.2 allow no registered bean even though we find one annotated with @AMPQReceiver. This is for cases using @Profile to eliminate some beans.

0.0.1 Initial version


package nz.co.senanque.madura.ampq;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import org.springframework.context.annotation.Import;

@Retention(value=java.lang.annotation.RetentionPolicy.RUNTIME)
@Target(value={java.lang.annotation.ElementType.TYPE})
@Documented
@Import({AMPQRegistrar.class})
public @interface EnableAMPQ {

	String connectionFactory() default "org.springframework.amqp.rabbit.connection.ConnectionFactory";
	String listenerContainer() default "org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer";
	String listenerAdapter() default "org.springframework.amqp.rabbit.listener.adapter.MessageListenerAdapter";

}

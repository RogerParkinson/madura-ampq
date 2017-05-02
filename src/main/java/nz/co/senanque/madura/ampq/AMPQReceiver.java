package nz.co.senanque.madura.ampq;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author Roger Parkinson
 *
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface AMPQReceiver {
	
	String queueName();
	String listenerContainer() default "";
	String listenerAdapter() default "";
	String autoStartup() default "true";
	
}

/*******************************************************************************
 * Copyright (c)2015 Prometheus Consulting
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package nz.co.senanque.madura.ampq;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.rabbit.listener.adapter.MessageListenerAdapter;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.MethodMetadata;
import org.springframework.core.type.classreading.CachingMetadataReaderFactory;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;

/**
 * @author Roger Parkinson
 *
 */
public class AMPQRegistrar implements ImportBeanDefinitionRegistrar/*,ResourceLoaderAware, BeanClassLoaderAware,BeanFactoryAware,EnvironmentAware*/  {
	
	private Logger m_logger = LoggerFactory.getLogger(this.getClass());
	private MetadataReaderFactory m_metadataReaderFactory = new CachingMetadataReaderFactory();
	static final String DEFAULT_RESOURCE_PATTERN = "**/*.class";
	private String resourcePattern = DEFAULT_RESOURCE_PATTERN;
	private ResourcePatternResolver resourcePatternResolver = new PathMatchingResourcePatternResolver();

	protected String resolveBasePackage(String basePackage) {
		return ClassUtils.convertClassNameToResourcePath(basePackage);
	}
	
	public void registerBeanDefinitions(
			AnnotationMetadata importingClassMetadata,
			BeanDefinitionRegistry registry) {
		
		Map<BeanDefinition,String> definitionMap = getBeanNames(registry);
		MultiValueMap<String, Object> importedAttributes = importingClassMetadata.getAllAnnotationAttributes("nz.co.senanque.madura.ampq.EnableAMPQ");
		String connectionFactoryClassName = getImportedAttributeValue(importedAttributes,"connectionFactory");
		String connectionFactoryBeanName = getBeanName(definitionMap,connectionFactoryClassName,registry);

		Map<String, Object> attributes = importingClassMetadata.getAnnotationAttributes("org.springframework.context.annotation.ComponentScan");
		String[] packageSearchPaths = (String[])attributes.get("basePackages");
		for (String basePackage : packageSearchPaths) {
			m_logger.debug("{}",basePackage);
			String packageSearchPath = ResourcePatternResolver.CLASSPATH_ALL_URL_PREFIX +
					resolveBasePackage(basePackage) + "/" + this.resourcePattern;
			try {
				Resource[] resources = this.resourcePatternResolver.getResources(packageSearchPath);
				for (Resource resource : resources) {
					if (resource.isReadable()) {
						MetadataReader metadataReader = m_metadataReaderFactory.getMetadataReader(resource);
						Set<MethodMetadata> methods = metadataReader.getAnnotationMetadata().getAnnotatedMethods("nz.co.senanque.madura.ampq.AMPQReceiver");
						for (MethodMetadata mm : methods) {
							String methodName = mm.getMethodName();
							String className = mm.getDeclaringClassName(); //TODO no package and we need one
							className = metadataReader.getClassMetadata().getClassName();
							String beanName = getBeanName(definitionMap,className,registry);
							MultiValueMap<String, Object> a = mm.getAllAnnotationAttributes("nz.co.senanque.madura.ampq.AMPQReceiver");
							List<Object> queueNameList = a.get("queueName");
							String queueName = (String)queueNameList.get(0);
							String listenerAdapterClassName = getImportedAttributeValue(mm.getAllAnnotationAttributes("nz.co.senanque.madura.ampq.AMPQReceiver"),"listenerAdapter");
							if (StringUtils.isEmpty(listenerAdapterClassName)) {
								listenerAdapterClassName = getImportedAttributeValue(importedAttributes,"listenerAdapter");
							}
							BeanDefinition messageListenerAdapterDefinition = createMessageListenerAdapter(beanName, methodName,listenerAdapterClassName);
							String messageListenerAdapterBeanName = BeanDefinitionReaderUtils.generateBeanName(messageListenerAdapterDefinition,registry,false);
							BeanDefinitionHolder messageListenerAdapterHolder = new BeanDefinitionHolder(messageListenerAdapterDefinition, messageListenerAdapterBeanName);
							m_logger.debug("Registering bean name: {} definition: {}",messageListenerAdapterBeanName,messageListenerAdapterDefinition);
							BeanDefinitionReaderUtils.registerBeanDefinition(messageListenerAdapterHolder, registry);
							
							// create the message listener container
							String listenerContainerClassName = getImportedAttributeValue(mm.getAllAnnotationAttributes("nz.co.senanque.madura.ampq.AMPQReceiver"),"listenerContainer");
							if (StringUtils.isEmpty(listenerContainerClassName)) {
								listenerContainerClassName = getImportedAttributeValue(importedAttributes,"listenerContainer");
							}
							BeanDefinition messageListenerContainerDefinition = createSimpleMessageListenerContainer(
									messageListenerAdapterBeanName, connectionFactoryBeanName, queueName,listenerContainerClassName);
							String messageListenerContainerBeanName = BeanDefinitionReaderUtils.generateBeanName(messageListenerContainerDefinition,registry,false);
							BeanDefinitionHolder messageListenerContainerHolder = new BeanDefinitionHolder(messageListenerContainerDefinition, messageListenerContainerBeanName);
							m_logger.debug("Registering bean name: {} definition: {}",beanName,messageListenerContainerDefinition);
							BeanDefinitionReaderUtils.registerBeanDefinition(messageListenerContainerHolder, registry);
						}
					}
				}
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
	}
	
	private String getImportedAttributeValue(
			MultiValueMap<String, Object> importedAttributes, String string) {
		return (String)importedAttributes.getFirst(string);
	}

	private BeanDefinition createMessageListenerAdapter(String beanName, String methodName, String listenerAdapterClassName) {
		Class<?> listenerAdapterClass;
		try {
			listenerAdapterClass = Class.forName(listenerAdapterClassName);
		} catch (ClassNotFoundException e) {
			throw new RuntimeException(e);
		}
		BeanDefinitionBuilder beanDefinitionBuilder = BeanDefinitionBuilder.genericBeanDefinition(listenerAdapterClass);
		beanDefinitionBuilder.addConstructorArgReference(beanName);
		beanDefinitionBuilder.addConstructorArgValue(methodName);
		BeanDefinition beanDefinition = beanDefinitionBuilder.getBeanDefinition();
		return beanDefinition;
	}

	private BeanDefinition createSimpleMessageListenerContainer(
			String messageListenerAdapterBeanName, 
			String connectionFactoryBeanName, 
			String queueName, String listenerContainerClassName) {
		Class<?> listenerContainerClass;
		try {
			listenerContainerClass = Class.forName(listenerContainerClassName);
		} catch (ClassNotFoundException e) {
			throw new RuntimeException(e);
		}
		BeanDefinitionBuilder beanDefinitionBuilder = BeanDefinitionBuilder.genericBeanDefinition(listenerContainerClass);
		beanDefinitionBuilder.addPropertyReference("ConnectionFactory", connectionFactoryBeanName);
		beanDefinitionBuilder.addPropertyReference("MessageListener", messageListenerAdapterBeanName);
		beanDefinitionBuilder.addPropertyValue("QueueNames", queueName);
		BeanDefinition beanDefinition = beanDefinitionBuilder.getBeanDefinition();
		return beanDefinition;
	}
	
	private Map<BeanDefinition,String> getBeanNames(BeanDefinitionRegistry registry) {
		Map<BeanDefinition,String> ret = new HashMap<BeanDefinition,String>();
		String[] definitionNames = registry.getBeanDefinitionNames();
		for (String definitionName: definitionNames) {
			BeanDefinition beanDefinition = registry.getBeanDefinition(definitionName);
			ret.put(beanDefinition, definitionName);
		}
		return ret;
	}
	private String getBeanName(Map<BeanDefinition,String> map, String beanType, BeanDefinitionRegistry registry) {
		List<String> ret = new ArrayList<String>();
		for (BeanDefinition beanDefinition: map.keySet()) {
			String beanClassName = beanDefinition.getBeanClassName();
			if (beanClassName == null) {
				// assume we have a factory of some kind
				beanClassName = getFactoryType(registry,beanDefinition.getFactoryBeanName(),beanDefinition.getFactoryMethodName());
			}
			if (beanType.equals(beanClassName)) {
				ret.add(map.get(beanDefinition));
			}
		}
		Assert.isTrue(ret.size() == 1,"There must be exactly one bean of type "+beanType+", found "+ret.size());
		return ret.get(0);
	}
	private String getFactoryType(BeanDefinitionRegistry registry, String factoryName, String factoryMethod) {
		String beanClassName = registry.getBeanDefinition(factoryName).getBeanClassName();
		Class<?> beanClass;
		try {
			beanClass = Class.forName(beanClassName);
		} catch (ClassNotFoundException e) {
			throw new RuntimeException(e);
		}
		for (Method method : beanClass.getMethods()) {
			if (factoryMethod.equals(method.getName())) {
				Class<?> returnType = method.getReturnType();
				String returnTypeName = returnType.getCanonicalName();
				return returnTypeName;
			}
		}
		return null;
	}

}

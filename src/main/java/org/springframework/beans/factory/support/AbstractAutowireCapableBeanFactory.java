package org.springframework.beans.factory.support;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.beans.factory.config.BeanPostProcessor;

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * Created by arahansa on 2016-03-20.
 */
@Slf4j
public abstract class AbstractAutowireCapableBeanFactory extends AbstractBeanFactory
        implements AutowireCapableBeanFactory {

    static {
        // Eagerly load the DisposableBean class to avoid weird classloader
        // issues on EJB shutdown within WebLogic 8.1's EJB container.
        // (Reported by Andreas Senft.)
        DisposableBean.class.getName();
    }


    private final Set disposableInnerBeans = Collections.synchronizedSet(new HashSet());

    public AbstractAutowireCapableBeanFactory() {
    }
    public AbstractAutowireCapableBeanFactory(BeanFactory parentBeanFactory) {
        super(parentBeanFactory);
    }

    //---------------------------------------------------------------------
    // AutowireCapableBeanFactory 의 구현
    // Implementation of AutowireCapableBeanFactory
    //---------------------------------------------------------------------
    public Object autowire(Class beanClass, int autowireMode, boolean dependencyCheck)
            throws BeansException {
        RootBeanDefinition bd = new RootBeanDefinition(beanClass, autowireMode, dependencyCheck);
        if (bd.getResolvedAutowireMode() == AUTOWIRE_CONSTRUCTOR) {
            return autowireConstructor(beanClass.getName(), bd).getWrappedInstance();
        }
        else {
            Object bean = BeanUtils.instantiateClass(beanClass);
            populateBean(bean.getClass().getName(), bd, new BeanWrapperImpl(bean));
            return bean;
        }
    }

    public void autowireBeanProperties(Object existingBean, int autowireMode, boolean dependencyCheck)
            throws BeansException {
        if (autowireMode != AUTOWIRE_BY_NAME && autowireMode != AUTOWIRE_BY_TYPE) {
            throw new IllegalArgumentException("Just constants AUTOWIRE_BY_NAME and AUTOWIRE_BY_TYPE allowed");
        }
        RootBeanDefinition bd = new RootBeanDefinition(existingBean.getClass(), autowireMode, dependencyCheck);
        populateBean(existingBean.getClass().getName(), bd, new BeanWrapperImpl(existingBean));
    }

    public Object applyBeanPostProcessorsBeforeInitialization(Object bean, String name) throws BeansException {
        if (log.isDebugEnabled()) {
            log.debug("Invoking BeanPostProcessors before initialization of bean '" + name + "'");
        }
        Object result = bean;
        for (Iterator it = getBeanPostProcessors().iterator(); it.hasNext();) {
            BeanPostProcessor beanProcessor = (BeanPostProcessor) it.next();
            result = beanProcessor.postProcessBeforeInitialization(result, name);
        }
        return result;
    }








}

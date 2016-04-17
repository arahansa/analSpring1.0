package org.springframework.beans.factory;

import org.springframework.beans.BeansException;

/**
 * Created by arahansa on 2016-03-30.
 */
public interface BeanFactoryAware {
    void setBeanFactory(BeanFactory beanFactory) throws BeansException;
}

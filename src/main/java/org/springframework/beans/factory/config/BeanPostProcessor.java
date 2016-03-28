package org.springframework.beans.factory.config;

import org.springframework.beans.BeansException;

/**
 * Created by arahansa on 2016-03-12.
 */
public interface BeanPostProcessor {

    Object postProcessBeforeInitialization(Object bean, String name) throws BeansException;

    Object postProcessAfterInitialization(Object bean, String name) throws BeansException;
}

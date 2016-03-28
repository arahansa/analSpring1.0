package org.springframework.beans.factory.config;

import org.springframework.beans.MutablePropertyValues;

/**
 * Created by arahansa on 2016-03-12.
 */
public interface BeanDefinition {
    MutablePropertyValues getPropertyValues();
    ConstructorArgumentValues getConstructorArgumentValues();
    String getResourceDescription();
}

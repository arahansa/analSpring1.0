package org.springframework.beans.factory.config;

import org.springframework.beans.factory.ListableBeanFactory;

/**
 * Created by arahansa on 2016-03-20.
 */
public interface ConfigurableListableBeanFactory
        extends ListableBeanFactory, ConfigurableBeanFactory, AutowireCapableBeanFactory {

    void preInstantiateSingletons();

}

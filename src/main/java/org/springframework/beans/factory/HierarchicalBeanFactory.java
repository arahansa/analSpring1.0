package org.springframework.beans.factory;

/**
 * Created by arahansa on 2016-03-12.
 */
public interface HierarchicalBeanFactory extends BeanFactory {

    BeanFactory getParentBeanFactory();
}

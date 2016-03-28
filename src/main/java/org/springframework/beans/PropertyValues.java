package org.springframework.beans;

/**
 * Created by arahansa on 2016-03-20.
 */
public interface PropertyValues {

    PropertyValue[] getPropertyValues();

    PropertyValue getPropertyValue(String propertyName);

    boolean contains(String propertyName);

    PropertyValues changesSince(PropertyValues old);

}

package org.springframework.beans;

import java.beans.PropertyDescriptor;
import java.beans.PropertyEditor;
import java.util.Map;

/**
 * Created by arahansa on 2016-03-28.
 */
public interface BeanWrapper {

    /**
     * Path separator for nested properties.
     * Follows normal Java conventions: getFoo().getBar() would be "foo.bar".
     */
    String NESTED_PROPERTY_SEPARATOR = ".";

    /**
     * Change the wrapped object. Implementations are required
     * to allow the type of the wrapped object to change.
     * @param obj wrapped object that we are manipulating
     */
    void setWrappedInstance(Object obj) throws BeansException;

    Object getWrappedInstance();

    Class getWrappedClass();


    void registerCustomEditor(Class requiredType, PropertyEditor propertyEditor);


    void registerCustomEditor(Class requiredType, String propertyPath, PropertyEditor propertyEditor);


    PropertyEditor findCustomEditor(Class requiredType, String propertyPath);

    Object getPropertyValue(String propertyName) throws BeansException;

    void setPropertyValue(String propertyName, Object value) throws BeansException;

    void setPropertyValue(PropertyValue pv) throws BeansException;

    void setPropertyValues(Map map) throws BeansException;

    void setPropertyValues(PropertyValues pvs) throws BeansException;

    void setPropertyValues(PropertyValues pvs, boolean ignoreUnknown)
            throws BeansException;



    PropertyDescriptor[] getPropertyDescriptors() throws BeansException;


    PropertyDescriptor getPropertyDescriptor(String propertyName) throws BeansException;


    boolean isReadableProperty(String propertyName);


    boolean isWritableProperty(String propertyName);
}

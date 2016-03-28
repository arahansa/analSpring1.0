package org.springframework.beans;

/**
 * Created by arahansa on 2016-03-20.
 */
public class PropertyValue {

    /** Property name */
    private String name;

    /** Value of the property */
    private Object value;

    /**
     * Creates new PropertyValue.
     * @param name name of the property
     * @param value value of the property (possibly before type conversion)
     */
    public PropertyValue(String name, Object value) {
        if (name == null) {
            throw new IllegalArgumentException("Property name cannot be null");
        }
        this.name = name;
        this.value = value;
    }

    /**
     * Return the name of the property.
     * @return the name of the property
     */
    public String getName() {
        return name;
    }

    /**
     * Return the value of the property.
     * <p>Note that type conversion will <i>not</i> have occurred here.
     * It is the responsibility of the BeanWrapper implementation to
     * perform type conversion.
     * @return the value of the property
     */
    public Object getValue() {
        return value;
    }

    public String toString() {
        return "PropertyValue: name='" + name + "'; value=[" + value + "]";
    }

    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof PropertyValue)) {
            return false;
        }
        PropertyValue otherPv = (PropertyValue) other;
        return (this.name.equals(otherPv.name) &&
                ((this.value == null && otherPv.value == null) || this.value.equals(otherPv.value)));
    }

    public int hashCode() {
        return this.name.hashCode() * 29 + (this.value != null ? this.value.hashCode() : 0);
    }
}

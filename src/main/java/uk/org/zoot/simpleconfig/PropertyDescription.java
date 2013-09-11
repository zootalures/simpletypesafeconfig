package uk.org.zoot.simpleconfig;


import com.google.common.base.Preconditions;

import java.lang.reflect.Method;

public class PropertyDescription {
    PropertyDescription(Method readMethod, String property, String description,
                        boolean required, Class<?> type, String	 defaultValue) {
        super();
        this.property = Preconditions.checkNotNull(property);
        this.description = description;
        this.required = required;
        this.type = Preconditions.checkNotNull(type);
        this.defaultValue = defaultValue;
        this.readMethod = readMethod;

    }

    private final String property;
    private final String description;
    private final boolean required;
    private final Class<?> type;
    private final String defaultValue;
    private final Method readMethod;

    public String getProperty() {
        return property;
    }

    public String getDescription() {
        return description;
    }

    public boolean isRequired() {
        return required;
    }


    public boolean isMultiValued() {
        return type.isArray();
    }

    public Class<?> getType() {
        return type;
    }

    public Class<?> getComponentType() {
        if (type.isArray()) {
            return type.getComponentType();
        } else {
            return type;
        }
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    public Method getReadMethod() {
        return readMethod;
    }
}

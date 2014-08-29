package uk.org.zoot.simpleconfig;


import com.google.common.base.Preconditions;

import java.lang.reflect.Method;

/**
 *
 */
public class PropertyDescriptionImpl implements PropertyDescription {
    PropertyDescriptionImpl(Method readMethod, String property, String description,
                            boolean required, Class<?> type, String defaultValue) {
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

    @Override
    public String getProperty() {
        return property;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public boolean isRequired() {
        return required;
    }


    @Override
    public boolean isMultiValued() {
        return type.isArray();
    }

    @Override
    public Class<?> getType() {
        return type;
    }

    @Override
    public Class<?> getComponentType() {
        if (type.isArray()) {
            return type.getComponentType();
        } else {
            return type;
        }
    }

    @Override
    public String getDefaultValue() {
        return defaultValue;
    }

    public Method getReadMethod() {
        return readMethod;
    }
}

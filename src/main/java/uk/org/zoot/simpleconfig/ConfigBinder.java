package uk.org.zoot.simpleconfig;

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.google.common.collect.Iterables;

import java.io.PrintStream;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.*;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Factory for creating bound configuration instances
 */
public class ConfigBinder {
    /**
     * Creates a property description from a specified method
     *
     * @param method
     * @return
     */
    private PropertyDescription createDescriptionFromMethod(Method method) {
        String propertyName;
        String description;
        boolean required = true;
        String defaultValue;
        Class<?> type = method.getReturnType();
        if (!validPropertyType(type)) {
            throw new InvalidConfigInterfaceException("Method " + method
                    + " has an invalid return type " + type
                    + " only primitive types and strings are supported");
        }
        if (method.getParameterTypes().length != 0) {
            throw new InvalidConfigInterfaceException(
                    "Method "
                            + method
                            + " has an invalid signature, only methods with empty signatures are supported");
        }
        if (method.isAnnotationPresent(ConfigProperty.class)) {
            ConfigProperty pb = method.getAnnotation(ConfigProperty.class);
            propertyName = pb.value();
            description = pb.description();
            required = pb.required();

            if (ConfigProperty.NODEFAULT
                    .equals(pb.defaultValue())) {
                defaultValue = null;
            } else {
                defaultValue = pb.defaultValue();
            }
        } else {
            propertyName = method.getName();
            defaultValue = null;
            description = null;
        }

        if (type.isPrimitive() && !required && defaultValue == null) {
            throw new InvalidConfigException("property " + propertyName
                    + " is optional, has no default and has a primititve type ");
        }

        return new PropertyDescription(method, propertyName, description,
                required, type, defaultValue);
    }

    private boolean validPropertyType(Class<?> type) {
        return validElementType(type)
                || (type.isArray() && validElementType(type.getComponentType()));
    }

    private boolean validElementType(Class<?> type) {
        return (type.isPrimitive() || type.isEnum() || type
                .equals(String.class));
    }

    private interface MethodVisitor {
        public void visitMethod(Class<?> type, Method m);
    }

    private void visitAllMethodsIncludingParents(Set<Class<?>> visited,
                                                 Class<?> type, MethodVisitor mv) {
        if (visited.contains(type)) {
            return;
        }
        visited.add(type);
        for (Class<?> ifs : type.getInterfaces()) {
            visitAllMethodsIncludingParents(visited, ifs, mv);
        }
        for (Method m : type.getMethods()) {
            mv.visitMethod(type, m);
        }
    }

    /**
     * Binds an interface to a property bundle
     *
     * @param type
     *            the Destination Interface type
     * @param bundle
     *            the property bundle
     * @return a new interface of the properties object
     * @throws InvalidConfigException
     */
    @SuppressWarnings("unchecked")
    public <T> T bind(Class<T> type, final Properties bundle)
            throws InvalidConfigException {
        checkNotNull(type, "type is required");
        checkNotNull(bundle, "bundle is required");

        final Map<Method, PropertyDescription> properties = extractDescriptors(type);

        final Method getDescriptorsMethod;
        try {
            getDescriptorsMethod = ConfigProxy.class
                    .getMethod("getDescriptors");
        } catch (Exception e) {
            throw new RuntimeException("Failed to get descriptor method", e);
        }
        return (T) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class[]{type, ConfigProxy.class},
                new InvocationHandler() {
                    public Object invoke(Object proxy, Method method,
                                         Object[] args) throws Throwable {
                        if (method.equals(getDescriptorsMethod)) {
                            return properties;
                        }
                        PropertyDescription desc = properties.get(method);
                        Preconditions.checkState(desc != null,
                                "cannot find descrptor for method %s ", method);

                        return fetchPropertyValue(desc, bundle);
                    }

                });
    }

    public <T> Map<Method, PropertyDescription> extractDescriptors(Class<T> type) {
        final Map<Method, PropertyDescription> properties = new HashMap<Method, PropertyDescription>();
        visitAllMethodsIncludingParents(new HashSet<Class<?>>(), type,
                new MethodVisitor() {
                    @Override
                    public void visitMethod(Class<?> type, Method m) {
                        properties.put(m, createDescriptionFromMethod(m));
                    }
                });
        return properties;
    }

    /**
     * Returns the property value in a given form
     *
     * @param desc
     * @param bundle
     * @return
     */
    private Object fetchPropertyValue(PropertyDescription desc,
                                      Properties bundle) throws InvalidConfigException {

        String property = desc.getProperty();
        String value = bundle.getProperty(property, desc.getDefaultValue());
        if (value == null && desc.isRequired()) {
            throw new InvalidConfigException("Property "
                    + desc.getProperty() + " is required but not set");
        }

        if (desc.getType().isArray()) {
            if (value == null) {
                return null;
            } else {
                List<String> values = new LinkedList<String>();
                Iterables.addAll(values, Splitter.on(",").omitEmptyStrings()
                        .split(value));
                Object[] destArrray = (Object[]) Array.newInstance(desc
                        .getType().getComponentType(), values.size());
                for (int i = 0; i < values.size(); i++) {
                    destArrray[i] = extractBaseValue(desc.getProperty(), desc
                            .getType().getComponentType(), values.get(i));
                }
                return destArrray;
            }
        } else {
            return extractBaseValue(desc.getProperty(), desc.getType(), value);
        }
    }

    @SuppressWarnings("unchecked")
    public Object extractBaseValue(String property, Class<?> targetType,
                                   String input) {
        String value;
        if (input != null) {
            value = input.trim();
        } else {
            value = null;
        }

        if (targetType == int.class) {
            return Integer.parseInt(value);
        } else if (targetType == float.class) {
            return Float.parseFloat(value);
        } else if (targetType == double.class) {
            return Double.parseDouble(value);
        } else if (targetType == boolean.class) {
            return Boolean.parseBoolean(value);
        } else if (targetType == long.class) {
            return Long.parseLong(value);
        } else if (targetType == String.class) {
            return value;
        } else if (targetType.isEnum()) {
            if (value != null) {
                @SuppressWarnings({ "unchecked", "rawtypes" })
                Class<? extends Enum> enumType = (Class<? extends Enum<?>>) targetType;
                try {
                    return Enum.valueOf(enumType, value);
                } catch (Exception e) {
                    throw new InvalidConfigException(String.format(
                            "Unsupported property value %s on property %s, valid values are [%s]",
                            value, property,
                            Joiner.on(",").join(enumType.getEnumConstants()))
                                    );
                }
            } else {
                return null;
            }
        } else {
            throw new InvalidConfigException("Unsupported return type "
                    + targetType + " on property " + property);
        }
    }

    /**
     * Create a dynamic proxy based on an interface describing a property file,
     * validating that each property can be read without exception
     *
     * @param type
     * @param bundle
     * @return
     * @throws InvalidConfigException
     */
    public <T> T bindAndValidate(Class<T> type, final Properties bundle)
            throws InvalidConfigException {
        checkNotNull(type, "type is required");
        checkNotNull(bundle, "bundle is required");

        validate(type, bundle);
        return bind(type, bundle);
    }

    /**
     * Validates a bindle against a specified interface
     *
     * @param type
     * @param bundle
     */
    public <T> void validate(Class<T> type, final Properties bundle) {
        Map<Method, PropertyDescription> descs = extractDescriptors(type);

        Map<PropertyDescription, InvalidConfigException> errors = new LinkedHashMap<PropertyDescription, InvalidConfigException>();
        for (PropertyDescription propDesc : descs.values()) {
            try {
                fetchPropertyValue(propDesc, bundle);
            } catch (InvalidConfigException ex) {
                errors.put(propDesc, ex);
            }
        }
        if (errors.size() > 0) {
            StringBuilder errorMsg = new StringBuilder();
            errorMsg.append("Invalid properties : ");
            for (Map.Entry<PropertyDescription, InvalidConfigException> entry : errors
                    .entrySet()) {
                errorMsg.append(String.format("{ %s : %s }", entry.getKey()
                        .getProperty(), entry.getValue().getMessage()));
            }
            throw new InvalidConfigException(errorMsg.toString());
        }
    }

    /**
     * Prints the property usage for one or more config interfaces
     *
     * @param types
     *            The config interfaces to describe
     * @param out
     */
    public void printPropertyDescription(PrintStream out, Class<?>... types) {
        Map<Method, PropertyDescription> descs = new HashMap<Method, PropertyDescription>();
        for (Class<?> t : types) {
            descs.putAll(extractDescriptors(t));
        }

        List<PropertyDescription> desclist = new ArrayList<PropertyDescription>();
        desclist.addAll(descs.values());
        Collections.sort(desclist, new Comparator<PropertyDescription>() {
            @Override
            public int compare(PropertyDescription p1, PropertyDescription p2) {
                return p1.getProperty().compareTo(p2.getProperty());
            }
        });

        for (PropertyDescription pd : desclist) {
            String values = "";
            Class<?> primitiveType = pd.getComponentType();
            if (primitiveType.isEnum()) {
                values = String.format(" values : [%s]",
                        Joiner.on(",").join(primitiveType.getEnumConstants()));
            }
            List<String> flags = new ArrayList<String>();
            if (pd.isRequired()) {
                flags.add("required");
            } else {
                flags.add("optional");
            }
            if (pd.isMultiValued()) {
                flags.add("multi-valued");
            }
            out.println(pd.getProperty()
                    + " : "
                    + pd.getDescription()
                    + " "
                    + (flags.size() > 0 ? ("(" + Joiner.on(",").join(flags) + ")")
                    : "") + values);
        }
    }
}


package uk.org.zoot.simpleconfig;

import com.fasterxml.classmate.*;
import com.fasterxml.classmate.members.ResolvedMethod;
import com.google.common.base.*;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;

import java.io.PrintStream;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.*;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

/**
 * Factory for creating bound configuration instances
 */
public class ConfigBinder {

	private final TypeResolver typeResolver = new TypeResolver();

	/**
	 * Creates a property description from a specified method
	 *
	 * @param method
	 * @return a property description of a given method
	 */
	private PropertyDescription createDescriptionFromMethod(ResolvedMethod method) {
		String propertyName;
		String description;
		boolean required = true;
		String defaultValue;
		ResolvedType type = method.getReturnType();
		checkState(type != null, "Type must be non-null on %s", method);
		if (!validPropertyType(type)) {
			throw new InvalidConfigInterfaceException("Method " + method
					+ " has an invalid return type " + type
					+ " only primitive types and strings are supported");
		}
		if (method.getRawMember().getParameterTypes().length != 0) {
			throw new InvalidConfigInterfaceException(
					"Method "
							+ method
							+ " has an invalid signature, only methods with empty signatures are supported");
		}
		if (method.getAnnotations().get(ConfigProperty.class) != null) {
			ConfigProperty pb = method.getAnnotations().get(ConfigProperty.class);
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
		boolean multiValued ;
		Class<?> elementType ;
		if(validCollectionType(type)){
			multiValued = true;
			if(type.isArray()){
				elementType = type.getArrayElementType().getErasedType();
			}else{
				elementType = type.findSupertype(Collection.class).getTypeParameters().get(0).getErasedType();
			}
		}else{
			multiValued = false;
			elementType = type.getErasedType();
		}

		return new PropertyDescriptionImpl(method.getRawMember(), propertyName, description,
				required, type.getErasedType(), multiValued, elementType,defaultValue);
	}

	private boolean validPropertyType(ResolvedType type) {
		return validElementType(type)
				|| validCollectionType(type);
	}

	private boolean validCollectionType(ResolvedType type) {
		return (type.isArray() && validElementType(type.getArrayElementType()))
				|| ((type.isInstanceOf(List.class) || type.isInstanceOf(Set.class))
				&& validElementType(type.findSupertype(Collection.class).getTypeParameters().get(0)));
	}

	private boolean validElementType(ResolvedType type) {
		return (type.isPrimitive() || type.getErasedType().isEnum() || type
				.isInstanceOf(String.class));
	}

	private interface MethodVisitor {

		public void visitMethod(ResolvedType type, ResolvedMethod m);
	}

	private void visitAllMethodsIncludingParents(Class<?> type, MethodVisitor mv) {
		ResolvedType resolvedType = typeResolver.resolve(type);
		MemberResolver memberResolver = new MemberResolver(typeResolver);
		ResolvedTypeWithMembers membersType = memberResolver.resolve(resolvedType, new AnnotationConfiguration.StdConfiguration(AnnotationInclusion.INCLUDE_AND_INHERIT), null);

		for (ResolvedMethod rm : membersType.getMemberMethods()) {
			mv.visitMethod(resolvedType, rm);
		}
	}

	/**
	 * Binds an interface to a property bundle
	 *
	 * @param type   the Destination Interface type
	 * @param bundle the property bundle
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
				new Class[] { type, ConfigProxy.class },
				new InvocationHandler() {

					public Object invoke(Object proxy, Method method,
							Object[] args) throws Throwable {
						if (method.equals(getDescriptorsMethod)) {
							return properties;
						}
						PropertyDescription desc = properties.get(method);
						Preconditions.checkState(desc != null,
								"cannot find descriptor for method %s ", method);

						return fetchPropertyValue(desc, bundle);
					}

				});
	}

	public <T> Map<Method, PropertyDescription> extractDescriptors(Class<T> type) {
		final Map<Method, PropertyDescription> properties = new HashMap<Method, PropertyDescription>();
		visitAllMethodsIncludingParents(type,
				new MethodVisitor() {

					@Override
					public void visitMethod(ResolvedType type, ResolvedMethod m) {
						properties.put(m.getRawMember(), createDescriptionFromMethod(m));
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
	private Object fetchPropertyValue(final PropertyDescription desc,
			Properties bundle) throws InvalidConfigException {

		final String property = desc.getProperty();
		String value = bundle.getProperty(property, desc.getDefaultValue());
		if (value == null && desc.isRequired()) {
			throw new InvalidConfigException("Property "
					+ desc.getProperty() + " is required but not set");
		}

		if (desc.isMultiValued()) {
			List<Object> values = new ArrayList<Object>();

			Iterables.addAll(values,
					Iterables.transform(Splitter.on(",").omitEmptyStrings()
							.split(Strings.nullToEmpty(value)), new Function<String, Object>() {

						@Override public Object apply(String input) {
							return extractBaseValue(property, desc.getComponentType(), input);
						}
					}));

			return createMultiValuedContainer(desc, values);

		} else {
			return extractBaseValue(desc.getProperty(), desc.getType(), value);
		}
	}

	private Object createMultiValuedContainer(PropertyDescription desc, List<Object> values) {

		if (desc.getType().isArray()) {
			return values.toArray((Object[]) Array.newInstance(desc
					.getType().getComponentType(), values.size()));
		} else if (Set.class.isAssignableFrom(desc.getType())) {
			return Sets.newHashSet(values);
		} else if (List.class.isAssignableFrom(desc.getType())) {
			return values;
		} else {
			throw new IllegalStateException("Cannot create multi-valued container for unsupported base type " + desc.getType());
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
	 * @param types The config interfaces to describe
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


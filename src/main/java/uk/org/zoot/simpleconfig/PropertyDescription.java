package uk.org.zoot.simpleconfig;

/**
 * Created with IntelliJ IDEA.
 * User: occ
 * Date: 13/09/13
 * Time: 08:25
 * To change this template use File | Settings | File Templates.
 */
public interface PropertyDescription {
    String getProperty();

    String getDescription();

    boolean isRequired();

    boolean isMultiValued();

    Class<?> getType();

    Class<?> getComponentType();

    String getDefaultValue();
}

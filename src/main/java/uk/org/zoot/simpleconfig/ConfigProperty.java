package uk.org.zoot.simpleconfig;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotate an interface method to be bound to a property in a property file
 *
 * This is used in conjunction with a {@link ConfigFactory} and a target
 * configuration interface to wrap configuratino data from a property bundler in
 * a strongly typed java interface.
 *
 * The binder supports properties of the following types:
 *
 * Primitive types  (int,long,boolean,double,float)
 *
 * Apply this annotation to a method of an interface in order to bind that
 * method to an underlying property value;
 *
 *
 * @author cliffeo
 *
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface ConfigProperty {

        public static final String NODEFAULT = "_NODEFAULT_";

        /**
         * The property name in the file to use
         *
         * @return
         */
        String value();

        /**
         * A textual description of the property
         *
         * @return
         */
        String description() default "";

        /**
         * Must this property be set in the file? (defaults to true)
         *
         * @return
         */
        boolean required() default true;

        /**
         * A default value (as a string) for this property.
         *
         * Note for primitive typed methods, the property must either be required or
         * must have a default value.
         *
         * for Object-typed methods null will be returned
         *
         * @return
         */
        String defaultValue() default NODEFAULT;
    }


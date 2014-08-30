package uk.org.zoot.simpleconfig;

import org.hamcrest.CoreMatchers;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.*;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.*;

public class ConfigBinderTest {


    static class PropertyBuilder {
        Properties properties = new Properties();

        public PropertyBuilder withProperty(String key, String value) {
            properties.setProperty(checkNotNull(key, "key cannot be null"),
                    checkNotNull(value, "value cannot be null"));
            return this;
        }

        public Properties build() {
            return (Properties) properties.clone();
        }
    }

    ConfigBinder binder = new ConfigBinder();

    public interface InvalidRT {
        public Object property();
    }

    public interface InvalidArgs {
        public String property(String arg);
    }

    public interface SubProps {
        @ConfigProperty("subProp")
        public String subProp();

    }

    public interface SuperProps extends SubProps {
        @ConfigProperty("superProp")
        public String superProp();

    }

    public enum SampleEnum {
        GOODVALUE, OTHERGOODVALUE
    }

    public interface EnumProps{
        @ConfigProperty(value = "enumProperty", required = false)
        public SampleEnum enumProperty();
        @ConfigProperty(value = "enumArrayProperty", required = false)
        public SampleEnum[] enumArrayProperty();
    }
    public interface SimpleProps {
        public String propertyName();

        @ConfigProperty("myProp")
        public String annotated();

        @ConfigProperty(value = "defaulted", defaultValue = "defaultedValue")
        public String defaulted();

        @ConfigProperty(value = "intPropertyName", defaultValue = "100")
        public int intProperty();

        @ConfigProperty(value = "boolProperty", description = "A boolean property")
        public boolean boolProperty();

        public long longProperty();

        public float floatProperty();

        public double doubleProperty();



        @ConfigProperty(value = "arrayStringProperty", required = false)
        public String[] arrayStringProperty();

		public Set<SampleEnum> enumSetProps();

		public List<String> stringListProps();

	}

    @Test
    public void canLoadSimpleStringPropertyWithNoAnnotation() {
        SimpleProps props = binder.bind(SimpleProps.class,
                new PropertyBuilder().withProperty("propertyName", "value")
                        .build());

        assertEquals("value", props.propertyName());
    }

    @Test
    public void canLoadSimpleStringPropertyWithAnnotation() {
        SimpleProps props = binder.bind(SimpleProps.class,
                new PropertyBuilder().withProperty("myProp", "value1").build());

        assertEquals("value1", props.annotated());
    }

    @Test
    public void canLoadIntegerProperty() {
        SimpleProps props = binder.bind(SimpleProps.class,
                new PropertyBuilder().withProperty("intPropertyName", "10")
                        .build());

        assertEquals(10, props.intProperty());

    }

    @Test
    public void canLoadBoolProperty() {
        SimpleProps props = binder.bind(SimpleProps.class,
                new PropertyBuilder().withProperty("boolProperty", "true")
                        .build());

        assertEquals(true, props.boolProperty());

    }

    @Test
    public void canLoadLongProperty() {
        SimpleProps props = binder.bind(SimpleProps.class,
                new PropertyBuilder().withProperty("longProperty", "10000")
                        .build());

        assertEquals(10000l, props.longProperty());

    }

    @Test
    public void canLoadFloatProperty() {
        SimpleProps props = binder.bind(SimpleProps.class,
                new PropertyBuilder().withProperty("floatProperty", "10.00")
                        .build());
        assertEquals(10.00, props.floatProperty(), 1e-15);
    }

    @Test
    public void canLoadDoubleProperty() {
        SimpleProps props = binder.bind(SimpleProps.class,
                new PropertyBuilder().withProperty("doubleProperty", "10.00")
                        .build());
        assertEquals(10.00, props.doubleProperty(), 1e-15);
    }

    @Test
    public void canLoadValidEnumProperty() {
        EnumProps props = binder.bind(EnumProps.class,
                new PropertyBuilder().withProperty("enumProperty", "GOODVALUE")
                        .build());
        assertEquals(SampleEnum.GOODVALUE, props.enumProperty());
    }

    @Test
    public void canLoadEmptyEnumProperty() {
        EnumProps props = binder.bind(EnumProps.class,
                new PropertyBuilder().build());
        assertEquals(null, props.enumProperty());
    }

    @Test()
    public void failsWhenEnumValueIsInvalid() {
        try {
            binder.bind(
                    EnumProps.class,
                    new PropertyBuilder().withProperty("enumProperty",
                            "SOMEOadsaTHERVALUE").build()).enumProperty();

            fail("should have thrown a binding exception");
        } catch (InvalidConfigException e) {
            assertThat(e.getMessage(),
                    containsString("[GOODVALUE,OTHERGOODVALUE]"));
        }
    }

    @Test(expected = NumberFormatException.class)
    public void failsWhenIntPropertyInvalid() {
        SimpleProps props = binder.bind(SimpleProps.class,
                new PropertyBuilder().withProperty("intPropertyName", "s10")
                        .build());

        props.intProperty();
    }

    @Test()
    public void failsToValidateWhenReturnTypeIsInvalid() {
        try {
            binder.bindAndValidate(InvalidRT.class,
                    new PropertyBuilder().build());
            fail("should have thrown a binding exception");
        } catch (InvalidConfigInterfaceException e) {
            assertThat(e.getMessage(), containsString("return type"));
        }
    }

    @Test(expected = InvalidConfigException.class)
    public void shouldFailWhenRequiredValueNotBind() {
        SimpleProps props = binder.bind(SimpleProps.class,
                new PropertyBuilder().build());

        props.annotated();
    }

    @Test()
    public void failsToValidateWhenSignatureIsInvalid() {
        try {
            binder.bindAndValidate(InvalidArgs.class,
                    new PropertyBuilder().build());
            fail("should have thrown a binding exception");
        } catch (InvalidConfigInterfaceException e) {
            assertThat(e.getMessage(), containsString("signature"));
        }
    }

    @Test()
    public void canBindtoSuperInterfaces() {
        SuperProps props = binder.bind(SuperProps.class,
                new PropertyBuilder().withProperty("subProp", "subPropVal")
                        .withProperty("superProp", "superPropVal").build());

        assertEquals("subPropVal", props.subProp());
        assertEquals("superPropVal", props.superProp());
    }

    @Test()
    public void canValidateMissingProps() {
        try {
            binder.validate(SimpleProps.class, new PropertyBuilder().build());
            fail("should have failed validation");
        } catch (InvalidConfigException e) {
            assertThat(
                    e.getMessage(),
                    containsString("{ myProp : Property myProp is required but not set }"));
        }
    }

    @Test()
    public void canPrintPropertyDescriptions() {

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(bos);
        binder.printPropertyDescription(ps, SimpleProps.class);
        ps.flush();
        String desc = new String(bos.toByteArray());
        assertThat(desc, containsString("myProp :  (required)"));
        assertThat(desc,
                containsString("boolProperty : A boolean property (required)"));
    }


    @Test()
    public void areEnumValuesShownInEnumProp() {

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(bos);
        binder.printPropertyDescription(ps, EnumProps.class);
        ps.flush();
        String desc = new String(bos.toByteArray());
        assertThat(desc,
                containsString("enumProperty :  (optional) values : [GOODVALUE,OTHERGOODVALUE]"));
    }

    @Test()
    public void areEnumArrayValuesShownInEnumProp() {

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(bos);
        binder.printPropertyDescription(ps, EnumProps.class);
        ps.flush();
        String desc = new String(bos.toByteArray());
        assertThat(desc,
                containsString("enumArrayProperty :  (optional,multi-valued) values : [GOODVALUE,OTHERGOODVALUE]"));
    }
    @Test()
    public void canLoadArrayPropertiesWithMultipleValues() {

        SimpleProps props = binder.bind(
                SimpleProps.class,
                new PropertyBuilder().withProperty("arrayStringProperty",
                        "string1, string2 , 3").build());

        assertArrayEquals(new String[] { "string1", "string2", "3" },
                props.arrayStringProperty());
    }

    @Test()
    public void canLoadArrayPropertiesWithNullValues() {

        SimpleProps props = binder.bind(SimpleProps.class,
                new PropertyBuilder().build());

        assertThat(props.arrayStringProperty(), CoreMatchers.equalTo(new String[0]));
    }

    @Test()
    public void canLoadArrayPropertiesWithEmptyString() {

        SimpleProps props = binder.bind(SimpleProps.class,
                new PropertyBuilder().withProperty("arrayStringProperty", "")
                        .build());
        assertArrayEquals(new String[] {}, props.arrayStringProperty());
    }

	@Test
    public void canLoadSetOfEnumProperties(){
		 SimpleProps props = binder.bind(SimpleProps.class,
		new PropertyBuilder().withProperty("enumSetProps","GOODVALUE,OTHERGOODVALUE").build());

		assertThat(props.enumSetProps(),equalTo((Set<SampleEnum>)EnumSet.of(SampleEnum.GOODVALUE, SampleEnum.OTHERGOODVALUE)));
    }


	@Test
	public void canLoadListOfStringProperties(){
		SimpleProps props = binder.bind(SimpleProps.class,
				new PropertyBuilder().withProperty("stringListProps","A,B").build());

		assertThat(props.stringListProps(),equalTo(Arrays.asList("A","B")));
	}


}

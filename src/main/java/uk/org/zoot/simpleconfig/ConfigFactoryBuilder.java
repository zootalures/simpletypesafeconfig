package uk.org.zoot.simpleconfig;

/**
 * Simple ConfigFactory builder
 */
public class ConfigFactoryBuilder {
    public static ConfigFactoryBuilder newBuilder() {
        return new ConfigFactoryBuilder();
    }

    ConfigFactory build(){
        return new ConfigFactory();
    }
}

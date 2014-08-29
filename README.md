Simple Type-Safe Config
=======================

The Problem: 
-----------

* You are writing a quick and dirty java app and you need configuration. 
* You're happy to use property-style bundle but you are annoyed by how much time it takes to load 
content from property bundles. 
* You want to make sure that config is valid 
* You want to inject different config aspect into different parts of your app
* You want to reload underlying config


The solution: 
------------

Config proxy:

    // Configuration Contract (with JSR 303 Annotations)
    public interface DBConfig{
       @ConfigProperty("dbUrl",required=true)
       @NotNull
       String getDbUrl();
    
    
       @ConfigProperty(value="connectTimeout", defaultValue="1000")
       @Min(0)
       int getConnectTimeout();


       public enum ConnectionFlags{
          KEEP_ALIVE,
          RECONNECT,
          POOL_CONNECTIONS
       };

       @ConfigProperty(value="connectionFlags")
       Set<ConnectionFlags> getConnectionFlags();
    }

Sample Config File:

    dbUrl=jdbc:mydb/test
    connectTimeout=1000
    multivalues=A,B,C,D

Config usage

    // Load and validate the config 
    ConfigBinder binder = new ConfigBinder();

    Properties p = new Properties();

    p.load("config.properties");
 
    ExampleConfig config = cf.bindExampleConfig.class);
 

    // use the config
    String dbUrl = config.getDbUrl();


   

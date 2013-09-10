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

    // Configuration Contract (with JSR 303 Annotations) 
    public interface ExampleConfig{
    
       @Config("dbUrl")
       @NotNull
       String getDbUrl();
    
    
       @Config(value="connectTimeout", default="1000")
       @Min(0)
       int getConnectTimeout();
 
    }
 
    // Load and validate the config 
    ConfigFactory cf = ConfigFactoryBuilder.newBuilder().withValidation().build();
 
    ExampleConfig config = cf.fromFile("config.properties").bindConfig(ExampleConfig.class); 
 
 
    // use the config
    String dbUrl = config.getDbUrl();
 
   

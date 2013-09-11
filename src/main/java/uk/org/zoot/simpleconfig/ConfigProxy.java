package uk.org.zoot.simpleconfig;
import java.lang.reflect.Method;
import java.util.Map;


/**
 * Internal proxy interface which all property proxies implement can be used to
 * retrieve service info about the properties bound to a proxy
 *
 * @author cliffeo
 *
 */
public interface ConfigProxy {
    /**
     *
     * @return a  map of methods to property descriptors
     */
    Map<Method, PropertyDescription> getDescriptors();
}

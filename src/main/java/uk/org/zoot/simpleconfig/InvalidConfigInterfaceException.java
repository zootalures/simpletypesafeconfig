package uk.org.zoot.simpleconfig;
/**
 * Exception thrown when annotations on the proxy are invalid
 *
 * @author cliffeo
 *
 */
@SuppressWarnings("serial")
public class InvalidConfigInterfaceException extends RuntimeException {

    public InvalidConfigInterfaceException(String message, Throwable cause) {
        super(message, cause);
    }

    public InvalidConfigInterfaceException(String message) {
        super(message);
    }

    public InvalidConfigInterfaceException(Throwable cause) {
        super(cause);
    }

}
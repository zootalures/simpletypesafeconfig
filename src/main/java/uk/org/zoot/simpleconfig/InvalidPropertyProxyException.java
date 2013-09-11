package uk.org.zoot.simpleconfig;
/**
 * Exception thrown when annotations on the proxy are invalid
 *
 * @author cliffeo
 *
 */
@SuppressWarnings("serial")
public class InvalidPropertyProxyException extends RuntimeException {

    public InvalidPropertyProxyException(String message, Throwable cause) {
        super(message, cause);
    }

    public InvalidPropertyProxyException(String message) {
        super(message);
    }

    public InvalidPropertyProxyException(Throwable cause) {
        super(cause);
    }

}
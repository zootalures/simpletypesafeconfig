package uk.org.zoot.simpleconfig;
@SuppressWarnings("serial")
public class InvalidPropertiesException extends RuntimeException {

    public InvalidPropertiesException(String message, Throwable cause) {
        super(message, cause);
    }

    public InvalidPropertiesException(String message) {
        super(message);
    }

    public InvalidPropertiesException(Throwable cause) {
        super(cause);
    }

}
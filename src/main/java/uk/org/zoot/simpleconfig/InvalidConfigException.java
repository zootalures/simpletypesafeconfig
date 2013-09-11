package uk.org.zoot.simpleconfig;
@SuppressWarnings("serial")
public class InvalidConfigException extends RuntimeException {

    public InvalidConfigException(String message, Throwable cause) {
        super(message, cause);
    }

    public InvalidConfigException(String message) {
        super(message);
    }

    public InvalidConfigException(Throwable cause) {
        super(cause);
    }

}
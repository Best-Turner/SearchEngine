package searchengine.exception;

public abstract class ApiException extends Exception {

    public ApiException(String message) {
        super(message);
    }
}

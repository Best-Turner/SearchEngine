package searchengine.exception;

public class InvalidQueryException extends ApiException {
    public InvalidQueryException(String errorMessage) {
        super(errorMessage);
    }
}

package uk.mcga.smart.lambda.exception;

public class SmartException extends  RuntimeException {

    private final Integer statusCode;

    public SmartException(String message, Integer statusCode) {
        super(message);
        this.statusCode = statusCode;
    }

    public Integer getStatusCode(){
        return statusCode;
    }
}

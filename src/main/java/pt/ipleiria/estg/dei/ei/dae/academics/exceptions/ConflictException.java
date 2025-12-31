package pt.ipleiria.estg.dei.ei.dae.academics.exceptions;

public class ConflictException extends RuntimeException  {
    public ConflictException(String message) {
        super(message);
    }

    public ConflictException(String message, Throwable cause) {
        super(message, cause);
    }
}
package pt.ipleiria.estg.dei.ei.dae.academics.exceptions;

public class UnauthorizedException extends RuntimeException  {
    public UnauthorizedException(String message) {
        super(message);
    }
}
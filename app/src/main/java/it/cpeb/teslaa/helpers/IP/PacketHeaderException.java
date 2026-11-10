package it.cpeb.teslaa.helpers.IP;

public class PacketHeaderException extends Exception {
    private static final long serialVersionUID = 1L;

    public PacketHeaderException(String message) {
        super(message);
    }

    public PacketHeaderException(String message, Throwable throwable) {
        super(message, throwable);
    }
}
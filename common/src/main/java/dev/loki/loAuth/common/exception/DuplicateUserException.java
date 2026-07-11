package dev.loki.loAuth.common.exception;

public final class DuplicateUserException extends RuntimeException {
    public DuplicateUserException(String msg) {
        super(msg);
    }
}
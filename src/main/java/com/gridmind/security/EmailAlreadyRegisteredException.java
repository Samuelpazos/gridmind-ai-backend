package com.gridmind.security;

public class EmailAlreadyRegisteredException extends RuntimeException {
    public EmailAlreadyRegisteredException() {
        super("Ya existe una cuenta con ese correo.");
    }
}

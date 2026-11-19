package com.gridmind.security;

public class RegistrationDisabledException extends RuntimeException {

    public RegistrationDisabledException() {
        super("El registro de cuentas está deshabilitado.");
    }
}

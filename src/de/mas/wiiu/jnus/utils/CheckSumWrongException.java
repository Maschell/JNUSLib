package de.mas.wiiu.jnus.utils;

import lombok.Getter;

@Getter
public class CheckSumWrongException extends Exception {
    /**
     * 
     */
    private static final long serialVersionUID = 5781223264453732269L;
    private final byte[] givenHash;
    private final byte[] expectedHash;

    public CheckSumWrongException(String string, byte[] given, byte[] expected) {
        super(string);
        this.givenHash = given;
        this.expectedHash = expected;

    }
}

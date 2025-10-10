package com.springAI.utilities;

import java.util.Arrays;
import lombok.Getter;

@Getter
public enum VerbLevel {
    MANAGE(4),
    USE(3),
    READ(2),
    INSPECT(1);

    private final int level;

    VerbLevel(int level) {
        this.level = level;
    }

    public static VerbLevel fromString(String value) {
        return Arrays.stream(values())
                .filter(v -> v.name().equalsIgnoreCase(value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown verb: " + value));
    }
}



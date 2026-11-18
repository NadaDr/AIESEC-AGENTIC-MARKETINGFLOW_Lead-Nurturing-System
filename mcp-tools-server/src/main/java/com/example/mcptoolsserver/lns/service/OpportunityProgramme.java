package com.example.mcptoolsserver.lns.service;

import java.text.Normalizer;
import java.util.Locale;
import java.util.Optional;

public enum OpportunityProgramme {

    OGV("OGV", "Global Volunteer"),
    OGTA("OGTa", "Global Talent"),
    OGTE("OGTe", "Global Teacher");

    private final String code;

    private final String displayName;

    OpportunityProgramme(String code, String displayName) {
        this.code = code;
        this.displayName = displayName;
    }

    public String code() { return code; }

    public String displayName() { return displayName; }

    public static Optional<OpportunityProgramme> fromApi(String shortName, String displayName) {
        String shortValue = normalize(shortName);

        String displayValue = normalize(displayName);

        for (OpportunityProgramme programme : values()) {

            if (programme.matches(shortValue) || programme.matches(displayValue)) {
                return Optional.of(programme);

            }
        }
        return Optional.empty();

    }

    private boolean matches(String value) {
        return switch (this) {

            case OGV -> value.equals("OGV")
                    || value.equals("GV")
                    || value.equals("GLOBALVOLUNTEER");

            case OGTA -> value.equals("OGTA")
                    || value.equals("GTA")
                    || value.equals("GLOBALTALENT");
            case OGTE -> value.equals("OGTE")
                    || value.equals("GTE")
                    || value.equals("GLOBALTEACHER");
        };
    }

    private static String normalize(String value) {
        if (value == null) return "";

        String normalized = Normalizer.normalize(value, Normalizer.Form.NFD)

                .replaceAll("\\p{M}", "");

        return normalized.replaceAll("[^A-Za-z0-9]", "").toUpperCase(Locale.ROOT);

    }
}

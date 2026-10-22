package dev.salmonllama.fsbot.endpoints.scapefashion;

import java.util.Optional;

// Source: https://github.com/ncpierson/fashionscape/blob/master/api/data/tools/slot.js
public enum ScapeFashionSlotOsrs {
    AMMUNITION("ammunition"),
    BODY("body"),
    CAPE("cape"),
    FEET("feet"),
    HAND("hand"),
    HEAD("head"),
    LEG("leg"),
    NECK("neck"),
    RING("ring"),
    SHIELD("shield"),
    WEAPON("weapon");

    private final String value;

    ScapeFashionSlotOsrs(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static Optional<ScapeFashionSlotOsrs> matches(String s) {
        try {
            var slot = ScapeFashionSlotOsrs.valueOf(s);
            return Optional.of(slot);
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}

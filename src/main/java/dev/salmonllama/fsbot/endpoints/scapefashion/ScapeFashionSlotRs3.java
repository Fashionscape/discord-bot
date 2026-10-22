package dev.salmonllama.fsbot.endpoints.scapefashion;

import java.util.Optional;

// Source: https://github.com/ncpierson/fashionscape/blob/master/api/data/tools/slot.js
public enum ScapeFashionSlotRs3 {
    AMMUNITION("ammunition"),
    BACK("back"),
    FEET("feet"),
    HAND("hand"),
    HEAD("head"),
    LEG("leg"),
    MAIN_HAND("main_hand"),
    NECK("neck"),
    OFF_HAND("off-hand"),
    POCKET("pocket"),
    RING("ring"),
    SIGIL("sigil"),
    TORSO("torso"),
    TWO_HANDED("two-handed");

    private final String value;

    ScapeFashionSlotRs3(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static Optional<ScapeFashionSlotRs3> matches(String s) {
        try {
            var slot = ScapeFashionSlotRs3.valueOf(s);
            return Optional.of(slot);
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}

package com.mapswithme.maps;

public enum MapSource {
    ORGANIC_MAPS(0),
    HIKINGBOOK_PRO_MAPS(1);

    private final int value;
    private MapSource(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}

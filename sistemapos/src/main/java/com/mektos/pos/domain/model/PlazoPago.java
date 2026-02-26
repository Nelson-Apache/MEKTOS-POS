package com.mektos.pos.domain.model;

public enum PlazoPago {
    QUINCE_DIAS(15),
    TREINTA_DIAS(30);

    private final int dias;

    PlazoPago(int dias) {
        this.dias = dias;
    }

    public int getDias() {
        return dias;
    }
}

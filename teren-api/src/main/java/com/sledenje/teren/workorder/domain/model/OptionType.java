package com.sledenje.teren.workorder.domain.model;

/**
 * Priključena opcija naprave. Vrednosti se ujemajo s Postgres enumom "OptionType".
 */
public enum OptionType {
    DIN1,
    DIN2,
    DIN3,
    DIN4,
    DIN5,
    ANI1,
    ANI2,
    ANI3,
    ALL_CAN,
    FMSCAN,
    TACHO,
    WIRE_TEMP1,
    WIRE_TEMP2,
    WIRE_TEMP3,
    ID_KEY,
    RFID_125,
    RFID_1356,
    BUZZER
}

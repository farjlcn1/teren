package com.sledenje.teren.workorder.domain.model;

/**
 * Sprememba enega polja pri urejanju naloga (vrednosti so niz, število ali {@code null}).
 */
public record ValueChange(Object from, Object to) {
}

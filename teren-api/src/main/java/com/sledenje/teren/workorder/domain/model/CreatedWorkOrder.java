package com.sledenje.teren.workorder.domain.model;

/**
 * @param ident številka naloga oblike DDMMLLLLNNN (dan naloga in zaporedna številka tega dne)
 */
public record CreatedWorkOrder(String id, String ident) {
}

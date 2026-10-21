package com.sledenje.teren.client.infrastructure.util;

import com.sledenje.teren.client.domain.model.Client;
import com.sledenje.teren.client.domain.model.ClientSyncConfig;
import com.sledenje.teren.client.domain.model.ClientUpdate;
import com.sledenje.teren.client.domain.model.NewClient;
import com.sledenje.teren.client.presentation.dto.ClientDto;
import com.sledenje.teren.client.presentation.dto.ClientSyncConfigDto;
import com.sledenje.teren.client.presentation.dto.CreateClientRequest;
import com.sledenje.teren.client.presentation.dto.ImportClientsRequest;
import com.sledenje.teren.client.presentation.dto.UpdateClientRequest;
import java.util.List;

/**
 * Pretvorbe med DTO-ji presentation nivoja in domenskim modelom stranke.
 */
public final class ClientDtoConverter {

    private ClientDtoConverter() {
    }

    public static ClientDto toDto(Client client) {
        return new ClientDto(
                client.id(),
                client.name(),
                client.address(),
                client.contactInfo(),
                client.source(),
                client.externalId(),
                client.createdAt(),
                client.updatedAt());
    }

    /**
     * Prazna neobvezna polja shrani kot {@code null}, tako kot obrazec v aplikaciji Teren.
     */
    public static NewClient toNewClient(CreateClientRequest request) {
        return new NewClient(request.name().trim(), blankToNull(request.address()), blankToNull(request.contactInfo()));
    }

    public static List<NewClient> toNewClients(ImportClientsRequest request) {
        return request.clients().stream().map(ClientDtoConverter::toNewClient).toList();
    }

    public static ClientUpdate toClientUpdate(UpdateClientRequest request) {
        return new ClientUpdate(request.name().trim(), blankToNull(request.address()), blankToNull(request.contactInfo()));
    }

    public static ClientSyncConfigDto toDto(ClientSyncConfig syncConfig) {
        return new ClientSyncConfigDto(
                syncConfig.apiUrl(),
                syncConfig.hasToken(),
                syncConfig.lastSyncAt(),
                syncConfig.lastSyncStatus());
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

}

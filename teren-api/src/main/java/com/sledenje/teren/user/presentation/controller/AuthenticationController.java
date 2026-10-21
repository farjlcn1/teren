package com.sledenje.teren.user.presentation.controller;

import com.sledenje.teren.user.domain.service.AuthenticationService;
import com.sledenje.teren.user.infrastructure.util.UserDtoConverter;
import com.sledenje.teren.user.presentation.dto.LoginRequest;
import com.sledenje.teren.user.presentation.dto.UserDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Prijava", description = "Preverjanje emaila in gesla; sejo ustvari aplikacija Teren")
public class AuthenticationController {

    private final AuthenticationService authenticationService;

    public AuthenticationController(AuthenticationService authenticationService) {
        this.authenticationService = authenticationService;
    }

    @PostMapping("/login")
    @Operation(summary = "Preverjanje prijave",
            description = "Vrne uporabnika, če sta email in geslo pravilna in je uporabnik aktiven.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Prijava uspela"),
            @ApiResponse(responseCode = "401", description = "Napačen email ali geslo (ali onemogočen uporabnik)",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<UserDto> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(UserDtoConverter.toDto(
                authenticationService.authenticate(request.email(), request.password())));
    }

}

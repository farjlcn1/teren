package com.sledenje.teren.user.presentation.controller;

import com.sledenje.teren.user.domain.model.User;
import com.sledenje.teren.user.domain.service.UserService;
import com.sledenje.teren.user.infrastructure.util.UserDtoConverter;
import com.sledenje.teren.user.presentation.dto.ChangePasswordRequest;
import com.sledenje.teren.user.presentation.dto.CreateUserRequest;
import com.sledenje.teren.user.presentation.dto.UpdateUserAccessRequest;
import com.sledenje.teren.user.presentation.dto.UserDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/v1/users")
@Tag(name = "Uporabniki", description = "Uporabniki, pravice in gesla")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    @Operation(summary = "Seznam uporabnikov", description = "Vrne vse uporabnike v vrstnem redu vnosa.")
    public ResponseEntity<List<UserDto>> getAllUsers() {
        return ResponseEntity.ok(userService.findAllUsers().stream().map(UserDtoConverter::toDto).toList());
    }

    @GetMapping("/{userId}")
    @Operation(summary = "Uporabnik s pravicami",
            description = "Aplikacija ga kliče ob vsakem zahtevku, da odvzem pravic ali onemogočenje velja takoj.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Uporabnik najden"),
            @ApiResponse(responseCode = "404", description = "Uporabnik s tem ID-jem ne obstaja",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<UserDto> getUserById(@PathVariable String userId) {
        return ResponseEntity.ok(UserDtoConverter.toDto(userService.findUserById(userId)));
    }

    @PostMapping
    @Operation(summary = "Vnos uporabnika", description = "Nov uporabnik je aktiven in nima pravic.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Uporabnik dodan"),
            @ApiResponse(responseCode = "400", description = "Manjka ali neveljaven email ali ime",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "409", description = "Email že uporablja drug uporabnik",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "422", description = "Geslo ne ustreza pravilom",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<UserDto> createUser(@Valid @RequestBody CreateUserRequest request) {
        User createdUser = userService.createUser(UserDtoConverter.toNewUser(request));
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{userId}")
                .buildAndExpand(createdUser.id())
                .toUri();
        return ResponseEntity.created(location).body(UserDtoConverter.toDto(createdUser));
    }

    @PutMapping("/{userId}/access")
    @Operation(summary = "Aktivnost in pravice", description = "Nastavi aktivnost in vse pravice hkrati.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Pravice shranjene"),
            @ApiResponse(responseCode = "400", description = "Manjka katera od pravic",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Uporabnik s tem ID-jem ne obstaja",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<UserDto> updateAccess(@PathVariable String userId,
            @Valid @RequestBody UpdateUserAccessRequest request) {
        User updatedUser = userService.updateAccess(userId, request.isActive(), UserDtoConverter.toPermissions(request));
        return ResponseEntity.ok(UserDtoConverter.toDto(updatedUser));
    }

    @PutMapping("/{userId}/password")
    @Operation(summary = "Nastavitev novega gesla")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Geslo spremenjeno"),
            @ApiResponse(responseCode = "404", description = "Uporabnik s tem ID-jem ne obstaja",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "422", description = "Geslo ne ustreza pravilom",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<Void> changePassword(@PathVariable String userId,
            @Valid @RequestBody ChangePasswordRequest request) {
        userService.changePassword(userId, request.password());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{userId}")
    @Operation(summary = "Brisanje uporabnika")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Uporabnik izbrisan"),
            @ApiResponse(responseCode = "404", description = "Uporabnik s tem ID-jem ne obstaja",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "409", description = "Uporabnik ima delovne naloge, urejanja ali dogodke v planu",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<Void> deleteUser(@PathVariable String userId) {
        userService.deleteUser(userId);
        return ResponseEntity.noContent().build();
    }

}

package com.trajetoria.dev.dto;

import java.time.LocalDate;

public record ClienteRequest (
        String nome,
        String email,
        String cpf,
        String senha,
        LocalDate dataNascimento
) {}

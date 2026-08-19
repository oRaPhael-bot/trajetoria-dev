package com.trajetoria.dev.dto;

import com.trajetoria.dev.model.Cliente;
import com.trajetoria.dev.model.StatusCliente;

import java.time.LocalDate;

public record ClienteResponse (
        Long id,
        String nome,
        String email,
        String cpf,
        LocalDate dataNascimento,
        StatusCliente status,
        String criadoEm
) {}

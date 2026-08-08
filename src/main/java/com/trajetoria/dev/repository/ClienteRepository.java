package com.trajetoria.dev.repository;

import com.trajetoria.dev.model.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClienteRepository extends JpaRepository<Cliente, Long> {
    //o JpaRepository faz o trabalho
}

package com.trajetoria.dev.controller;

import com.trajetoria.dev.dto.ClienteRequest;
import com.trajetoria.dev.dto.ClienteResponse;
import com.trajetoria.dev.dto.SenhaUpdate;
import com.trajetoria.dev.service.ClienteService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/clientes")
public class ClienteController {

    private final ClienteService service;

    public ClienteController(ClienteService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<ClienteResponse> salvar(@RequestBody ClienteRequest request) {
        ClienteResponse salvo = service.salvar(request);
        return ResponseEntity.status(201).body(salvo);
    }

    @GetMapping
    public ResponseEntity<List<ClienteResponse>> listarTodos() {
        List<ClienteResponse> lista = service.listarTodos();
        return ResponseEntity.ok(lista);
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ClienteResponse> inativar(@PathVariable Long id) {
        ClienteResponse atualizado = service.inativarCliente(id);
        if (atualizado != null) {
            return ResponseEntity.ok(atualizado);
        }
        return ResponseEntity.notFound().build();
    }

    // Novo Endpoint: A Rota exclusiva e blindada para troca de senhas
    @PatchMapping("/{id}/alterar-senha")
    public ResponseEntity<ClienteResponse> alterarSenha(@PathVariable Long id, @RequestBody SenhaUpdate senhaUpdate) {
        ClienteResponse atualizado = service.alterarSenha(id, senhaUpdate);
        if (atualizado != null) {
            return ResponseEntity.ok(atualizado);
        }
        return ResponseEntity.notFound().build();
    }
}
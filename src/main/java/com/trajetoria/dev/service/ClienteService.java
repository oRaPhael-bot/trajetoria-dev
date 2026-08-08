package com.trajetoria.dev.service;

import com.trajetoria.dev.model.Cliente;
import com.trajetoria.dev.repository.ClienteRepository;
import org.springframework.stereotype.Service;

@Service
public class ClienteService {

    private final ClienteRepository repository;

    public ClienteService(ClienteRepository repository) {
        this.repository = repository;
    }

    public Cliente salvar(Cliente cliente) {
        return repository.save(cliente);
    }

    public Cliente buscaPorId(Long id) {
        return repository.findById(id).orElse(null);
    }

    public void deletar(Long id) {
        repository.deleteById(id);
    }

    public Cliente atualizar(Long id, Cliente clienteAtualizado) {
        Cliente clienteExistente = repository.findById(id).orElse(null);

        if (clienteExistente != null) {
            clienteExistente.setNome(clienteAtualizado.getNome());
            clienteExistente.setCpf(clienteAtualizado.getCpf());

            return repository.save(clienteExistente);
        }
        return null;
    }
}
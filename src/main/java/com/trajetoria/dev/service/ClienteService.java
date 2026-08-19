package com.trajetoria.dev.service;

import com.trajetoria.dev.dto.ClienteRequest;
import com.trajetoria.dev.dto.ClienteResponse;
import com.trajetoria.dev.dto.SenhaUpdate;
import com.trajetoria.dev.model.Cliente;
import com.trajetoria.dev.model.StatusCliente;
import com.trajetoria.dev.repository.ClienteRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ClienteService {

    private final ClienteRepository repository;

    public ClienteService(ClienteRepository repository) {
        this.repository = repository;
    }

    public ClienteResponse salvar(ClienteRequest request) {
        int idade = Period.between(request.dataNascimento(), LocalDate.now()).getYears();
        if (idade < 18) {
            throw new IllegalArgumentException("Cadastro bloqueado: O usuário deve ter pelo menos 18 anos.");
        }

        Cliente cliente = new Cliente();
        cliente.setNome(request.nome());
        cliente.setEmail(request.email());
        cliente.setCpf(request.cpf());
        cliente.setSenha(request.senha());
        cliente.setDataNascimento(request.dataNascimento());

        cliente.setStatus(StatusCliente.ATIVO);
        cliente.setCriadoEm(LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));

        Cliente salvo = repository.save(cliente);
        return converterParaResponse(salvo);
    }

    public List<ClienteResponse> listarTodos() {
        return repository.findAll().stream()
                .filter(cliente -> cliente.getStatus() == StatusCliente.ATIVO)
                .map(this::converterParaResponse)
                .collect(Collectors.toList());
    }

    public ClienteResponse inativarCliente(Long id) {
        Cliente cliente = repository.findById(id).orElse(null);
        if (cliente != null) {
            cliente.setStatus(StatusCliente.INATIVO);
            Cliente atualizado = repository.save(cliente);
            return converterParaResponse(atualizado);
        }
        return null;
    }

    public ClienteResponse alterarSenha(Long id, SenhaUpdate senhaUpdate) {
        Cliente cliente = repository.findById(id).orElse(null);
        if (cliente != null) {
            cliente.setSenha(senhaUpdate.novaSenha());
            Cliente atualizado = repository.save(cliente);
            return converterParaResponse(atualizado);
        }
        return null;
    }

    private ClienteResponse converterParaResponse(Cliente cliente) {
        return new ClienteResponse(
                cliente.getId(),
                cliente.getNome(),
                cliente.getEmail(),
                cliente.getCpf(),
                cliente.getDataNascimento(),
                cliente.getStatus(),
                cliente.getCriadoEm()
        );
    }
}
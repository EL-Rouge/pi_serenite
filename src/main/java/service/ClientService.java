package service;

import models.Client;
import repository.ClientRepository;

import java.sql.SQLException;

public class ClientService {

    private final ClientRepository repository = new ClientRepository();

    public Client getById(long id) throws SQLException {
        Client c = repository.findById(id);
        if (c == null)
            throw new IllegalArgumentException("Client not found with id: " + id);
        return c;
    }
}
package com.tfg.backend.service;

import com.tfg.backend.dto.ConnectionDTO;
import com.tfg.backend.dto.UserDTO;
import com.tfg.backend.model.Connection;
import com.tfg.backend.repository.ConnectionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ConnectionService {

    private final ConnectionRepository connectionRepository;

    // Enrichment for a single UserDTO instance
    public void enrichWithConnection(UserDTO user) {
        if (user == null || user.getUsername() == null) return;

        connectionRepository.findById(user.getUsername()).ifPresentOrElse(
                conn -> {
                    // User has a record in Mongo: map real data
                    user.setConnection(new ConnectionDTO(
                            conn.isOnline(),
                            conn.isOnline() ? conn.getLastConnected() : conn.getLastDisconnected(),
                            conn.getLastSessionDurationSeconds()
                    ));
                },
                () -> {
                    // User has no record in Mongo: explicitly set as offline to remove null ambiguity
                    user.setConnection(new ConnectionDTO(false, null, 0L));
                }
        );
    }

    // Enrichment for a UserDTO list (avoids N+1 problem)
    public void enrichWithConnections(List<UserDTO> users) {
        if (users == null || users.isEmpty()) return;

        Set<String> usernames = users.stream()
                .map(UserDTO::getUsername)
                .collect(Collectors.toSet());

        List<Connection> connections = connectionRepository.findAllById(usernames);

        Map<String, Connection> connMap = connections.stream()
                .collect(Collectors.toMap(Connection::getUsername, c -> c));

        for (UserDTO user : users) {
            Connection conn = connMap.get(user.getUsername());

            if (conn != null) {
                // User has a record in Mongo: map real data
                user.setConnection(new ConnectionDTO(
                        conn.isOnline(),
                        conn.isOnline() ? conn.getLastConnected() : conn.getLastDisconnected(),
                        conn.getLastSessionDurationSeconds()
                ));
            } else {
                // User has no record in Mongo: explicitly set as offline to remove null ambiguity
                user.setConnection(new ConnectionDTO(false, null, 0L));
            }
        }
    }
}
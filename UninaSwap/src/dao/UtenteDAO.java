package dao;

import java.sql.*;
import model.Utente;

public class UtenteDAO {
    private Connection conn;

    public UtenteDAO(Connection conn) {
        this.conn = conn;
    }

    public Utente login(String email, String password) throws SQLException {
        String sql = "SELECT * FROM utente WHERE email = ? AND password = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email);
            ps.setString(2, password);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return new Utente(
                    rs.getString("matricola"),
                    rs.getString("nome"),
                    rs.getString("cognome"),
                    rs.getString("email"),
                    rs.getString("password"),
                    rs.getString("universita")
                );
            }
        }
        return null;
    }

    public Utente getUtenteByMatricola(String matricola) throws SQLException {
        String sql = "SELECT * FROM utente WHERE matricola = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, matricola);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return new Utente(
                    rs.getString("matricola"),
                    rs.getString("nome"),
                    rs.getString("cognome"),
                    rs.getString("email"),
                    rs.getString("password"),
                    rs.getString("universita")
                );
            }
        }
        return null;
    }

    public boolean creaUtente(Utente utente) throws SQLException {
        String sql = "INSERT INTO utente (matricola, nome, cognome, email, password, universita) VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, utente.getMatricola());
            ps.setString(2, utente.getNome());
            ps.setString(3, utente.getCognome());
            ps.setString(4, utente.getEmail());
            ps.setString(5, utente.getPassword());
            ps.setString(6, utente.getUniversita());
            return ps.executeUpdate() == 1;
        }
    }

    public boolean aggiornaUtente(Utente utente) throws SQLException {
        String sql = "UPDATE utente SET nome = ?, cognome = ?, email = ?, password = ?, universita = ? WHERE matricola = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, utente.getNome());
            ps.setString(2, utente.getCognome());
            ps.setString(3, utente.getEmail());
            ps.setString(4, utente.getPassword());
            ps.setString(5, utente.getUniversita());
            ps.setString(6, utente.getMatricola());
            return ps.executeUpdate() == 1;
        }
    }

    public boolean eliminaUtente(String matricola) throws SQLException {
        String sql = "DELETE FROM utente WHERE matricola = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, matricola);
            return ps.executeUpdate() == 1;
        }
    }
}

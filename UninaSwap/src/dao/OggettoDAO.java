package dao;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import model.Oggetto;

public class OggettoDAO {
    private final Connection conn;

    public OggettoDAO(Connection conn) { this.conn = conn; }

    public boolean creaOggetto(Oggetto oggetto, String matricola) throws SQLException {
        String last = getLastCodiceOggetto(), prefix = "O";
        int nextNum = 1;
        if (last != null && last.startsWith(prefix)) {
            String numPart = last.substring(prefix.length());
            try { nextNum = Integer.parseInt(numPart) + 1; }
            catch (NumberFormatException e) { throw new SQLException("Errore parsing codice oggetto: " + numPart, e); }
        }
        oggetto.setCodiceOggetto(String.format(prefix + "%06d", nextNum));

        String sql = "INSERT INTO oggetto (codiceoggetto, nome, descrizione, categoria, matricola, codiceannuncio) VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, oggetto.getCodiceOggetto());
            ps.setString(2, oggetto.getNome());
            ps.setString(3, oggetto.getDescrizione());
            ps.setString(4, oggetto.getCategoria());
            ps.setString(5, matricola);
            ps.setString(6, oggetto.getCodiceAnnuncio());
            return ps.executeUpdate() == 1;
        }
    }

    public void aggiornaCodiceAnnuncioOggetto(String codiceOggetto, String codiceAnnuncio) throws SQLException {
        String sql = "UPDATE oggetto SET codiceannuncio = ? WHERE codiceoggetto = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) { ps.setString(1, codiceAnnuncio); ps.setString(2, codiceOggetto); ps.executeUpdate(); }
    }

    public boolean modificaOggetto(Oggetto oggetto) throws SQLException {
        String sql = "UPDATE oggetto SET nome = ?, descrizione = ?, categoria = ? WHERE codiceoggetto = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, oggetto.getNome());
            ps.setString(2, oggetto.getDescrizione());
            ps.setString(3, oggetto.getCategoria());
            ps.setString(4, oggetto.getCodiceOggetto());
            return ps.executeUpdate() == 1;
        }
    }

    public Oggetto getOggettoByCodice(String codiceOggetto) throws SQLException {
        String sql = "SELECT * FROM oggetto WHERE codiceoggetto = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, codiceOggetto);
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? extractOggetto(rs) : null; }
        }
    }

    public List<Oggetto> getOggettiByAnnuncio(String codiceAnnuncio) throws SQLException {
        String sql = "SELECT * FROM oggetto WHERE codiceannuncio = ?";
        List<Oggetto> oggetti = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, codiceAnnuncio);
            try (ResultSet rs = ps.executeQuery()) { while (rs.next()) oggetti.add(extractOggetto(rs)); }
        }
        return oggetti;
    }

    public List<Oggetto> getOggettiByMatricola(String matricola) throws SQLException {
        String sql = "SELECT * FROM oggetto WHERE matricola = ?";
        List<Oggetto> oggetti = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, matricola);
            try (ResultSet rs = ps.executeQuery()) { while (rs.next()) oggetti.add(extractOggetto(rs)); }
        }
        return oggetti;
    }

    public boolean aggiornaOggetto(Oggetto oggetto) throws SQLException {
        String sql = "UPDATE oggetto SET nome = ?, descrizione = ?, categoria = ?, codiceannuncio = ? WHERE codiceoggetto = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, oggetto.getNome());
            ps.setString(2, oggetto.getDescrizione());
            ps.setString(3, oggetto.getCategoria());
            ps.setString(4, oggetto.getCodiceAnnuncio());
            ps.setString(5, oggetto.getCodiceOggetto());
            return ps.executeUpdate() == 1;
        }
    }

    public boolean eliminaOggetto(String codiceOggetto) throws SQLException {
        String sql = "DELETE FROM oggetto WHERE codiceoggetto = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) { ps.setString(1, codiceOggetto); return ps.executeUpdate() == 1; }
    }

    public String getLastCodiceOggetto() throws SQLException {
        String sql = "SELECT MAX(codiceoggetto) AS maxcode FROM oggetto WHERE codiceoggetto LIKE 'O%'";
        try (PreparedStatement ps = conn.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getString("maxcode") : null;
        }
    }

    private Oggetto extractOggetto(ResultSet rs) throws SQLException {
        return new Oggetto(
            rs.getString("codiceoggetto"),
            rs.getString("nome"),
            rs.getString("descrizione"),
            rs.getString("categoria"),
            rs.getString("codiceannuncio")
        );
    }

    public boolean aggiornaOggettoMatricola(String codiceOggetto, String nuovaMatricola) throws SQLException {
        String sql = "UPDATE oggetto SET matricola = ? WHERE codiceoggetto = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) { ps.setString(1, nuovaMatricola); ps.setString(2, codiceOggetto); return ps.executeUpdate() == 1; }
    }

    public boolean rimuoviAssociazioneAnnuncio(String codiceOggetto) throws SQLException {
        String sql = "UPDATE oggetto SET codiceannuncio = NULL WHERE codiceoggetto = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) { ps.setString(1, codiceOggetto); return ps.executeUpdate() == 1; }
    }
}

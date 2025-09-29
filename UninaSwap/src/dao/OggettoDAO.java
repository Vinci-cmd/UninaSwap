package dao;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import model.Oggetto;

public class OggettoDAO {
    private Connection conn;

    public OggettoDAO(Connection conn) {
        this.conn = conn;
    }

    // CREA NUOVO OGGETTO
    public boolean creaOggetto(Oggetto oggetto) throws SQLException {
        String sql = "INSERT INTO oggetto (codiceoggetto, nome, descrizione, categoria, codiceannuncio) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, oggetto.getCodiceOggetto());
            ps.setString(2, oggetto.getNome());
            ps.setString(3, oggetto.getDescrizione());
            ps.setString(4, oggetto.getCategoria());
            ps.setString(5, oggetto.getCodiceAnnuncio());
            return ps.executeUpdate() == 1;
        }
    }

    // RECUPERA OGGETTO PER CODICE
    public Oggetto getOggettoByCodice(String codiceOggetto) throws SQLException {
        String sql = "SELECT * FROM oggetto WHERE codiceoggetto = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, codiceOggetto);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return new Oggetto(
                    rs.getString("codiceoggetto"),
                    rs.getString("nome"),
                    rs.getString("descrizione"),
                    rs.getString("categoria"),
                    rs.getString("codiceannuncio")
                );
            }
        }
        return null;
    }

    // RECUPERA TUTTI GLI OGGETTI DI UN ANNUNCIO
    public List<Oggetto> getOggettiByAnnuncio(String codiceAnnuncio) throws SQLException {
        String sql = "SELECT * FROM oggetto WHERE codiceannuncio = ?";
        List<Oggetto> oggetti = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, codiceAnnuncio);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                oggetti.add(new Oggetto(
                    rs.getString("codiceoggetto"),
                    rs.getString("nome"),
                    rs.getString("descrizione"),
                    rs.getString("categoria"),
                    rs.getString("codiceannuncio")
                ));
            }
        }
        return oggetti;
    }

    // AGGIORNA OGGETTO
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

    // ELIMINA OGGETTO
    public boolean eliminaOggetto(String codiceOggetto) throws SQLException {
        String sql = "DELETE FROM oggetto WHERE codiceoggetto = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, codiceOggetto);
            return ps.executeUpdate() == 1;
        }
    }
}

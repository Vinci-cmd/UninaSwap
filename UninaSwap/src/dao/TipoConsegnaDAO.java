package dao;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import model.TipoConsegna;

public class TipoConsegnaDAO {
    private Connection conn;

    public TipoConsegnaDAO(Connection conn) {
        this.conn = conn;
    }

    // CREA NUOVA CONSEGNA
    public boolean creaTipoConsegna(TipoConsegna consegna) throws SQLException {
        String sql = "INSERT INTO tipoconsegna (codiceconsegna, sede, descrizione, fasciaoraria, codiceannuncio) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, consegna.getCodiceConsegna());
            ps.setString(2, consegna.getSede());
            ps.setString(3, consegna.getDescrizione());
            ps.setString(4, consegna.getFasciaOraria());
            ps.setString(5, consegna.getCodiceAnnuncio());
            return ps.executeUpdate() == 1;
        }
    }

    // RECUPERA CONSEGNA PER CODICE
    public TipoConsegna getTipoconsegnaByCodice(String codiceConsegna) throws SQLException {
        String sql = "SELECT * FROM tipoconsegna WHERE codiceconsegna = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, codiceConsegna);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return new TipoConsegna(
                    rs.getString("codiceconsegna"),
                    rs.getString("sede"),
                    rs.getString("descrizione"),
                    rs.getString("fasciaoraria"),
                    rs.getString("codiceannuncio")
                );
            }
        }
        return null;
    }

    // RECUPERA TUTTE LE CONSEGNE DI UN ANNUNCIO
    public List<TipoConsegna> getConsegneByAnnuncio(String codiceAnnuncio) throws SQLException {
        String sql = "SELECT * FROM tipoconsegna WHERE codiceannuncio = ?";
        List<TipoConsegna> consegne = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, codiceAnnuncio);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                consegne.add(new TipoConsegna(
                    rs.getString("codiceconsegna"),
                    rs.getString("sede"),
                    rs.getString("descrizione"),
                    rs.getString("fasciaoraria"),
                    rs.getString("codiceannuncio")
                ));
            }
        }
        return consegne;
    }

    // AGGIORNA CONSEGNA
    public boolean aggiornaTipoConsegna(TipoConsegna consegna) throws SQLException {
        String sql = "UPDATE tipoconsegna SET sede = ?, descrizione = ?, fasciaoraria = ?, codiceannuncio = ? WHERE codiceconsegna = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, consegna.getSede());
            ps.setString(2, consegna.getDescrizione());
            ps.setString(3, consegna.getFasciaOraria());
            ps.setString(4, consegna.getCodiceAnnuncio());
            ps.setString(5, consegna.getCodiceConsegna());
            return ps.executeUpdate() == 1;
        }
    }

    // ELIMINA CONSEGNA
    public boolean eliminaTipoConsegna(String codiceConsegna) throws SQLException {
        String sql = "DELETE FROM tipoconsegna WHERE codiceconsegna = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, codiceConsegna);
            return ps.executeUpdate() == 1;
        }
    }
}

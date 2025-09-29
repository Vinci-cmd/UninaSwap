package dao;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import model.Offre;

public class OffreDAO {
    private Connection conn;

    public OffreDAO(Connection conn) {
        this.conn = conn;
    }

    // Crea un'associazione offerta-oggetto
    public boolean creaOffre(Offre offre) throws SQLException {
        String sql = "INSERT INTO offre (codiceofferta, codiceoggetto) VALUES (?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, offre.getCodiceOfferta());
            ps.setString(2, offre.getCodiceOggetto());
            return ps.executeUpdate() == 1;
        }
    }

    // Recupera tutti gli oggetti associati a un'offerta
    public List<Offre> getOggettiByOfferta(String codiceOfferta) throws SQLException {
        List<Offre> lista = new ArrayList<>();
        String sql = "SELECT * FROM offre WHERE codiceofferta = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, codiceOfferta);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                lista.add(new Offre(
                        rs.getString("codiceofferta"),
                        rs.getString("codiceoggetto")
                ));
            }
        }
        return lista;
    }

    // Elimina un'associazione offerta-oggetto
    public boolean eliminaOffre(String codiceOfferta, String codiceOggetto) throws SQLException {
        String sql = "DELETE FROM offre WHERE codiceofferta = ? AND codiceoggetto = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, codiceOfferta);
            ps.setString(2, codiceOggetto);
            return ps.executeUpdate() == 1;
        }
    }


//Associa un oggetto a un'offerta di tipo scambio
public boolean aggiungiOggettoAScambio(String codiceOfferta, String codiceOggetto) throws SQLException {
 String sql = "INSERT INTO offre (codiceofferta, codiceoggetto) VALUES (?, ?)";
 try (PreparedStatement ps = conn.prepareStatement(sql)) {
     ps.setString(1, codiceOfferta);
     ps.setString(2, codiceOggetto);
     return ps.executeUpdate() == 1;
 }
}}
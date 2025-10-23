package dao;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import model.Offre;

public class OffreDAO {
    private final Connection conn;

    public OffreDAO(Connection conn) { this.conn = conn; }

    public boolean creaOffre(Offre offre) throws SQLException {
        return aggiungiOggettoAScambio(offre.getCodiceOfferta(), offre.getCodiceOggetto());
    }

    public boolean aggiungiOggettoAScambio(String codiceOfferta, String codiceOggetto) throws SQLException {
        String sql = "INSERT INTO offre (codiceofferta, codiceoggetto) VALUES (?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, codiceOfferta);
            ps.setString(2, codiceOggetto);
            return ps.executeUpdate() == 1;
        }
    }

    public List<Offre> getOggettiByOfferta(String codiceOfferta) throws SQLException {
        List<Offre> lista = new ArrayList<>();
        String sql = "SELECT * FROM offre WHERE codiceofferta = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, codiceOfferta);
            try (ResultSet rs = ps.executeQuery()) { while (rs.next()) lista.add(extractOffre(rs)); }
        }
        return lista;
    }

    public boolean eliminaOffre(String codiceOfferta, String codiceOggetto) throws SQLException {
        String sql = "DELETE FROM offre WHERE codiceofferta = ? AND codiceoggetto = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, codiceOfferta);
            ps.setString(2, codiceOggetto);
            return ps.executeUpdate() == 1;
        }
    }

    private Offre extractOffre(ResultSet rs) throws SQLException {
        return new Offre(
            rs.getString("codiceofferta"),
            rs.getString("codiceoggetto")
        );
    }
}

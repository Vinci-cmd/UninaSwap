package dao;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import model.Offerta;

public class OffertaDAO {
    private Connection conn;

    public OffertaDAO(Connection conn) {
        this.conn = conn;
    }

    public boolean creaOfferta(Offerta offerta) throws SQLException {
        String sql = "INSERT INTO offerta (codiceofferta, dataofferta, stato, prezzoofferto, tipo, matricola, codiceannuncio) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, offerta.getCodiceOfferta());
            ps.setDate(2, new java.sql.Date(offerta.getDataOfferta().getTime())); // conversione java.util.Date -> java.sql.Date
            ps.setString(3, offerta.getStato());
            if (offerta.getPrezzoOfferto() != null) {
                ps.setDouble(4, offerta.getPrezzoOfferto());
            } else {
                ps.setNull(4, Types.NUMERIC);
            }
            ps.setString(5, offerta.getTipo());
            ps.setString(6, offerta.getMatricola());
            ps.setString(7, offerta.getCodiceAnnuncio());
            return ps.executeUpdate() == 1;
        }
    }

    public boolean aggiornaOfferta(Offerta offerta) throws SQLException {
        // Aggiornabile solo se lo stato è "inviata"
        String sql = "UPDATE offerta SET prezzoofferto = ?, tipo = ?, stato = ? WHERE codiceofferta = ? AND stato = 'inviata'";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            if (offerta.getPrezzoOfferto() != null) {
                ps.setDouble(1, offerta.getPrezzoOfferto());
            } else {
                ps.setNull(1, Types.NUMERIC);
            }
            ps.setString(2, offerta.getTipo());
            ps.setString(3, offerta.getStato());
            ps.setString(4, offerta.getCodiceOfferta());
            return ps.executeUpdate() == 1;
        }
    }

    public boolean eliminaOfferta(String codiceOfferta) throws SQLException {
        // Eliminabile solo se lo stato è "inviata"
        String sql = "DELETE FROM offerta WHERE codiceofferta = ? AND stato = 'inviata'";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, codiceOfferta);
            return ps.executeUpdate() == 1;
        }
    }

    public List<Offerta> getOfferteByUtente(String matricola) throws SQLException {
        List<Offerta> list = new ArrayList<>();
        String sql = "SELECT * FROM offerta WHERE matricola = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, matricola);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(new Offerta(
                    rs.getString("codiceofferta"),
                    rs.getDate("dataofferta"),
                    rs.getString("stato"),
                    rs.getDouble("prezzoofferto"),
                    rs.getString("tipo"),
                    rs.getString("matricola"),
                    rs.getString("codiceannuncio")
                ));
            }
        }
        return list;
    }

    public List<Offerta> getOfferteByAnnuncio(String codiceAnnuncio) throws SQLException {
        List<Offerta> list = new ArrayList<>();
        String sql = "SELECT * FROM offerta WHERE codiceannuncio = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, codiceAnnuncio);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(new Offerta(
                    rs.getString("codiceofferta"),
                    rs.getDate("dataofferta"),
                    rs.getString("stato"),
                    rs.getDouble("prezzoofferto"),
                    rs.getString("tipo"),
                    rs.getString("matricola"),
                    rs.getString("codiceannuncio")
                ));
            }
        }
        return list;
    }
    
    public Offerta getOffertaByCodice(String codiceOfferta) throws SQLException {
        String sql = "SELECT * FROM offerta WHERE codiceofferta = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, codiceOfferta);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return new Offerta(
                    rs.getString("codiceofferta"),
                    rs.getDate("dataofferta"),
                    rs.getString("stato"),
                    rs.getDouble("prezzoofferto"),
                    rs.getString("tipo"),
                    rs.getString("matricola"),
                    rs.getString("codiceannuncio")
                );
            }
        }
        return null;
    }
    
    public boolean aggiungiOggettoAScambio(String codiceOfferta, String codiceOggetto) throws SQLException {
        String sql = "INSERT INTO offre (codiceofferta, codiceoggetto) VALUES (?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, codiceOfferta);
            ps.setString(2, codiceOggetto);
            return ps.executeUpdate() == 1;
        }
    }
    
    public int getTotaleOfferte() throws SQLException {
        String sql = "SELECT COUNT(*) AS totale FROM offerta";
        try (Statement stmt = conn.createStatement()) {
            ResultSet rs = stmt.executeQuery(sql);
            if (rs.next()) return rs.getInt("totale");
        }
        return 0;
    }
    
    public int getTotaleOffertePerTipologia(String tipologia) throws SQLException {
        String sql = "SELECT COUNT(*) AS totale FROM offerta WHERE tipo = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, tipologia);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt("totale");
        }
        return 0;
    }
    
    public int getOfferteAccettatePerTipologia(String tipologia) throws SQLException {
        String sql = "SELECT COUNT(*) AS totale FROM offerta WHERE tipo = ? AND stato = 'accettata'";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, tipologia);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt("totale");
        }
        return 0;
    }
    
    public double[] getStatisticheVenditeAccettate() throws SQLException {
        String sql = "SELECT MIN(prezzoofferto) AS minPrezzo, MAX(prezzoofferto) AS maxPrezzo, AVG(prezzoofferto) AS avgPrezzo "
                   + "FROM offerta WHERE tipo = 'vendita' AND stato = 'accettata'";
        try (Statement stmt = conn.createStatement()) {
            ResultSet rs = stmt.executeQuery(sql);
            if (rs.next()) {
                return new double[] {
                    rs.getDouble("minPrezzo"),
                    rs.getDouble("maxPrezzo"),
                    rs.getDouble("avgPrezzo")
                };
            }
        }
        return new double[] {0, 0, 0};
    }
}

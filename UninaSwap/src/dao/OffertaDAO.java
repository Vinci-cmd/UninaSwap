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
        String last = getLastCodiceOfferta(); // es. "OFF00017"
        String prefix = "OFF";
        int nextNum = 1;
        if (last != null && last.startsWith(prefix)) {
            String numPart = last.substring(prefix.length());
            try {
                nextNum = Integer.parseInt(numPart) + 1;
            } catch(NumberFormatException e) {
                throw new SQLException("Errore parsing codice offerta: " + numPart, e);
            }
        }
        String newCode = String.format(prefix + "%05d", nextNum);
        offerta.setCodiceOfferta(newCode);

        String sql = "INSERT INTO offerta (codiceofferta, tipo, stato, data, prezzoofferto, matricola, codiceannuncio, messaggio) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, offerta.getCodiceOfferta());
            ps.setString(2, offerta.getTipo());
            ps.setString(3, offerta.getStato());
            ps.setDate(4, new Date(System.currentTimeMillis()));
            if (offerta.getPrezzoOfferto() != null)
                ps.setDouble(5, offerta.getPrezzoOfferto());
            else
                ps.setNull(5, java.sql.Types.DOUBLE);
            ps.setString(6, offerta.getMatricola());
            ps.setString(7, offerta.getCodiceAnnuncio());
            ps.setString(8, offerta.getMessaggio()); // questo prende null se non usato
            int rows = ps.executeUpdate();
            return rows == 1;
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
                Double prezzoOfferto = null;
                Object prezzoObj = rs.getObject("prezzoofferto");
                if (prezzoObj != null) {
                    prezzoOfferto = rs.getDouble("prezzoofferto");
                }
                list.add(new Offerta(
                    rs.getString("codiceofferta"),
                    rs.getString("codiceannuncio"),
                    rs.getString("matricola"),
                    rs.getString("tipo"),
                    prezzoOfferto,
                    rs.getString("stato"),
                    rs.getDate("data")
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
                Double prezzoOfferto = null;
                Object prezzoObj = rs.getObject("prezzoofferto");
                if (prezzoObj != null) {
                    prezzoOfferto = rs.getDouble("prezzoofferto");
                }
                list.add(new Offerta(
                    rs.getString("codiceofferta"),
                    rs.getString("codiceannuncio"),
                    rs.getString("matricola"),
                    rs.getString("tipo"),
                    prezzoOfferto,
                    rs.getString("stato"),
                    rs.getDate("dataofferta")
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
                Double prezzoOfferto = null;
                Object prezzoObj = rs.getObject("prezzoofferto");
                if (prezzoObj != null) {
                    prezzoOfferto = rs.getDouble("prezzoofferto");
                }
                return new Offerta(
                    rs.getString("codiceofferta"),
                    rs.getString("codiceannuncio"),
                    rs.getString("matricola"),
                    rs.getString("tipo"),
                    prezzoOfferto,
                    rs.getString("stato"),
                    rs.getDate("dataofferta")
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
    
    // Invia offerta semplice (vendita/regalo)
    public boolean inviaOfferta(Offerta offerta) throws SQLException {
        String sql = "INSERT INTO offerta (codiceofferta, codiceannuncio, matricola, tipo, prezzoofferto, stato, dataofferta) VALUES (?, ?, ?, ?, ?, ?, ?)";
        
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, offerta.getCodiceOfferta());
            stmt.setString(2, offerta.getCodiceAnnuncio());
            stmt.setString(3, offerta.getMatricola());
            stmt.setString(4, offerta.getTipo());
            if (offerta.getPrezzoOfferto() != null) {
                stmt.setDouble(5, offerta.getPrezzoOfferto());
            } else {
                stmt.setNull(5, java.sql.Types.NUMERIC);
            }
            stmt.setString(6, offerta.getStato());
            stmt.setDate(7, offerta.getDataOfferta());
            
            return stmt.executeUpdate() > 0;
        }
    }

    // Invia offerta con oggetti (per scambi)
    public boolean inviaOffertaConOggetti(Offerta offerta, List<String> codiciOggetti) throws SQLException {
        conn.setAutoCommit(false);
        try {
            // Prima inserisci l'offerta
            if (!inviaOfferta(offerta)) {
                conn.rollback();
                return false;
            }
            
            // Poi inserisci gli oggetti associati
            String sqlOffre = "INSERT INTO offre (codiceofferta, codiceoggetto) VALUES (?, ?)";
            try (PreparedStatement stmt = conn.prepareStatement(sqlOffre)) {
                for (String codiceOggetto : codiciOggetti) {
                    stmt.setString(1, offerta.getCodiceOfferta());
                    stmt.setString(2, codiceOggetto);
                    stmt.addBatch();
                }
                stmt.executeBatch();
            }
            
            conn.commit();
            return true;
        } catch (SQLException e) {
            conn.rollback();
            throw e;
        } finally {
            conn.setAutoCommit(true);
        }
    }

    // Ottiene offerte ricevute sui propri annunci
    public List<Offerta> getOfferteRicevuteByUtente(String matricola) throws SQLException {
        String sql = "SELECT o.* FROM offerta o " +
                     "JOIN annuncio a ON o.codiceannuncio = a.codiceannuncio " +
                     "WHERE a.matricola = ? " +
                     "ORDER BY o.dataofferta DESC";
        
        List<Offerta> offerte = new ArrayList<>();
        
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, matricola);
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                Double prezzoOfferto = null;
                Object prezzoObj = rs.getObject("prezzoofferto");
                if (prezzoObj != null) {
                    prezzoOfferto = rs.getDouble("prezzoofferto");
                }
                
                offerte.add(new Offerta(
                    rs.getString("codiceofferta"),
                    rs.getString("codiceannuncio"),
                    rs.getString("matricola"),
                    rs.getString("tipo"),
                    prezzoOfferto,
                    rs.getString("stato"),
                    rs.getDate("dataofferta")
                ));
            }
        }
        
        return offerte;
    }
    
    // Accetta un'offerta
    public boolean accettaOfferta(String codiceOfferta) throws SQLException {
        String sql = "UPDATE offerta SET stato = 'accettata' WHERE codiceofferta = ? AND stato = 'inviata'";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, codiceOfferta);
            return ps.executeUpdate() == 1;
        }
    }
    
    // Rifiuta un'offerta
    public boolean rifiutaOfferta(String codiceOfferta) throws SQLException {
        String sql = "UPDATE offerta SET stato = 'rifiutata' WHERE codiceofferta = ? AND stato = 'inviata'";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, codiceOfferta);
            return ps.executeUpdate() == 1;
        }
    }
    public String getLastCodiceOfferta() throws SQLException {
        String sql = "SELECT MAX(codiceofferta) AS maxcode FROM offerta WHERE codiceofferta LIKE 'OFF%'";
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return rs.getString("maxcode"); // null se nessun codice OFF%
            }
        }
        return null;
    }
}

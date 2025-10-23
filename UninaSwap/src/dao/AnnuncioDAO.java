package dao;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import model.Annuncio;

public class AnnuncioDAO {
    private final Connection conn;

    public AnnuncioDAO(Connection conn) { this.conn = conn; }

    private Annuncio extractAnnuncio(ResultSet rs) throws SQLException {
        return new Annuncio(
            rs.getString("codiceannuncio"),
            rs.getString("descrizione"),
            rs.getString("categoria"),
            rs.getString("tipologia"),
            rs.getObject("prezzo") != null ? rs.getDouble("prezzo") : null,
            rs.getString("stato"),
            rs.getDate("datapubblicazione"),
            rs.getString("matricola")
        );
    }

    public Annuncio getAnnuncioByCodice(String codice) throws SQLException {
        String sql = "SELECT * FROM annuncio WHERE codiceannuncio = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, codice);
            try (ResultSet rs = ps.executeQuery()) { if (rs.next()) return extractAnnuncio(rs); }
        }
        return null;
    }

    public List<Annuncio> getAnnunciAttivi() throws SQLException {
        String sql = "SELECT * FROM annuncio WHERE stato = 'attivo'";
        List<Annuncio> lista = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) lista.add(extractAnnuncio(rs));
        }
        return lista;
    }

    public List<Annuncio> getAnnunciFiltrati(String categoria, String tipologia) throws SQLException {
        StringBuilder sql = new StringBuilder("SELECT * FROM annuncio WHERE stato = 'attivo'");
        if (categoria != null && !categoria.isEmpty()) sql.append(" AND categoria = ?");
        if (tipologia != null && !tipologia.isEmpty()) sql.append(" AND tipologia = ?");
        List<Annuncio> lista = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            int i = 1;
            if (categoria != null && !categoria.isEmpty()) ps.setString(i++, categoria);
            if (tipologia != null && !tipologia.isEmpty()) ps.setString(i++, tipologia);
            try (ResultSet rs = ps.executeQuery()) { while (rs.next()) lista.add(extractAnnuncio(rs)); }
        }
        return lista;
    }

    public boolean creaAnnuncio(Annuncio annuncio) throws SQLException {
        String last = getLastCodiceAnnuncio(), prefix = "AN";
        int nextNum = 1;
        if (last != null && last.startsWith(prefix)) {
            String numPart = last.substring(prefix.length());
            try { nextNum = Integer.parseInt(numPart) + 1; }
            catch (NumberFormatException e) { System.err.println("Errore parsing codice annuncio: " + numPart); throw e; }
        }
        annuncio.setCodiceAnnuncio(String.format(prefix + "%05d", nextNum));

        String sql = "INSERT INTO annuncio (codiceannuncio, descrizione, categoria, tipologia, prezzo, stato, datapubblicazione, matricola) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, annuncio.getCodiceAnnuncio());
            ps.setString(2, annuncio.getDescrizione());
            ps.setString(3, annuncio.getCategoria());
            ps.setString(4, annuncio.getTipologia());
            if ("vendita".equalsIgnoreCase(annuncio.getTipologia())) ps.setDouble(5, annuncio.getPrezzo()); else ps.setNull(5, Types.DOUBLE);
            ps.setString(6, annuncio.getStato());
            ps.setDate(7, new Date(annuncio.getDataPubblicazione().getTime()));
            ps.setString(8, annuncio.getMatricola());
            int rows = ps.executeUpdate();
            if (!conn.getAutoCommit()) conn.commit();
            return rows == 1;
        }
    }

    public boolean aggiornaAnnuncio(Annuncio annuncio) throws SQLException {
        String sql = "UPDATE annuncio SET descrizione = ?, categoria = ?, tipologia = ?, prezzo = ?, stato = ?, datapubblicazione = ? WHERE codiceannuncio = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, annuncio.getDescrizione());
            ps.setString(2, annuncio.getCategoria());
            ps.setString(3, annuncio.getTipologia());
            if (annuncio.getPrezzo() != null) ps.setDouble(4, annuncio.getPrezzo()); else ps.setNull(4, Types.NUMERIC);
            ps.setString(5, annuncio.getStato());
            ps.setDate(6, annuncio.getDataPubblicazione());
            ps.setString(7, annuncio.getCodiceAnnuncio());
            return ps.executeUpdate() == 1;
        }
    }

    public boolean modificaAnnuncio(Annuncio annuncio) throws SQLException {
        String sql = "UPDATE annuncio SET descrizione = ?, categoria = ?, tipologia = ?, prezzo = ?, stato = ? WHERE codiceannuncio = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, annuncio.getDescrizione());
            ps.setString(2, annuncio.getCategoria());
            ps.setString(3, annuncio.getTipologia());
            if ("vendita".equalsIgnoreCase(annuncio.getTipologia())) ps.setDouble(4, annuncio.getPrezzo()); else ps.setNull(4, Types.DOUBLE);
            ps.setString(5, annuncio.getStato());
            ps.setString(6, annuncio.getCodiceAnnuncio());
            return ps.executeUpdate() == 1;
        }
    }

    public boolean aggiornaStatoAnnuncio(String codiceAnnuncio, String nuovoStato) throws SQLException {
        String sql = "UPDATE annuncio SET stato = ? WHERE codiceannuncio = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) { ps.setString(1, nuovoStato); ps.setString(2, codiceAnnuncio); return ps.executeUpdate() > 0; }
    }

    public boolean eliminaAnnuncio(String codice) throws SQLException {
        String sql = "DELETE FROM annuncio WHERE codiceannuncio = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) { ps.setString(1, codice); return ps.executeUpdate() == 1; }
    }

    public List<Annuncio> getAnnunciByUtente(String matricola) throws SQLException {
        String sql = "SELECT * FROM annuncio WHERE matricola = ?";
        List<Annuncio> lista = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, matricola);
            try (ResultSet rs = ps.executeQuery()) { while (rs.next()) lista.add(extractAnnuncio(rs)); }
        }
        return lista;
    }

    public int getTotaleAnnunci() throws SQLException {
        String sql = "SELECT COUNT(*) FROM annuncio";
        try (PreparedStatement ps = conn.prepareStatement(sql); ResultSet rs = ps.executeQuery()) { return rs.next() ? rs.getInt(1) : 0; }
    }

    public String getLastCodiceAnnuncio() throws SQLException {
        String sql = "SELECT MAX(codiceannuncio) AS maxcode FROM annuncio WHERE codiceannuncio LIKE 'AN%'";
        try (PreparedStatement ps = conn.prepareStatement(sql); ResultSet rs = ps.executeQuery()) { return rs.next() ? rs.getString("maxcode") : null; }
    }
}

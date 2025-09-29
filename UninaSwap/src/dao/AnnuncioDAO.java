package dao;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import model.Annuncio;

public class AnnuncioDAO {
    private Connection conn;

    public AnnuncioDAO(Connection conn) {
        this.conn = conn;
    }

    // Recupera un annuncio specifico per codice
    public Annuncio getAnnuncioByCodice(String codice) throws SQLException {
        String sql = "SELECT * FROM annuncio WHERE codiceannuncio = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, codice);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
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
        }
        return null;
    }

    // Recupera tutti gli annunci attivi
    public List<Annuncio> getAnnunciAttivi() throws SQLException {
        String sql = "SELECT * FROM annuncio WHERE stato = 'attivo'";
        List<Annuncio> lista = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                lista.add(new Annuncio(
                    rs.getString("codiceannuncio"),
                    rs.getString("descrizione"),
                    rs.getString("categoria"),
                    rs.getString("tipologia"),
                    rs.getObject("prezzo") != null ? rs.getDouble("prezzo") : null,
                    rs.getString("stato"),
                    rs.getDate("datapubblicazione"),
                    rs.getString("matricola")
                ));
            }
        }
        return lista;
    }

    // Recupera annunci filtrati per categoria e tipologia (può essere null)
    public List<Annuncio> getAnnunciFiltrati(String categoria, String tipologia) throws SQLException {
        StringBuilder sql = new StringBuilder("SELECT * FROM annuncio WHERE stato = 'attivo'");
        if (categoria != null && !categoria.isEmpty()) {
            sql.append(" AND categoria = ?");
        }
        if (tipologia != null && !tipologia.isEmpty()) {
            sql.append(" AND tipologia = ?");
        }
        List<Annuncio> lista = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            int index = 1;
            if (categoria != null && !categoria.isEmpty()) ps.setString(index++, categoria);
            if (tipologia != null && !tipologia.isEmpty()) ps.setString(index++, tipologia);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                lista.add(new Annuncio(
                    rs.getString("codiceannuncio"),
                    rs.getString("descrizione"),
                    rs.getString("categoria"),
                    rs.getString("tipologia"),
                    rs.getObject("prezzo") != null ? rs.getDouble("prezzo") : null,
                    rs.getString("stato"),
                    rs.getDate("datapubblicazione"),
                    rs.getString("matricola")
                ));
            }
        }
        return lista;
    }

    // Crea un nuovo annuncio
    public boolean creaAnnuncio(Annuncio annuncio) throws SQLException {
        String sql = "INSERT INTO annuncio (codiceannuncio, descrizione, categoria, tipologia, prezzo, stato, datapubblicazione, matricola) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, annuncio.getCodiceAnnuncio());
            ps.setString(2, annuncio.getDescrizione());
            ps.setString(3, annuncio.getCategoria());
            ps.setString(4, annuncio.getTipologia());
            if (annuncio.getPrezzo() != null) {
                ps.setDouble(5, annuncio.getPrezzo());
            } else {
                ps.setNull(5, Types.NUMERIC);
            }
            ps.setString(6, annuncio.getStato());
            ps.setDate(7, annuncio.getDataPubblicazione());
            ps.setString(8, annuncio.getMatricola());
            return ps.executeUpdate() == 1;
        }
    }

    // Aggiorna un annuncio esistente
    public boolean aggiornaAnnuncio(Annuncio annuncio) throws SQLException {
        String sql = "UPDATE annuncio SET descrizione = ?, categoria = ?, tipologia = ?, prezzo = ?, stato = ?, datapubblicazione = ? WHERE codiceannuncio = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, annuncio.getDescrizione());
            ps.setString(2, annuncio.getCategoria());
            ps.setString(3, annuncio.getTipologia());
            if (annuncio.getPrezzo() != null) {
                ps.setDouble(4, annuncio.getPrezzo());
            } else {
                ps.setNull(4, Types.NUMERIC);
            }
            ps.setString(5, annuncio.getStato());
            ps.setDate(6, annuncio.getDataPubblicazione());
            ps.setString(7, annuncio.getCodiceAnnuncio());
            return ps.executeUpdate() == 1;
        }
    }

    // Elimina un annuncio
    public boolean eliminaAnnuncio(String codice) throws SQLException {
        String sql = "DELETE FROM annuncio WHERE codiceannuncio = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, codice);
            return ps.executeUpdate() == 1;
        }
    }


public List<Annuncio> getAnnunciByUtente(String matricola) throws SQLException {
    String sql = "SELECT * FROM annuncio WHERE matricola = ?";
    List<Annuncio> lista = new ArrayList<>();
    try (PreparedStatement ps = conn.prepareStatement(sql)) {
        ps.setString(1, matricola);
        ResultSet rs = ps.executeQuery();
        while (rs.next()) {
            lista.add(new Annuncio(
                rs.getString("codiceannuncio"),
                rs.getString("descrizione"),
                rs.getString("categoria"),
                rs.getString("tipologia"),
                rs.getObject("prezzo") != null ? rs.getDouble("prezzo") : null,
                rs.getString("stato"),
                rs.getDate("datapubblicazione"),
                rs.getString("matricola")
            ));
        }
    }
    return lista;
}

public int getTotaleAnnunci() throws SQLException {
    String sql = "SELECT COUNT(*) FROM annuncio";
    try (PreparedStatement ps = conn.prepareStatement(sql)) {
        ResultSet rs = ps.executeQuery();
        if (rs.next()) return rs.getInt(1);
    }
    return 0;
}
}
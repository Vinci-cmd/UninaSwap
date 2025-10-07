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

    public boolean creaAnnuncio(Annuncio annuncio) throws SQLException {
        String last = getLastCodiceAnnuncio(); // es. "AN0000042"
        String prefix = "AN";
        int nextNum = 1;
        if (last != null && last.startsWith(prefix)) {
            String numPart = last.substring(prefix.length()); // "0000042"
            try {
                nextNum = Integer.parseInt(numPart) + 1;          // 43
            } catch(NumberFormatException e) {
                System.err.println("Errore parsing codice annuncio: " + numPart);
                throw e;
            }
        }
        String newCode = String.format(prefix + "%05d", nextNum); // "AN0000043"
        annuncio.setCodiceAnnuncio(newCode);

        String sql = "INSERT INTO annuncio (codiceannuncio, descrizione, categoria, tipologia, prezzo, stato, datapubblicazione, matricola) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            System.out.println("DAO.creaAnnuncio: Inserimento annuncio con codice " + newCode);
            ps.setString(1, annuncio.getCodiceAnnuncio());
            ps.setString(2, annuncio.getDescrizione());
            ps.setString(3, annuncio.getCategoria());
            ps.setString(4, annuncio.getTipologia());
            if ("vendita".equalsIgnoreCase(annuncio.getTipologia())) {
                ps.setDouble(5, annuncio.getPrezzo());
            } else {
                ps.setNull(5, java.sql.Types.DOUBLE);
            }
            ps.setString(6, annuncio.getStato());
            ps.setDate(7, new java.sql.Date(annuncio.getDataPubblicazione().getTime()));
            ps.setString(8, annuncio.getMatricola());
            int rows = ps.executeUpdate();
            System.out.println("DAO.creaAnnuncio: Righe inserite = " + rows);
            if (!conn.getAutoCommit()) {
                conn.commit();
                System.out.println("DAO.creaAnnuncio: Commit eseguito manualmente");
            } 
            return rows == 1;
        } catch (SQLException e) {
            System.err.println("DAO.creaAnnuncio: Errore durante inserimento: " + e.getMessage());
            throw e;
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
public boolean modificaAnnuncio(Annuncio annuncio) throws SQLException {
    String sql = "UPDATE annuncio SET descrizione = ?, categoria = ?, tipologia = ?, prezzo = ?, stato = ? WHERE codiceannuncio = ?";
    try (PreparedStatement ps = conn.prepareStatement(sql)) {
        ps.setString(1, annuncio.getDescrizione());
        ps.setString(2, annuncio.getCategoria());
        ps.setString(3, annuncio.getTipologia());
        if ("vendita".equalsIgnoreCase(annuncio.getTipologia())) {
            ps.setDouble(4, annuncio.getPrezzo());
        } else {
            ps.setNull(4, java.sql.Types.DOUBLE);
        }
        ps.setString(5, annuncio.getStato());
        ps.setString(6, annuncio.getCodiceAnnuncio());
        return ps.executeUpdate() == 1;
    }
}
public String getLastCodiceAnnuncio() throws SQLException {
    String sql = "SELECT MAX(codiceannuncio) AS maxcode FROM annuncio WHERE codiceannuncio LIKE 'AN%'";
    try (PreparedStatement ps = conn.prepareStatement(sql);
         ResultSet rs = ps.executeQuery()) {
        if (rs.next()) {
            return rs.getString("maxcode"); // null se nessun codice AN%
        }
    }
    return null;
}



}
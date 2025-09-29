package service;

import dao.*;
import model.*;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

public class Service {

    private AnnuncioDAO annuncioDAO;
    private OffertaDAO offertaDAO;
    private OffreDAO offreDAO;
    private OggettoDAO oggettoDAO;
    private TipoConsegnaDAO tipoConsegnaDAO;
    private UtenteDAO utenteDAO;

    public Service(Connection conn) {
        this.annuncioDAO = new AnnuncioDAO(conn);
        this.offertaDAO = new OffertaDAO(conn);
        this.offreDAO = new OffreDAO(conn);
        this.oggettoDAO = new OggettoDAO(conn);
        this.tipoConsegnaDAO = new TipoConsegnaDAO(conn);
        this.utenteDAO = new UtenteDAO(conn);
    }

    // -------------------- UTENTI --------------------
    public Utente login(String email, String password) throws SQLException {
        return utenteDAO.login(email, password);
    }

    public Utente getUtenteByMatricola(String matricola) throws SQLException {
        return utenteDAO.getUtenteByMatricola(matricola);
    }

    public boolean creaUtente(Utente utente) throws SQLException {
        return utenteDAO.creaUtente(utente);
    }

    public boolean aggiornaUtente(Utente utente) throws SQLException {
        return utenteDAO.aggiornaUtente(utente);
    }

    public boolean eliminaUtente(String matricola) throws SQLException {
        return utenteDAO.eliminaUtente(matricola);
    }

    // -------------------- ANNUNCI --------------------
    public boolean creaAnnuncio(Annuncio annuncio) throws SQLException {
        return annuncioDAO.creaAnnuncio(annuncio);
    }

    public boolean aggiornaAnnuncio(Annuncio annuncio) throws SQLException {
        return annuncioDAO.aggiornaAnnuncio(annuncio);
    }

    public boolean eliminaAnnuncio(String codiceAnnuncio) throws SQLException {
        return annuncioDAO.eliminaAnnuncio(codiceAnnuncio);
    }

    public Annuncio getAnnuncioByCodice(String codiceAnnuncio) throws SQLException {
        return annuncioDAO.getAnnuncioByCodice(codiceAnnuncio);
    }

    public List<Annuncio> getAnnunciAttivi() throws SQLException {
        return annuncioDAO.getAnnunciAttivi();
    }

    public List<Annuncio> getAnnunciFiltrati(String categoria, String tipologia) throws SQLException {
        return annuncioDAO.getAnnunciFiltrati(categoria, tipologia);
    }
    
    public List<Annuncio> getAnnunciByUtente(String matricola) throws SQLException {
    	return annuncioDAO.getAnnunciByUtente(matricola);
    }

    // -------------------- OFFERTE --------------------
    public boolean creaOfferta(Offerta offerta) throws SQLException {
        return offertaDAO.creaOfferta(offerta);
    }

    public boolean aggiornaOfferta(Offerta offerta) throws SQLException {
        return offertaDAO.aggiornaOfferta(offerta);
    }

    public boolean eliminaOfferta(String codiceOfferta) throws SQLException {
        return offertaDAO.eliminaOfferta(codiceOfferta);
    }

    public List<Offerta> getOfferteByUtente(String matricola) throws SQLException {
        return offertaDAO.getOfferteByUtente(matricola);
    }

    public List<Offerta> getOfferteByAnnuncio(String codiceAnnuncio) throws SQLException {
        return offertaDAO.getOfferteByAnnuncio(codiceAnnuncio);
    }

    public Offerta getOffertaByCodice(String codiceOfferta) throws SQLException {
        return offertaDAO.getOffertaByCodice(codiceOfferta);
    }

    // -------------------- OGGETTI --------------------
    public boolean creaOggetto(Oggetto oggetto) throws SQLException {
        return oggettoDAO.creaOggetto(oggetto);
    }

    public boolean aggiornaOggetto(Oggetto oggetto) throws SQLException {
        return oggettoDAO.aggiornaOggetto(oggetto);
    }

    public boolean eliminaOggetto(String codiceOggetto) throws SQLException {
        return oggettoDAO.eliminaOggetto(codiceOggetto);
    }

    public Oggetto getOggettoByCodice(String codiceOggetto) throws SQLException {
        return oggettoDAO.getOggettoByCodice(codiceOggetto);
    }

    public List<Oggetto> getOggettiByAnnuncio(String codiceAnnuncio) throws SQLException {
        return oggettoDAO.getOggettiByAnnuncio(codiceAnnuncio);
    }

    // -------------------- TIPI CONSEGNA --------------------
    public boolean creaTipoConsegna(TipoConsegna consegna) throws SQLException {
        return tipoConsegnaDAO.creaTipoConsegna(consegna);
    }

    public boolean aggiornaTipoConsegna(TipoConsegna consegna) throws SQLException {
        return tipoConsegnaDAO.aggiornaTipoConsegna(consegna);
    }

    public boolean eliminaTipoConsegna(String codiceConsegna) throws SQLException {
        return tipoConsegnaDAO.eliminaTipoConsegna(codiceConsegna);
    }

    public List<TipoConsegna> getConsegneByAnnuncio(String codiceAnnuncio) throws SQLException {
        return tipoConsegnaDAO.getConsegneByAnnuncio(codiceAnnuncio);
    }

    // -------------------- STATISTICHE --------------------
    public int getTotaleOfferte() throws SQLException {
        return offertaDAO.getTotaleOfferte();
    }

    public int getTotaleOffertePerTipologia(String tipologia) throws SQLException {
        return offertaDAO.getTotaleOffertePerTipologia(tipologia);
    }

    public int getOfferteAccettatePerTipologia(String tipologia) throws SQLException {
        return offertaDAO.getOfferteAccettatePerTipologia(tipologia);
    }

    public double[] getStatisticheVenditeAccettate() throws SQLException {
        return offertaDAO.getStatisticheVenditeAccettate();
    }

    // -------------------- INVIO DI UNA NUOVA OFFERTA --------------------
    public boolean inviaOfferta(Offerta offerta, List<String> codiciOggetti) throws SQLException {
        Annuncio annuncio = annuncioDAO.getAnnuncioByCodice(offerta.getCodiceAnnuncio());
        if (annuncio == null) throw new SQLException("Annuncio non trovato");
        if (annuncio.getMatricola().equals(offerta.getMatricola()))
            throw new SQLException("Non puoi inviare offerte ai tuoi stessi annunci");
        if (!"attivo".equals(annuncio.getStato()))
            throw new SQLException("Non è possibile inviare offerte su annunci non attivi");
        if (!offerta.getTipo().equals(annuncio.getTipologia()))
            throw new SQLException("La tipologia dell'offerta deve coincidere con quella dell'annuncio");
        if ("vendita".equals(offerta.getTipo()) && offerta.getPrezzoOfferto() == null)
            throw new SQLException("Una offerta di vendita deve avere prezzoOfferto valorizzato");
        if (!"vendita".equals(offerta.getTipo()) && offerta.getPrezzoOfferto() != null)
            throw new SQLException("Solo le offerte di tipo vendita possono avere prezzoOfferto valorizzato");

        boolean inserita = offertaDAO.creaOfferta(offerta);
        if (!inserita) throw new SQLException("Errore durante l'inserimento dell'offerta");

        if ("scambio".equals(offerta.getTipo()) && codiciOggetti != null) {
            for (String codiceOggetto : codiciOggetti) {
                Oggetto oggetto = oggettoDAO.getOggettoByCodice(codiceOggetto);
                if (oggetto == null) throw new SQLException("Oggetto " + codiceOggetto + " non trovato");
                offreDAO.aggiungiOggettoAScambio(offerta.getCodiceOfferta(), codiceOggetto);
            }
        }

        return true;
    }

    // -------------------- ACCETTA UN'OFFERTA --------------------
    public boolean accettaOfferta(String codiceOfferta) throws SQLException {
        Offerta offerta = offertaDAO.getOffertaByCodice(codiceOfferta);
        if (offerta == null) throw new SQLException("Offerta non trovata");

        Annuncio annuncio = annuncioDAO.getAnnuncioByCodice(offerta.getCodiceAnnuncio());
        if (annuncio == null) throw new SQLException("Annuncio non trovato");
        if (!"attivo".equals(annuncio.getStato()))
            throw new SQLException("L'annuncio non è attivo");

        offerta.setStato("accettata");
        boolean aggiornata = offertaDAO.aggiornaOfferta(offerta);
        if (!aggiornata) throw new SQLException("Errore durante l'accettazione dell'offerta");

        switch (offerta.getTipo()) {
            case "vendita": annuncio.setStato("venduto"); break;
            case "scambio": annuncio.setStato("scambiato"); break;
            case "regalo": annuncio.setStato("regalato"); break;
        }
        annuncioDAO.aggiornaAnnuncio(annuncio);

        List<Offerta> altreOfferte = offertaDAO.getOfferteByAnnuncio(offerta.getCodiceAnnuncio());
        for (Offerta o : altreOfferte) {
            if (!o.getCodiceOfferta().equals(codiceOfferta) && "inviata".equals(o.getStato())) {
                o.setStato("rifiutata");
                offertaDAO.aggiornaOfferta(o);
            }
        }

        return true;
    }

    // -------------------- RIFIUTA UN'OFFERTA --------------------
    public boolean rifiutaOfferta(String codiceOfferta) throws SQLException {
        Offerta offerta = offertaDAO.getOffertaByCodice(codiceOfferta);
        if (offerta == null) throw new SQLException("Offerta non trovata");
        if (!"inviata".equals(offerta.getStato()))
            throw new SQLException("Solo le offerte inviate possono essere rifiutate");
        offerta.setStato("rifiutata");
        return offertaDAO.aggiornaOfferta(offerta);
    }
}

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

    // UTENTI
    public Utente login(String email, String password) throws SQLException {
        return utenteDAO.login(email, password);
    }
    
 // Offerte inviate (query filtrata sulla tabella offerte per colonna "matricola", e non "proprietario annuncio")
    public List<Offerta> getOfferteInviateByUtente(String matricola) throws SQLException {
        return offertaDAO.getOfferteInviateByUtente(matricola);
    }

    // Recupera descrizione annuncio da codice (usando il DAO)
    public String getDescrizioneAnnuncio(String codiceAnnuncio) throws SQLException {
        Annuncio ann = annuncioDAO.getAnnuncioByCodice(codiceAnnuncio);
        return ann != null ? ann.getDescrizione() : "";
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

    // ANNUNCI
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

    // OFFERTE
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

    
    public List<Offerta> getOfferteByCodiceAnnuncio(String codiceAnnuncio) throws SQLException {
        return offertaDAO.getOfferteByCodiceAnnuncio(codiceAnnuncio);
    }
    
    
    public boolean accettaOfferta(String codiceOfferta) throws SQLException {
        return offertaDAO.accettaOfferta(codiceOfferta);
    }

    public boolean rifiutaOfferta(String codiceOfferta) throws SQLException {
        return offertaDAO.rifiutaOfferta(codiceOfferta);
    }

    // OGGETTI
    public boolean creaOggetto(Oggetto oggetto, String matricola) throws SQLException {
        return oggettoDAO.creaOggetto(oggetto, matricola);
    }

    public boolean aggiornaOggetto(Oggetto oggetto) throws SQLException {
        return oggettoDAO.aggiornaOggetto(oggetto);
    }

    public boolean eliminaOggetto(String codiceOggetto) throws SQLException {
        return oggettoDAO.eliminaOggetto(codiceOggetto);
    }
    
    public boolean modificaOggetto(Oggetto oggetto) throws SQLException {
        return oggettoDAO.aggiornaOggetto(oggetto);
    }
    public void aggiornaCodiceAnnuncioOggetto(String codiceOggetto, String codiceAnnuncio) throws SQLException {
        oggettoDAO.aggiornaCodiceAnnuncioOggetto(codiceOggetto, codiceAnnuncio);
    }

    public Oggetto getOggettoByCodice(String codiceOggetto) throws SQLException {
        return oggettoDAO.getOggettoByCodice(codiceOggetto);
    }

    public List<Oggetto> getOggettiByAnnuncio(String codiceAnnuncio) throws SQLException {
        return oggettoDAO.getOggettiByAnnuncio(codiceAnnuncio);
    }

    public List<Oggetto> getOggettiUtente(String matricola) throws SQLException {
        return oggettoDAO.getOggettiByMatricola(matricola);
    }
    
    // TIPI CONSEGNA
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

    // STATISTICHE
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

    public List<Offerta> getOfferteRicevuteByUtente(String matricola) throws SQLException {
        return offertaDAO.getOfferteRicevuteByUtente(matricola);
    }

    // CONTROLLO OFFERTA E INVIO
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
    
    public boolean inviaOffertaLogica(String codiceAnnuncio, String tipo, Double prezzoOfferto, String matricolaUtente) throws SQLException {
        // Crea oggetto Offerta
        Offerta offerta = new Offerta();
        offerta.setCodiceAnnuncio(codiceAnnuncio);
        offerta.setTipo(tipo);
        offerta.setPrezzoOfferto(prezzoOfferto);
        offerta.setMatricola(matricolaUtente);
        offerta.setStato("inviata");
        // Puoi generare il codice offerta come preferisci
        offerta.setCodiceOfferta(java.util.UUID.randomUUID().toString());
        return inviaOfferta(offerta, null);
    }
    public boolean inviaOffertaComplessiva(String codiceAnnuncio, String tipo, Double prezzoOfferto, String messaggio, String matricolaUtente) throws SQLException {
        Offerta offerta = new Offerta();
        offerta.setCodiceAnnuncio(codiceAnnuncio);
        offerta.setTipo(tipo);
        offerta.setPrezzoOfferto(prezzoOfferto);
        offerta.setMatricola(matricolaUtente);
        offerta.setStato("inviata");
        offerta.setMessaggio(messaggio);
        return offertaDAO.creaOfferta(offerta);
    }
    public boolean inviaOffertaConOggettiLogica(String codiceAnnuncio, List<String> codiciOggetti, String matricolaUtente) throws SQLException {
        Offerta offerta = new Offerta();
        offerta.setCodiceAnnuncio(codiceAnnuncio);
        offerta.setTipo("scambio");
        offerta.setMatricola(matricolaUtente);
        offerta.setStato("inviata");
        offerta.setCodiceOfferta(java.util.UUID.randomUUID().toString());
        // In questo caso, il prezzoOfferto resta null per offerte di scambio
        return inviaOfferta(offerta, codiciOggetti);
    }
 // MODIFICA ANNUNCIO (da aggiungere se manca)
    public boolean modificaAnnuncio(Annuncio annuncio) throws SQLException {
        return annuncioDAO.modificaAnnuncio(annuncio);
    }
}
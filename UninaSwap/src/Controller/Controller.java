package Controller;

import dao.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import model.*;

import java.sql.Connection;
import java.sql.Date;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Controller principale dell'applicazione UninaSwap.
 * Gestisce login/registrazione, annunci, offerte e oggetti.
 * Integra tutta la logica di business precedentemente nel Service.
 */
public class Controller {

    // =========================================================
    // == CAMPI / DAO
    // =========================================================
    private AnnuncioDAO annuncioDAO;
    private OffertaDAO offertaDAO;
    private OffreDAO offreDAO;
    private OggettoDAO oggettoDAO;
    private TipoConsegnaDAO tipoConsegnaDAO;
    private UtenteDAO utenteDAO;

    private Utente utenteCorrente;

    // =========================================================
    // == COSTRUTTORE
    // =========================================================
    public Controller(Connection conn) {
        this.annuncioDAO = new AnnuncioDAO(conn);
        this.offertaDAO = new OffertaDAO(conn);
        this.offreDAO = new OffreDAO(conn);
        this.oggettoDAO = new OggettoDAO(conn);
        this.tipoConsegnaDAO = new TipoConsegnaDAO(conn);
        this.utenteDAO = new UtenteDAO(conn);
    }

    // =========================================================
    // == AUTHENTICATION (Login / Registrazione / Logout)
    // =========================================================

    /**
     * Login utente.
     */
    public boolean login(String email, String password) {
        try {
            String e = email == null ? "" : email.trim();
            String p = password == null ? "" : password.trim();
            if (e.isEmpty() || p.isEmpty()) {
                showError("Email e password sono obbligatorie.");
                return false;
            }
            Utente utente = utenteDAO.login(e, p);
            if (utente != null) {
                utenteCorrente = utente;
                return true;
            }
        } catch (SQLException ex) {
            showError("Errore login: " + ex.getMessage());
        }
        return false;
    }

    /**
     * Registrazione nuovo utente.
     */
    public boolean register(String nome, String cognome, String matricola, String email, String password, String universita) {
        try {
            String n = safeTrim(nome);
            String c = safeTrim(cognome);
            String m = safeTrim(matricola);
            String e = safeTrim(email);
            String p = safeTrim(password);
            String u = safeTrim(universita);

            if (n.isEmpty() || c.isEmpty() || m.isEmpty() || e.isEmpty() || p.isEmpty() || u.isEmpty()) {
                showError("Tutti i campi sono obbligatori.");
                return false;
            }
            if (!isLikelyEmail(e)) {
                showError("Email non valida.");
                return false;
            }
            if (p.length() < 8) {
                showError("La password deve avere almeno 8 caratteri.");
                return false;
            }

            Utente nuovo = new Utente(m, n, c, e, p, u);
            boolean ok = utenteDAO.creaUtente(nuovo);
            if (!ok) showError("Registrazione non riuscita.");
            return ok;

        } catch (SQLException ex) {
            if ("23505".equals(ex.getSQLState())) {
                showError("Email o matricola già registrate.");
            } else {
                showError("Errore registrazione: " + ex.getMessage());
            }
            return false;
        }
    }

    /**
     * Logout.
     */
    public void logout() {
        utenteCorrente = null;
    }

    /**
     * Restituisce l'utente correntemente loggato.
     */
    public Utente getUtenteCorrente() {
        return utenteCorrente;
    }

    /**
     * Recupera utente per matricola.
     */
    public Utente getUtenteByMatricola(String matricola) throws SQLException {
        return utenteDAO.getUtenteByMatricola(matricola);
    }

    /**
     * Aggiorna dati utente.
     */
    public boolean aggiornaUtente(Utente utente) throws SQLException {
        return utenteDAO.aggiornaUtente(utente);
    }

    /**
     * Elimina utente.
     */
    public boolean eliminaUtente(String matricola) throws SQLException {
        return utenteDAO.eliminaUtente(matricola);
    }

    // =========================================================
    // == ANNUNCI
    // =========================================================

    /**
     * Ottiene tutti gli annunci attivi (raw).
     */
    public List<Annuncio> getAnnunciAttiviRaw() throws SQLException {
        return annuncioDAO.getAnnunciAttivi();
    }

    /**
     * Ottiene annunci attivi formattati per la GUI.
     */
    public ObservableList<String> getAnnunciAttiviFormatted() {
        try {
            List<Annuncio> annunci = annuncioDAO.getAnnunciAttivi();
            ObservableList<String> items = FXCollections.observableArrayList();
            for (Annuncio a : annunci) {
                String prezzo = (a.getPrezzo() != null) ? "€" + a.getPrezzo() : "Gratis";
                String formatted = String.format("[%s] %s - %s - %s",
                        a.getTipologia().toUpperCase(), a.getCategoria(), prezzo, a.getStato().toUpperCase());
                items.add(formatted);
            }
            if (items.isEmpty()) items.add("Nessun annuncio trovato");
            return items;
        } catch (SQLException e) {
            showError("Errore caricamento annunci: " + e.getMessage());
            return FXCollections.observableArrayList("Errore caricamento dati");
        }
    }

    /**
     * Annunci di un utente specifico.
     */
    public List<Annuncio> getAnnunciByUtente(String matricola) throws SQLException {
        return annuncioDAO.getAnnunciByUtente(matricola);
    }

    /**
     * Recupera annuncio per codice.
     */
    public Annuncio getAnnuncioByCodice(String codiceAnnuncio) throws SQLException {
        return annuncioDAO.getAnnuncioByCodice(codiceAnnuncio);
    }

    /**
     * Annunci filtrati per categoria e tipologia.
     */
    public List<Annuncio> getAnnunciFiltrati(String categoria, String tipologia) throws SQLException {
        return annuncioDAO.getAnnunciFiltrati(categoria, tipologia);
    }

    /**
     * Recupera descrizione annuncio dato il codice.
     */
    public String getDescrizioneAnnuncio(String codiceAnnuncio) throws SQLException {
        Annuncio ann = annuncioDAO.getAnnuncioByCodice(codiceAnnuncio);
        return ann != null ? ann.getDescrizione() : "";
    }

    /**
     * Crea nuovo annuncio.
     */
    public boolean creaAnnuncio(String categoria, String tipologia, String descrizione, double prezzo, String codiceOggetto) throws SQLException {
        if (codiceOggetto == null || codiceOggetto.isEmpty()) {
            throw new SQLException("Devi associare obbligatoriamente un tuo oggetto all'annuncio!");
        }

        Date oggi = new Date(System.currentTimeMillis());
        Annuncio nuovoAnnuncio = new Annuncio(
            null,
            descrizione,
            categoria,
            tipologia,
            prezzo,
            "attivo",
            oggi,
            utenteCorrente.getMatricola()
        );
        boolean result = annuncioDAO.creaAnnuncio(nuovoAnnuncio);

        // Collega oggetto all'annuncio
        oggettoDAO.aggiornaCodiceAnnuncioOggetto(codiceOggetto, nuovoAnnuncio.getCodiceAnnuncio());
        // OPPURE, se hai la tabella 'offre':
        // offreDAO.creaOffre(new Offre(nuovoAnnuncio.getCodiceAnnuncio(), codiceOggetto));

        return result;
    }

    
    /**
     * Modifica annuncio esistente.
     */
    public boolean modificaAnnuncio(String codiceAnnuncio,
                                    String categoria,
                                    String tipologia,
                                    String descrizione,
                                    double prezzo,
                                    String stato) throws SQLException {
        Annuncio esistente = annuncioDAO.getAnnuncioByCodice(codiceAnnuncio);

        Annuncio aggiornato = new Annuncio(
                codiceAnnuncio,
                descrizione,
                categoria,
                tipologia,
                prezzo,
                stato,
                esistente.getDataPubblicazione(),
                esistente.getMatricola()
        );

        return annuncioDAO.aggiornaAnnuncio(aggiornato);
    }

    /**
     * Elimina annuncio.
     */
    public void eliminaAnnuncio(String codiceAnnuncio) throws SQLException {
        annuncioDAO.eliminaAnnuncio(codiceAnnuncio);
    }

    // =========================================================
    // == OFFERTE
    // =========================================================

    /**
     * Offerte ricevute da un utente (sul proprio annuncio).
     */
    public List<Offerta> getOfferteRicevuteByUtente(String matricola) throws SQLException {
        return offertaDAO.getOfferteRicevuteByUtente(matricola);
    }

    /**
     * Offerte inviate da un utente.
     */
    public List<Offerta> getOfferteInviateByUtente(String matricola) throws SQLException {
        return offertaDAO.getOfferteInviateByUtente(matricola);
    }

    /**
     * Offerte per un annuncio specifico.
     */
    public List<Offerta> getOfferteByAnnuncio(String codiceAnnuncio) throws SQLException {
        return offertaDAO.getOfferteByAnnuncio(codiceAnnuncio);
    }

    /**
     * Offerte di un utente (generico).
     */
    public List<Offerta> getOfferteByUtente(String matricola) throws SQLException {
        return offertaDAO.getOfferteByUtente(matricola);
    }

    /**
     * Recupera offerta per codice.
     */
    public Offerta getOffertaByCodice(String codiceOfferta) throws SQLException {
        return offertaDAO.getOffertaByCodice(codiceOfferta);
    }

    /**
     * Recupera offerte dato il codice annuncio.
     */
    public List<Offerta> getOfferteByCodiceAnnuncio(String codiceAnnuncio) throws SQLException {
        return offertaDAO.getOfferteByCodiceAnnuncio(codiceAnnuncio);
    }

    /**
     * Invia offerta con controllo logica di business.
     */
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

    /**
     * Invia offerta (semplificata con parametri diretti).
     */
    public boolean inviaOfferta(String codiceAnnuncio, String tipo, Double prezzoOfferto) throws SQLException {
        Offerta offerta = new Offerta();
        offerta.setCodiceAnnuncio(codiceAnnuncio);
        offerta.setTipo(tipo);
        offerta.setPrezzoOfferto(prezzoOfferto);
        offerta.setMatricola(utenteCorrente.getMatricola());
        offerta.setStato("inviata");
        offerta.setCodiceOfferta(UUID.randomUUID().toString());
        return inviaOfferta(offerta, null);
    }

    /**
     * Invia offerta con messaggio.
     */
    public boolean inviaOfferta(String codiceAnnuncio, String tipo, Double prezzoOfferto, String messaggio) throws SQLException {
        Offerta offerta = new Offerta();
        offerta.setCodiceAnnuncio(codiceAnnuncio);
        offerta.setTipo(tipo);
        offerta.setPrezzoOfferto(prezzoOfferto);
        offerta.setMatricola(utenteCorrente.getMatricola());
        offerta.setStato("inviata");
        offerta.setMessaggio(messaggio);
        offerta.setCodiceOfferta(UUID.randomUUID().toString());
        return offertaDAO.creaOfferta(offerta);
    }

    /**
     * Invia offerta con oggetti (scambio).
     */
    public boolean inviaOffertaConOggetti(String codiceAnnuncio, List<String> codiciOggetti) throws SQLException {
        // Crea l'offerta di scambio
        boolean ok = inviaOfferta(codiceAnnuncio, "scambio", null);
        if (ok) {
            // Ottieni l'ultima offerta creata (o il codice dell'offerta appena creata)
            String codiceOfferta = getUltimoaOffertaCreata(); // Metodo che recupera l'ultimo codice offerta
            
            // Associa tutti gli oggetti selezionati all'offerta
            for (String codiceOggetto : codiciOggetti) {
                offreDAO.creaOffre(new Offre(codiceOfferta, codiceOggetto));
            }
        }
        return ok;
    }
    
    private String getUltimoaOffertaCreata() throws SQLException {
        String matricola = getUtenteCorrente().getMatricola();
        List<Offerta> offerte = offertaDAO.getOfferteByUtente(matricola);
        if (offerte.isEmpty()) return null;
        // Prendi l'ultima offerta creata (assumendo ordinamento per data o ID)
        return offerte.get(offerte.size() - 1).getCodiceOfferta();
    }

    public String getUltimaOffertaScambioUtente() throws SQLException {
        List<Offerta> offerte = offertaDAO.getOfferteByUtente(getUtenteCorrente().getMatricola());
        for (int i = offerte.size() - 1; i >= 0; i--) {
            if ("scambio".equalsIgnoreCase(offerte.get(i).getTipo())) return offerte.get(i).getCodiceOfferta();
        }
        return null;
    }
    public void associaOggettoAdOfferta(String codiceOfferta, String codiceOggetto) throws SQLException {
        offreDAO.creaOffre(new Offre(codiceOfferta, codiceOggetto));
    }
    /**
     * Accetta offerta.
     */
    public boolean accettaOfferta(String codiceOfferta) {
        try {
            Offerta offerta = offertaDAO.getOffertaByCodice(codiceOfferta);
            if (offerta == null) throw new SQLException("Offerta non trovata");
            boolean ok = offertaDAO.accettaOfferta(codiceOfferta);
            if (ok) {
                String nuovoStato = switch (offerta.getTipo().toLowerCase()) {
                    case "vendita" -> "venduto";
                    case "scambio" -> "scambiato";
                    case "regalo" -> "regalato";
                    default -> "attivo";
                };
                Annuncio annuncio = annuncioDAO.getAnnuncioByCodice(offerta.getCodiceAnnuncio());
                String matricolaAnnunciante = annuncio.getMatricola();
                String matricolaRichiedente = offerta.getMatricola();
                
                // Per TUTTI i tipi: trasferisci l'oggetto dell'annuncio al richiedente
                List<Oggetto> oggettiAnnuncio = getOggettiByAnnuncio(annuncio.getCodiceAnnuncio());
                for (Oggetto obj : oggettiAnnuncio) {
                    oggettoDAO.aggiornaOggettoMatricola(obj.getCodiceOggetto(), matricolaRichiedente);
                    oggettoDAO.rimuoviAssociazioneAnnuncio(obj.getCodiceOggetto()); // <-- AGGIUNTO
                }
                
                // Solo per SCAMBIO: trasferisci anche gli oggetti dell'offerta all'annunciante
                if ("scambio".equalsIgnoreCase(offerta.getTipo())) {
                    List<Offre> oggettiScambiati = offreDAO.getOggettiByOfferta(codiceOfferta);
                    for (Offre o : oggettiScambiati) {
                        oggettoDAO.aggiornaOggettoMatricola(o.getCodiceOggetto(), matricolaAnnunciante);
                        oggettoDAO.rimuoviAssociazioneAnnuncio(o.getCodiceOggetto()); // <-- AGGIUNTO
                    }
                }
                
                annuncio.setStato(nuovoStato);
                annuncioDAO.aggiornaAnnuncio(annuncio);
            }
            return ok;
        } catch (SQLException e) {
            showError("Errore accettazione offerta: " + e.getMessage());
            return false;
        }
    }



    public boolean accettaScambio(String codiceOfferta, String matricolaRichiedente) {
        try {
            Offerta offerta = offertaDAO.getOffertaByCodice(codiceOfferta);
            if (offerta == null || !"scambio".equalsIgnoreCase(offerta.getTipo())) {
                throw new SQLException("Non è uno scambio");
            }
            boolean ok = offertaDAO.accettaOfferta(codiceOfferta);
            if (ok) {
                List<Offre> oggettiScambiati = offreDAO.getOggettiByOfferta(codiceOfferta);
                Annuncio annuncio = annuncioDAO.getAnnuncioByCodice(offerta.getCodiceAnnuncio());
                String matricolaAnnunciante = annuncio.getMatricola();
                for (Offre o : oggettiScambiati) {
                    Oggetto obj = oggettoDAO.getOggettoByCodice(o.getCodiceOggetto());
                    String nuovoProprietario;
                    if (obj.getMatricola().equals(matricolaAnnunciante)) {
                        nuovoProprietario = matricolaRichiedente;
                    } else {
                        nuovoProprietario = matricolaAnnunciante;
                    }
                    oggettoDAO.aggiornaOggettoMatricola(o.getCodiceOggetto(), nuovoProprietario);
                }
                annuncio.setStato("scambiato");
                annuncioDAO.aggiornaAnnuncio(annuncio);
            }
            return ok;
        } catch (SQLException e) {
            showError("Errore scambio: " + e.getMessage());
            return false;
        }
    }


    /**
     * Rifiuta offerta.
     */
    public boolean rifiutaOfferta(String codiceOfferta) {
        try {
            return offertaDAO.rifiutaOfferta(codiceOfferta);
        } catch (SQLException e) {
            showError("Errore rifiuto offerta: " + e.getMessage());
            return false;
        }
    }

    /**
     * Elimina offerta.
     */
    public boolean eliminaOfferta(String codiceOfferta) throws SQLException {
        return offertaDAO.eliminaOfferta(codiceOfferta);
    }

    // =========================================================
    // == OGGETTI
    // =========================================================

    /**
     * Recupera oggetti di un utente (solo codici).
     */
    public List<String> getOggettiUtente(String matricola) throws SQLException {
        List<Oggetto> oggetti = oggettoDAO.getOggettiByMatricola(matricola);
        List<String> codici = new ArrayList<>();
        for (Oggetto o : oggetti) {
            codici.add(o.getCodiceOggetto());
        }
        return codici;
    }
    
    public List<Oggetto> getOggettiByUtente(String matricola) throws SQLException {
        return oggettoDAO.getOggettiByMatricola(matricola);
    }


    /**
     * Recupera oggetti di un utente (oggetti completi).
     */
    public List<Oggetto> getOggettiUtenteObj(String matricola) throws SQLException {
        return oggettoDAO.getOggettiByMatricola(matricola);
    }

    /**
     * Recupera oggetto per codice.
     */
    public Oggetto getOggettoByCodice(String codiceOggetto) throws SQLException {
        return oggettoDAO.getOggettoByCodice(codiceOggetto);
    }

    /**
     * Oggetti associati a un annuncio.
     */
    public List<Oggetto> getOggettiByAnnuncio(String codiceAnnuncio) throws SQLException {
        return oggettoDAO.getOggettiByAnnuncio(codiceAnnuncio);
    }

    /**
     * Crea nuovo oggetto.
     */
    public boolean creaOggetto(Oggetto oggetto) throws SQLException {
        String matricola = getUtenteCorrente().getMatricola();
        return oggettoDAO.creaOggetto(oggetto, matricola);
    }

    /**
     * Modifica oggetto esistente.
     */
    public boolean modificaOggetto(Oggetto oggetto) throws SQLException {
        return oggettoDAO.aggiornaOggetto(oggetto);
    }

    /**
     * Elimina oggetto.
     */
    public boolean eliminaOggetto(String codiceOggetto) throws SQLException {
        return oggettoDAO.eliminaOggetto(codiceOggetto);
    }

    /**
     * Aggiorna codice annuncio di un oggetto.
     */
    public void aggiornaCodiceAnnuncioOggetto(String codiceOggetto, String codiceAnnuncio) throws SQLException {
        oggettoDAO.aggiornaCodiceAnnuncioOggetto(codiceOggetto, codiceAnnuncio);
    }

    // =========================================================
    // == TIPI CONSEGNA
    // =========================================================

    /**
     * Crea tipo consegna.
     */
    public boolean creaTipoConsegna(TipoConsegna consegna) throws SQLException {
        return tipoConsegnaDAO.creaTipoConsegna(consegna);
    }

    /**
     * Aggiorna tipo consegna.
     */
    public boolean aggiornaTipoConsegna(TipoConsegna consegna) throws SQLException {
        return tipoConsegnaDAO.aggiornaTipoConsegna(consegna);
    }

    /**
     * Elimina tipo consegna.
     */
    public boolean eliminaTipoConsegna(String codiceConsegna) throws SQLException {
        return tipoConsegnaDAO.eliminaTipoConsegna(codiceConsegna);
    }

    /**
     * Consegne associate a un annuncio.
     */
    public List<TipoConsegna> getConsegneByAnnuncio(String codiceAnnuncio) throws SQLException {
        return tipoConsegnaDAO.getConsegneByAnnuncio(codiceAnnuncio);
    }

    // =========================================================
    // == STATISTICHE
    // =========================================================

    /**
     * Totale offerte complessive.
     */
    public int getTotaleOfferte() {
        try {
            return offertaDAO.getTotaleOfferte();
        } catch (SQLException e) {
            showError("Errore getTotaleOfferte: " + e.getMessage());
            return 0;
        }
    }

    /**
     * Totale offerte per tipologia.
     */
    public int getTotaleOffertePerTipologia(String tipologia) {
        try {
            return offertaDAO.getTotaleOffertePerTipologia(tipologia);
        } catch (SQLException e) {
            showError("Errore getTotaleOffertePerTipologia: " + e.getMessage());
            return 0;
        }
    }

    /**
     * Offerte accettate per tipologia.
     */
    public int getOfferteAccettatePerTipologia(String tipologia) {
        try {
            return offertaDAO.getOfferteAccettatePerTipologia(tipologia);
        } catch (SQLException e) {
            showError("Errore getOfferteAccettatePerTipologia: " + e.getMessage());
            return 0;
        }
    }

    /**
     * Statistiche vendite accettate.
     */
    public double[] getStatisticheVenditeAccettate() {
        try {
            return offertaDAO.getStatisticheVenditeAccettate();
        } catch (SQLException e) {
            showError("Errore getStatisticheVenditeAccettate: " + e.getMessage());
            return new double[0];
        }
    }

    // =========================================================
    // == NOTIFICHE
    // =========================================================

    /**
     * Genera notifiche per l'utente (offerte ricevute, annunci scaduti).
     */
    public List<String> getNotificheUtente(String matricola) {
        List<String> notif = new ArrayList<>();
        try {
            List<Annuncio> mieiAnnunci = getAnnunciByUtente(matricola);
            int offerteRicevute = 0;

            for (Annuncio a : mieiAnnunci) {
                List<Offerta> offerteSuAnnuncio = offertaDAO.getOfferteByCodiceAnnuncio(a.getCodiceAnnuncio());
                for (Offerta o : offerteSuAnnuncio) {
                    if ("inviata".equalsIgnoreCase(o.getStato())) {
                        offerteRicevute++;
                    }
                }
            }
            if (offerteRicevute > 0)
                notif.add("Hai " + offerteRicevute + " offerte ricevute da accettare.");

            int annunciScaduti = 0;
            for (Annuncio a : mieiAnnunci) {
                if ("scaduto".equalsIgnoreCase(a.getStato())) {
                    annunciScaduti++;
                }
            }
            if (annunciScaduti > 0)
                notif.add("Hai " + annunciScaduti + " annunci scaduti.");

        } catch (Exception e) {
            notif.add("Errore nel caricamento notifiche.");
        }
        return notif;
    }

    // =========================================================
    // == UTILITY INTERNE
    // =========================================================

    private void showError(String message) {
        System.err.println(message);
    }

    private static String safeTrim(String s) {
        return s == null ? "" : s.trim();
    }

    private static boolean isLikelyEmail(String s) {
        return s != null && s.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    }
}
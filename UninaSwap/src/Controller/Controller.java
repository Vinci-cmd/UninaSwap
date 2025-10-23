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
    private Connection conn; // Campo per la connessione

    // =========================================================
    // == COSTRUTTORE
    // =========================================================
    public Controller(Connection conn) {
        this.conn = conn; // Salvata la connessione
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
    public boolean creaAnnuncio(String categoria, String tipologia, String descrizione, double prezzo) throws SQLException {
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
        return annuncioDAO.creaAnnuncio(nuovoAnnuncio);
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
        // Aggiungo la data
        offerta.setData(new Date(System.currentTimeMillis()));
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
        // Aggiungo la data
        offerta.setData(new Date(System.currentTimeMillis()));
        return offertaDAO.creaOfferta(offerta);
    }

    /**
     * Invia offerta con oggetti (scambio).
     */
    public boolean inviaOffertaConOggetti(String codiceAnnuncio, List<String> codiciOggetti) throws SQLException {
        Offerta offerta = new Offerta();
        offerta.setCodiceAnnuncio(codiceAnnuncio);
        offerta.setTipo("scambio");
        offerta.setMatricola(utenteCorrente.getMatricola());
        offerta.setStato("inviata");
        offerta.setCodiceOfferta(UUID.randomUUID().toString());
        // Aggiungo la data
        offerta.setData(new Date(System.currentTimeMillis()));
        return inviaOfferta(offerta, codiciOggetti);
    }

    /**
     * Accetta offerta.
     */
    public boolean accettaOfferta(String codiceOfferta) {
        try {
            return offertaDAO.accettaOfferta(codiceOfferta);
        } catch (SQLException e) {
            showError("Errore accettazione offerta: " + e.getMessage());
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


    /**
     * Esegue un acquisto immediato di un annuncio.
     * Questa operazione è atomica (transazionale):
     * 1. Marca l'annuncio come "concluso".
     * 2. Crea un'offerta GIA' ACCETTATA per tracciare la vendita.
     * 3. Rifiuta tutte le altre offerte "inviate" per quell'annuncio.
     *
     * @param codiceAnnuncio Il codice dell'annuncio da acquistare.
     * @return true se l'acquisto è andato a buon fine.
     * @throws SQLException Se c'è un errore DB o logico.
     */
    public boolean compraSubito(String codiceAnnuncio) throws SQLException {
        
        // Prendi i dettagli dell'acquirente (l'utente corrente)
        String matricolaBuyer = getUtenteCorrente().getMatricola();

        // 1. Ottieni i dettagli dell'annuncio (prezzo e venditore)
        Annuncio annuncio = annuncioDAO.getAnnuncioByCodice(codiceAnnuncio);

        // --- VALIDAZIONE LOGICA ---
        if (annuncio == null) {
            throw new SQLException("Annuncio non trovato.");
        }
        if (!"attivo".equals(annuncio.getStato())) {
            throw new SQLException("Questo annuncio non è più attivo.");
        }
        if (annuncio.getMatricola().equals(matricolaBuyer)) {
            throw new SQLException("Non puoi acquistare un tuo annuncio.");
        }
        if (!"vendita".equals(annuncio.getTipologia()) || annuncio.getPrezzo() == null) {
            throw new SQLException("Questo annuncio non è in vendita o non ha un prezzo.");
        }

        double prezzoPieno = annuncio.getPrezzo();

        try {
            // --- INIZIO TRANSAZIONE ---
            conn.setAutoCommit(false);

            // =========================================================
            // == MODIFICA: Uso il nuovo metodo aggiornaStatoAnnuncio ==
            // =========================================================
            // 1. Aggiorna l'annuncio a "concluso"
            // Uso il nuovo metodo 'aggiornaStatoAnnuncio' del DAO per modificare SOLO lo stato
            boolean statoAggiornato = annuncioDAO.aggiornaStatoAnnuncio(annuncio.getCodiceAnnuncio(), "venduto");
            if (!statoAggiornato) {
                // Se non si aggiorna, forzo il rollback
                throw new SQLException("Impossibile aggiornare lo stato dell'annuncio a 'concluso'.");
            }
            // =========================================================
            // =========================================================

            // 2. Crea l'offerta GIA' ACCETTATA per tracciare la vendita
            Offerta offerta = new Offerta();
            offerta.setCodiceOfferta(UUID.randomUUID().toString());
            offerta.setCodiceAnnuncio(codiceAnnuncio);
            offerta.setMatricola(matricolaBuyer);
            offerta.setTipo("vendita");
            offerta.setPrezzoOfferto(prezzoPieno);
            offerta.setStato("accettata"); // <-- GIA' ACCETTATA
            offerta.setData(new Date(System.currentTimeMillis()));
            offerta.setMessaggio("Acquisto 'Compra Subito'");
            
            offertaDAO.creaOfferta(offerta);
            
            // 3. Rifiuta tutte le ALTRE offerte "inviate" per questo annuncio
            List<Offerta> altreOfferte = offertaDAO.getOfferteByCodiceAnnuncio(codiceAnnuncio);
            for (Offerta o : altreOfferte) {
                if ("inviata".equals(o.getStato())) {
                    offertaDAO.rifiutaOfferta(o.getCodiceOfferta());
                }
            }

            // --- FINE TRANSAZIONE ---
            conn.commit();
            return true;

        } catch (Exception e) {
            // Se qualcosa va storto, annulla tutto
            conn.rollback();
            
            // =========================================================
            // == MODIFICA: Corretto il typo nell'errore ==
            // =========================================================
            throw new SQLException("Errore durante la transazione di acquisto: " + e.getMessage());
        } finally {
            conn.setAutoCommit(true);
        }
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
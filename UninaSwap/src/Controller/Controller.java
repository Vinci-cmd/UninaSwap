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

    
    
 // Associa un oggetto a un annuncio e riattiva l'annuncio se era scaduto
    public void associaOggettoEAttivaAnnuncio(String codiceOggetto, String codiceAnnuncio) throws SQLException {
        boolean auto = conn.getAutoCommit();
        try {
            conn.setAutoCommit(false);
            // 1) Associa l'oggetto
            oggettoDAO.aggiornaCodiceAnnuncioOggetto(codiceOggetto, codiceAnnuncio);
            // 2) Riporta annuncio "attivo"
            annuncioDAO.aggiornaStatoAnnuncio(codiceAnnuncio, "attivo");
            conn.commit();
        } catch (SQLException e) {
            try { conn.rollback(); } catch (SQLException ignore) {}
            throw e;
        } finally {
            try { conn.setAutoCommit(auto); } catch (SQLException ignore) {}
        }
    }

    
    public void disassociaOggettoEChiudiAnnuncio(String codiceOggetto) throws SQLException {
        boolean auto = conn.getAutoCommit();
        try {
            conn.setAutoCommit(false);

            // 1) Leggi l'oggetto per recuperare il codice annuncio
            Oggetto ogg = oggettoDAO.getOggettoByCodice(codiceOggetto);
            if (ogg == null) throw new SQLException("Oggetto non trovato: " + codiceOggetto);

            String codiceAnnuncio = ogg.getCodiceAnnuncio();

            // 2) Rimuovi associazione oggetto -> annuncio
            oggettoDAO.rimuoviAssociazioneAnnuncio(codiceOggetto);

            // 3) Se esiste un annuncio collegato, marcane lo stato a 'scaduto'
            if (codiceAnnuncio != null && !codiceAnnuncio.isBlank()) {
                annuncioDAO.aggiornaStatoAnnuncio(codiceAnnuncio, "scaduto");
            }

            conn.commit();
        } catch (SQLException e) {
            try { conn.rollback(); } catch (SQLException ignore) {}
            throw e;
        } finally {
            try { conn.setAutoCommit(auto); } catch (SQLException ignore) {}
        }
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

        boolean auto = conn.getAutoCommit();
        try {
            conn.setAutoCommit(false);

            Date oggi = new Date(System.currentTimeMillis());
            Double prezzoBoxed = "vendita".equalsIgnoreCase(tipologia) ? prezzo : null; // <-- normalizza

            Annuncio nuovoAnnuncio = new Annuncio(
                null, descrizione, categoria, tipologia, prezzoBoxed, "attivo", oggi, utenteCorrente.getMatricola()
            );
            boolean created = annuncioDAO.creaAnnuncio(nuovoAnnuncio);
            if (!created) throw new SQLException("Creazione annuncio fallita");

            // associa oggetto all'annuncio creato
            oggettoDAO.aggiornaCodiceAnnuncioOggetto(codiceOggetto, nuovoAnnuncio.getCodiceAnnuncio());

            conn.commit();
            return true;
        } catch (SQLException e) {
            try { conn.rollback(); } catch (SQLException ignore) {}
            throw e;
        } finally {
            try { conn.setAutoCommit(auto); } catch (SQLException ignore) {}
        }
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
if (esistente == null) throw new SQLException("Annuncio non trovato");

Double prezzoBoxed = "vendita".equalsIgnoreCase(tipologia) ? prezzo : null; // <-- normalizza

Annuncio aggiornato = new Annuncio(
codiceAnnuncio, descrizione, categoria, tipologia, prezzoBoxed,
stato, esistente.getDataPubblicazione(), esistente.getMatricola()
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
     * --- ORA TRANSAZIONALE PER EVITARE RACE CONDITION ---
     */
    public boolean inviaOfferta(Offerta offerta, List<String> codiciOggetti) throws SQLException {
        
        boolean autoCommitOriginale = false;
        try {
            // --- INIZIO TRANSAZIONE ---
            autoCommitOriginale = conn.getAutoCommit();
            conn.setAutoCommit(false); 

            // 1. CONTROLLO (all'interno della transazione)
            Annuncio annuncio = annuncioDAO.getAnnuncioByCodice(offerta.getCodiceAnnuncio());
            if (annuncio == null) throw new SQLException("Annuncio non trovato");
            if (annuncio.getMatricola().equals(offerta.getMatricola()))
                throw new SQLException("Non puoi inviare offerte ai tuoi stessi annunci");
            
            // Questo è il controllo che ora è protetto dalla transazione
            if (!"attivo".equals(annuncio.getStato()))
                throw new SQLException("Non è possibile inviare offerte su annunci non attivi");
            
            if (!offerta.getTipo().equals(annuncio.getTipologia()))
                throw new SQLException("La tipologia dell'offerta deve coincidere con quella dell'annuncio");
            if ("vendita".equals(offerta.getTipo()) && offerta.getPrezzoOfferto() == null)
                throw new SQLException("Una offerta di vendita deve avere prezzoOfferto valorizzato");
            if (!"vendita".equals(offerta.getTipo()) && offerta.getPrezzoOfferto() != null)
                throw new SQLException("Solo le offerte di tipo vendita possono avere prezzoOfferto valorizzato");

            // 2. INSERIMENTO (all'interno della transazione)
            boolean inserita = offertaDAO.creaOfferta(offerta);
            if (!inserita) throw new SQLException("Errore durante l'inserimento dell'offerta");

            // 3. GESTIONE OGGETTI (all'interno della transazione)
            if ("scambio".equals(offerta.getTipo()) && codiciOggetti != null) {
                for (String codiceOggetto : codiciOggetti) {
                    Oggetto oggetto = oggettoDAO.getOggettoByCodice(codiceOggetto);
                    if (oggetto == null) throw new SQLException("Oggetto " + codiceOggetto + " non trovato");
                    // Assicura che l'offerta abbia un codice prima di creare l'associazione
                    if (offerta.getCodiceOfferta() == null) {
                        // Potrebbe essere necessario recuperare l'offerta appena creata se il DAO non la popola
                        throw new SQLException("Codice offerta mancante per associazione oggetto.");
                    }
                    offreDAO.aggiungiOggettoAScambio(offerta.getCodiceOfferta(), codiceOggetto);
                }
            }

            // --- FINE TRANSAZIONE ---
            conn.commit();
            return true;

        } catch (SQLException e) {
            // Se qualcosa va storto, annulla tutto
            try { conn.rollback(); } catch (SQLException ex) { showError("Errore rollback: " + ex.getMessage()); }
            // Rilancia l'eccezione con il messaggio di errore (es. "Non è possibile inviare...")
            throw e; 
        } finally {
            // Ripristina l'autocommit
            try { conn.setAutoCommit(autoCommitOriginale); } catch (SQLException ex) { showError("Errore ripristino autocommit: " + ex.getMessage()); }
        }
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
        offerta.setData(new Date(System.currentTimeMillis()));
        // Chiamiamo il metodo principale che gestisce anche gli oggetti (passando null per gli oggetti)
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
        offerta.setData(new Date(System.currentTimeMillis()));
        
        // CORREZIONE: Chiamiamo il metodo principale che ha la validazione
        // (passando null per la lista oggetti, visto che non ci sono)
        return inviaOfferta(offerta, null);
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
        offerta.setData(new Date(System.currentTimeMillis()));
        
        // Chiama il metodo generale inviaOfferta e ritorna il suo risultato
        // Questo metodo ora gestirà la creazione dell'offerta E l'associazione degli oggetti
        return inviaOfferta(offerta, codiciOggetti);
    }
    
    // Questi metodi potrebbero non essere più necessari se inviaOffertaConOggetti funziona correttamente
    /*
    private String getUltimoaOffertaCreata() throws SQLException {
        String matricola = getUtenteCorrente().getMatricola();
        List<Offerta> offerte = offertaDAO.getOfferteByUtente(matricola);
        if (offerte.isEmpty()) return null;
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
    */
    
    /**
     * Accetta offerta.
     */
    public boolean accettaOfferta(String codiceOfferta) {
        // Manteniamo la logica transazionale qui per sicurezza
        boolean autoCommitOriginale = false; 
        try {
            autoCommitOriginale = conn.getAutoCommit();
            conn.setAutoCommit(false); // Inizio transazione

            Offerta offerta = offertaDAO.getOffertaByCodice(codiceOfferta);
            if (offerta == null) throw new SQLException("Offerta non trovata");

            boolean ok = offertaDAO.accettaOfferta(codiceOfferta); // Accetta l'offerta
            if (!ok) throw new SQLException("Impossibile accettare l'offerta nel DB.");

            // Rifiuta le altre offerte inviate per lo stesso annuncio
             List<Offerta> altreOfferte = offertaDAO.getOfferteByCodiceAnnuncio(offerta.getCodiceAnnuncio());
            for (Offerta o : altreOfferte) {
                if ("inviata".equals(o.getStato()) && !o.getCodiceOfferta().equals(codiceOfferta)) {
                    offertaDAO.rifiutaOfferta(o.getCodiceOfferta());
                }
            }

            // Determina il nuovo stato dell'annuncio
            String nuovoStatoAnnuncio = switch (offerta.getTipo().toLowerCase()) {
                case "vendita" -> "venduto";
                case "scambio" -> "scambiato";
                case "regalo" -> "regalato";
                default -> "attivo"; // Fallback, non dovrebbe succedere
            };

            // Aggiorna lo stato dell'annuncio
            boolean statoAnnuncioAggiornato = annuncioDAO.aggiornaStatoAnnuncio(offerta.getCodiceAnnuncio(), nuovoStatoAnnuncio);
             if (!statoAnnuncioAggiornato) throw new SQLException("Impossibile aggiornare lo stato dell'annuncio.");

            // Gestione trasferimento oggetti
            Annuncio annuncio = annuncioDAO.getAnnuncioByCodice(offerta.getCodiceAnnuncio()); // Recupera dati annuncio
            String matricolaAnnunciante = annuncio.getMatricola();
            String matricolaRichiedente = offerta.getMatricola();
            
            // Trasferisci oggetto/i DELL'ANNUNCIO al RICHIEDENTE (acquirente/ricevente)
            List<Oggetto> oggettiAnnuncio = getOggettiByAnnuncio(annuncio.getCodiceAnnuncio());
            for (Oggetto obj : oggettiAnnuncio) {
                oggettoDAO.aggiornaOggettoMatricola(obj.getCodiceOggetto(), matricolaRichiedente);
                oggettoDAO.rimuoviAssociazioneAnnuncio(obj.getCodiceOggetto()); 
            }
            
            // SOLO per SCAMBIO: trasferisci oggetti DELL'OFFERTA all'ANNUNCIANTE
            if ("scambio".equalsIgnoreCase(offerta.getTipo())) {
                List<Offre> oggettiOfferti = offreDAO.getOggettiByOfferta(codiceOfferta);
                for (Offre o : oggettiOfferti) {
                    oggettoDAO.aggiornaOggettoMatricola(o.getCodiceOggetto(), matricolaAnnunciante);
                    // Rimuovi associazione annuncio anche per questi oggetti, se presente
                    oggettoDAO.rimuoviAssociazioneAnnuncio(o.getCodiceOggetto()); 
                }
            }

            conn.commit(); // Fine transazione
            return true;

        } catch (SQLException e) {
            showError("Errore accettazione offerta (transazione annullata): " + e.getMessage());
            try { conn.rollback(); } catch (SQLException ex) { showError("Errore rollback: " + ex.getMessage()); }
            return false;
        } finally {
             try { conn.setAutoCommit(autoCommitOriginale); } catch (SQLException ex) { showError("Errore ripristino autocommit: " + ex.getMessage()); }
        }
    }
    
    public boolean aggiornaStatoAnnuncio(String codiceAnnuncio, String nuovoStato) throws SQLException {
        return annuncioDAO.aggiornaStatoAnnuncio(codiceAnnuncio, nuovoStato);
    }



    // Questo metodo specifico per lo scambio potrebbe non essere più necessario
    // dato che accettaOfferta ora gestisce tutti i casi.
    /*
    public boolean accettaScambio(String codiceOfferta, String matricolaRichiedente) {
        // ... (Logica precedente, potenzialmente ridondante) ...
    }
    */


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
     * 1. Marca l'annuncio come "venduto".
     * 2. Crea un'offerta GIA' ACCETTATA per tracciare la vendita.
     * 3. Rifiuta tutte le altre offerte "inviate" per quell'annuncio.
     * 4. Trasferisce l'oggetto dall'annunciante all'acquirente.
     *
     * @param codiceAnnuncio Il codice dell'annuncio da acquistare.
     * @return true se l'acquisto è andato a buon fine.
     * @throws SQLException Se c'è un errore DB o logico.
     */
public boolean compraSubito(String codiceAnnuncio) throws SQLException {
    // Recupera l'annuncio
    Annuncio annuncio = annuncioDAO.getAnnuncioByCodice(codiceAnnuncio);
    if (annuncio == null) {
        throw new SQLException("Annuncio non trovato");
    }
    
    // Verifica che sia attivo e di tipo vendita
    if (!"attivo".equalsIgnoreCase(annuncio.getStato())) {
        throw new SQLException("L'annuncio non è più attivo");
    }
    if (!"vendita".equalsIgnoreCase(annuncio.getTipologia())) {
        throw new SQLException("Il compra subito è disponibile solo per annunci di vendita");
    }
    
    // 1. Cambia lo stato dell'annuncio a "venduto"
    annuncio.setStato("venduto");
    annuncioDAO.aggiornaAnnuncio(annuncio);
    
    // 2. Trasferisci la proprietà degli oggetti associati all'annuncio
    List<Oggetto> oggettiAnnuncio = oggettoDAO.getOggettiByAnnuncio(codiceAnnuncio);
    String compratore = getUtenteCorrente().getMatricola();
    
    for (Oggetto obj : oggettiAnnuncio) {
        oggettoDAO.aggiornaOggettoMatricola(obj.getCodiceOggetto(), compratore);
        oggettoDAO.rimuoviAssociazioneAnnuncio(obj.getCodiceOggetto());
    }
    
    return true;
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
     /**
     * Rimuove associazione annuncio da un oggetto (imposta codiceannuncio a NULL).
     */
     public boolean rimuoviAssociazioneAnnuncioOggetto(String codiceOggetto) throws SQLException {
        return oggettoDAO.rimuoviAssociazioneAnnuncio(codiceOggetto);
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
                 // Conta solo offerte su annunci ancora attivi
                 if ("attivo".equalsIgnoreCase(a.getStato())) {
                    List<Offerta> offerteSuAnnuncio = offertaDAO.getOfferteByCodiceAnnuncio(a.getCodiceAnnuncio());
                    for (Offerta o : offerteSuAnnuncio) {
                        if ("inviata".equalsIgnoreCase(o.getStato())) {
                            offerteRicevute++;
                        }
                    }
                 }
            }
            if(offerteRicevute == 1)
            	notif.add("Hai" + offerteRicevute + " offerta ricevuta da valutare");
            if (offerteRicevute > 1)
                notif.add("Hai " + offerteRicevute + " offerte ricevute da valutare.");

            int annunciScaduti = 0;
            for (Annuncio a : mieiAnnunci) {
                if ("scaduto".equalsIgnoreCase(a.getStato())) {
                    annunciScaduti++;
                }
            }
            if (annunciScaduti == 1)
                notif.add("Hai " + annunciScaduti + " annuncio scaduto.");
            if (annunciScaduti > 1)
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
    public void associaOggettoAdOfferta(String codiceOfferta, String codiceOggetto) throws SQLException {
        offreDAO.creaOffre(new Offre(codiceOfferta, codiceOggetto));
    }

    public String getUltimaOffertaScambioUtente() throws SQLException {
        List<Offerta> offerte = offertaDAO.getOfferteByUtente(getUtenteCorrente().getMatricola());
        for (int i = offerte.size() - 1; i >= 0; i--) {
            if ("scambio".equalsIgnoreCase(offerte.get(i).getTipo())) return offerte.get(i).getCodiceOfferta();
        }
        return null;
    }
}
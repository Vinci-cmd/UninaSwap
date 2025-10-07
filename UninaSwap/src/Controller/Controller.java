package Controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import model.*;
import service.Service;
import model.*;
import java.sql.Date;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Controller dell'applicazione UninaSwap.
 * Gestisce login/registrazione, annunci, offerte e oggetti, delegando la logica al Service.
 */
public class Controller {

    // =========================================================
    // == CAMPI / COSTRUTTORI
    // =========================================================
    private final Service service;
    private Utente utenteCorrente;

    public Controller(Service service) {
        this.service = service;
    }

    // =========================================================
    // == AUTHENTICATION (Login / Registrazione / Logout)
    // =========================================================

    // -------------------- AUTH: Login --------------------
    public boolean login(String email, String password) {
        try {
            String e = email == null ? "" : email.trim();
            String p = password == null ? "" : password.trim();
            if (e.isEmpty() || p.isEmpty()) {
                showError("Email e password sono obbligatorie.");
                return false;
            }
            Utente utente = service.login(e, p); // delega al Service
            if (utente != null) {
                utenteCorrente = utente;
                return true;
            }
        } catch (SQLException ex) {
            showError("Errore login: " + ex.getMessage());
        }
        return false;
    }

    // -------------------- AUTH: Registrazione --------------------
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
            boolean ok = service.creaUtente(nuovo);
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
     * Restituisce l'utente correntemente loggato (può essere null).
     */
    public Utente getUtenteCorrente() {
        return utenteCorrente;
    }
    
    public Utente getUtenteByMatricola(String matricola) throws SQLException {
        return service.getUtenteByMatricola(matricola);
    }
    

    public void logout() {
        utenteCorrente = null;
    }

    // =========================================================
    // == ANNUNCI
    // =========================================================

    public List<Annuncio> getAnnunciAttiviRaw() throws SQLException {
        return service.getAnnunciAttivi();
    }

    public ObservableList<String> getAnnunciAttiviFormatted() {
        try {
            List<Annuncio> annunci = service.getAnnunciAttivi();
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

    public List<Annuncio> getAnnunciByUtente(String matricola) throws SQLException {
        return service.getAnnunciByUtente(matricola);
    }

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
        return service.creaAnnuncio(nuovoAnnuncio);
    }

    public boolean modificaAnnuncio(String codiceAnnuncio,
                                    String categoria,
                                    String tipologia,
                                    String descrizione,
                                    double prezzo,
                                    String stato) throws SQLException {
        Annuncio esistente = service.getAnnuncioByCodice(codiceAnnuncio);

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

        // Usa quello che preferisci tra aggiornaAnnuncio / modificaAnnuncio
        return service.aggiornaAnnuncio(aggiornato);
        // return service.modificaAnnuncio(aggiornato);
    }

    public void eliminaAnnuncio(String codiceAnnuncio) throws SQLException {
        service.eliminaAnnuncio(codiceAnnuncio);
    }

    // =========================================================
    // == OFFERTE
    // =========================================================

    public List<Offerta> getOfferteByAnnuncio(String codiceAnnuncio) throws SQLException {
        return service.getOfferteByAnnuncio(codiceAnnuncio);
    }

    public List<Offerta> getOfferteByUtente(String matricola) throws SQLException {
        return service.getOfferteByUtente(matricola);
    }

    public boolean inviaOfferta(String codiceAnnuncio, String tipo, Double prezzoOfferto) throws SQLException {
        return service.inviaOffertaLogica(codiceAnnuncio, tipo, prezzoOfferto, utenteCorrente.getMatricola());
    }
    public boolean inviaOfferta(String codiceAnnuncio, String tipo, Double prezzoOfferto, String messaggio) throws SQLException {
        return service.inviaOffertaComplessiva(codiceAnnuncio, tipo, prezzoOfferto, messaggio, utenteCorrente.getMatricola());
    }
    
    public boolean inviaOffertaConOggetti(String codiceAnnuncio, List<String> codiciOggetti) throws SQLException {
        return service.inviaOffertaConOggettiLogica(codiceAnnuncio, codiciOggetti, utenteCorrente.getMatricola());
    }

    public boolean accettaOfferta(String codiceOfferta) {
        try {
            return service.accettaOfferta(codiceOfferta);
        } catch (SQLException e) {
            showError("Errore accettazione offerta: " + e.getMessage());
            return false;
        }
    }

    public boolean rifiutaOfferta(String codiceOfferta) {
        try {
            return service.rifiutaOfferta(codiceOfferta);
        } catch (SQLException e) {
            showError("Errore rifiuto offerta: " + e.getMessage());
            return false;
        }
    }
 // =========================================================
 // == STATISTICHE (bridge verso Service)
 // =========================================================

 /** Totale offerte complessive. */
 public int getTotaleOfferte() {
     try {
         return service.getTotaleOfferte();
     } catch (SQLException e) {
         showError("Errore getTotaleOfferte: " + e.getMessage());
         return 0;
     }
 }

 /** Totale offerte per tipologia (es. "vendita", "scambio", ...). */
 public int getTotaleOffertePerTipologia(String tipologia) {
     try {
         return service.getTotaleOffertePerTipologia(tipologia);
     } catch (SQLException e) {
         showError("Errore getTotaleOffertePerTipologia: " + e.getMessage());
         return 0;
     }
 }

 /** Offerte accettate per tipologia. */
 public int getOfferteAccettatePerTipologia(String tipologia) {
     try {
         return service.getOfferteAccettatePerTipologia(tipologia);
     } catch (SQLException e) {
         showError("Errore getOfferteAccettatePerTipologia: " + e.getMessage());
         return 0;
     }
 }

 /** Eventuali statistiche aggiuntive lato service (se ti servono). */
 public double[] getStatisticheVenditeAccettate() {
     try {
         return service.getStatisticheVenditeAccettate();
     } catch (SQLException e) {
         showError("Errore getStatisticheVenditeAccettate: " + e.getMessage());
         return new double[0];
     }
 }

    // =========================================================
    // == OGGETTI
    // =========================================================

    public List<String> getOggettiUtente(String matricola) throws SQLException {
        List<Oggetto> oggetti = service.getOggettiUtente(matricola);
        List<String> codici = new ArrayList<>();
        for (Oggetto o : oggetti) {
            codici.add(o.getCodiceOggetto());
        }
        return codici;
    }
    public List<Oggetto> getOggettiUtenteObj(String matricola) throws SQLException {
        return service.getOggettiUtente(matricola);
    }
    
    public boolean creaOggetto(Oggetto oggetto) throws SQLException {
        String matricola = getUtenteCorrente().getMatricola();
        return service.creaOggetto(oggetto, matricola);
    }
    
    public boolean modificaOggetto(Oggetto oggetto) throws SQLException {
    	return service.modificaOggetto(oggetto);
    }
    
    public boolean eliminaOggetto(String codiceOggetto) throws SQLException{
        return service.eliminaOggetto(codiceOggetto);
    }	
    public void aggiornaCodiceAnnuncioOggetto(String codiceOggetto, String codiceAnnuncio) throws SQLException {
        service.aggiornaCodiceAnnuncioOggetto(codiceOggetto, codiceAnnuncio);
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

package Controller;

import javafx.collections.FXCollections;
import java.util.*;
import javafx.collections.ObservableList;
import javafx.scene.chart.XYChart; // <-- IMPORT AGGIUNTO
import model.*;
import service.Service;
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
    // Ottiene solo le offerte inviate dall'utente (non ricevute)
    public List<Offerta> getOfferteInviateByUtente(String matricola) throws SQLException {
        return service.getOfferteInviateByUtente(matricola);
    }

    // Recupera la descrizione di un annuncio dato il codice
    public String getDescrizioneAnnuncio(String codiceAnnuncio) throws SQLException {
        return service.getDescrizioneAnnuncio(codiceAnnuncio);
    }

    public List<Offerta> getOfferteRicevuteByUtente(String matricola) throws SQLException {
        return service.getOfferteRicevuteByUtente(matricola);
    }

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
    
    public List<String> getNotificheUtente(String matricola) {
        List<String> notif = new ArrayList<>();
        try {
            // 1. Ottieni i tuoi annunci
            List<Annuncio> mieiAnnunci = getAnnunciByUtente(matricola);
            int offerteRicevute = 0;
            
            // 2. Per ogni annuncio, carica le sue offerte e filtra quelle "inviata"
            for (Annuncio a : mieiAnnunci) {
                List<Offerta> offerteSuAnnuncio = service.getOfferteByCodiceAnnuncio(a.getCodiceAnnuncio()); // <-- serve/implementa questo metodo se non esiste!
                for (Offerta o : offerteSuAnnuncio) {
                    if ("inviata".equalsIgnoreCase(o.getStato())) {
                        offerteRicevute++;
                    }
                }
            }
            if (offerteRicevute > 0)
                notif.add("Hai " + offerteRicevute + " offerte ricevute da accettare.");

            // Annunci scaduti (già va bene come hai)
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

    
    public boolean eliminaOfferta(String codiceOfferta) throws SQLException {
        return service.eliminaOfferta(codiceOfferta);
    }
    
    // =========================================================
    // == NUOVI METODI PER REPORTVIEW (INIZIO)
    // =========================================================

    public Map<String, Integer> getStatisticheGenerali(boolean sempre) throws SQLException {
        // TODO: Implementare la logica nel Service (e DAO) per recuperare le statistiche
        // Questa è solo una implementazione di esempio con dati fittizi.
        Map<String, Integer> stats = new HashMap<>();
        stats.put("annunci", 18);
        stats.put("offerteInviate", 27);
        // Tasso di successo come intero (es. 35 per 35%)
        stats.put("tassoSuccesso", 35);
        return stats;
    }

    public Map<String, Integer> getDatiGraficoTortaTipologie() throws SQLException {
        // TODO: Implementare la logica nel Service (e DAO)
        Map<String, Integer> dati = new HashMap<>();
        dati.put("Vendita", 10);
        dati.put("Scambio", 5);
        dati.put("Regalo", 3);
        return dati;
    }

    public Map<String, Integer> getDatiGraficoBarreOfferte() throws SQLException {
        // TODO: Implementare la logica nel Service (e DAO)
        Map<String, Integer> dati = new HashMap<>();
        dati.put("InviateVendita", 15);
        dati.put("InviateScambio", 8);
        dati.put("InviateRegalo", 4);
        dati.put("RicevuteVendita", 12);
        dati.put("RicevuteScambio", 10);
        dati.put("RicevuteRegalo", 2);
        return dati;
    }
    
    public Map<String, XYChart.Series<String, Number>> getDatiGraficoAndamento() throws SQLException {
        // TODO: Implementare la logica nel Service (e DAO) per l'andamento degli ultimi 30 giorni
        Map<String, XYChart.Series<String, Number>> dati = new HashMap<>();
        
        XYChart.Series<String, Number> annunciSeries = new XYChart.Series<>();
        annunciSeries.setName("Annunci Pubblicati");
        // Dati di esempio
        annunciSeries.getData().add(new XYChart.Data<>("1 Mag", 1));
        annunciSeries.getData().add(new XYChart.Data<>("5 Mag", 3));
        annunciSeries.getData().add(new XYChart.Data<>("12 Mag", 5));
        annunciSeries.getData().add(new XYChart.Data<>("20 Mag", 8));
        annunciSeries.getData().add(new XYChart.Data<>("28 Mag", 10));

        XYChart.Series<String, Number> offerteSeries = new XYChart.Series<>();
        offerteSeries.setName("Offerte Inviate");
        // Dati di esempio
        offerteSeries.getData().add(new XYChart.Data<>("1 Mag", 2));
        offerteSeries.getData().add(new XYChart.Data<>("5 Mag", 5));
        offerteSeries.getData().add(new XYChart.Data<>("12 Mag", 8));
        offerteSeries.getData().add(new XYChart.Data<>("20 Mag", 12));
        offerteSeries.getData().add(new XYChart.Data<>("28 Mag", 15));

        dati.put("annunci", annunciSeries);
        dati.put("offerte", offerteSeries);
        
        return dati;
    }

    // =========================================================
    // == NUOVI METODI PER REPORTVIEW (FINE)
    // =========================================================

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
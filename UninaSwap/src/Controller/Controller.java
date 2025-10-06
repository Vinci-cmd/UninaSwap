package Controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.Alert;
import model.*;
import service.Service;

import java.sql.Date;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

public class Controller {
    private Service service;
    private Utente utenteCorrente;

    public Controller(Service service) {
        this.service = service;
    }

    // Metodo login con validazioni e gestione utente corrente
    public boolean login(String email, String password) {
        if (email == null || email.trim().isEmpty()) {
            showError("Email obbligatoria");
            return false;
        }
        if (password == null || password.trim().isEmpty()) {
            showError("Password obbligatoria");
            return false;
        }
        try {
            Utente utente = service.login(email.trim(), password);
            if (utente != null) {
                utenteCorrente = utente;
                return true;
            } else {
                showError("Credenziali non valide");
                return false;
            }
        } catch (SQLException e) {
            showError("Errore durante il login: " + e.getMessage());
            return false;
        }
    }

    public Utente getUtenteCorrente() {
        return utenteCorrente;
    }

    public void logout() {
        utenteCorrente = null;
    }

    // Recupera annunci attivi formattati
    public ObservableList<String> getAnnunciAttiviFormatted() {
        try {
            List<Annuncio> annunci = service.getAnnunciAttivi();
            return formatAnnunciForList(annunci);
        } catch (SQLException e) {
            showError("Errore caricamento annunci: " + e.getMessage());
            return FXCollections.observableArrayList("Errore caricamento dati");
        }
    }

    // Recupera annunci filtrati formattati
    public ObservableList<String> getAnnunciFiltrati(String categoria, String tipologia) {
        try {
            String cat = (categoria != null && categoria.trim().isEmpty()) ? null : categoria;
            String tip = (tipologia != null && tipologia.trim().isEmpty()) ? null : tipologia;

            List<Annuncio> annunci = service.getAnnunciFiltrati(cat, tip);
            return formatAnnunciForList(annunci);
        } catch (SQLException e) {
            showError("Errore filtro annunci: " + e.getMessage());
            return FXCollections.observableArrayList("Errore caricamento dati");
        }
    }

    public ObservableList<String> getMieiAnnunci() {
        if (utenteCorrente == null)
            return FXCollections.observableArrayList("Utente non autenticato");

        try {
            List<Annuncio> annunci = service.getAnnunciByUtente(utenteCorrente.getMatricola());
            return formatAnnunciForList(annunci);
        } catch (SQLException e) {
            showError("Errore caricamento tuoi annunci: " + e.getMessage());
            return FXCollections.observableArrayList("Errore caricamento dati");
        }
    }

    public List<Annuncio> getAnnunciAttiviRaw() throws SQLException {
        return service.getAnnunciAttivi();
    }

    public List<Offerta> getOfferteByAnnuncio(String codiceAnnuncio) throws SQLException {
        return service.getOfferteByAnnuncio(codiceAnnuncio);
    }

    // Crea annuncio con validazione
    public boolean creaAnnuncio(String descrizione, String categoria, String tipologia, String prezzoStr) {
        if (utenteCorrente == null) {
            showError("Utente non autenticato");
            return false;
        }

        if (descrizione == null || descrizione.trim().isEmpty()) {
            showError("Descrizione obbligatoria");
            return false;
        }
        if (categoria == null || categoria.trim().isEmpty()) {
            showError("Categoria obbligatoria");
            return false;
        }
        if (tipologia == null || tipologia.trim().isEmpty()) {
            showError("Tipologia obbligatoria");
            return false;
        }

        Double prezzo = null;
        if ("vendita".equalsIgnoreCase(tipologia.trim())) {
            if (prezzoStr == null || prezzoStr.trim().isEmpty()) {
                showError("Prezzo obbligatorio per le vendite");
                return false;
            }
            try {
                prezzo = Double.parseDouble(prezzoStr.trim());
                if (prezzo <= 0) {
                    showError("Il prezzo deve essere maggiore di 0");
                    return false;
                }
            } catch (NumberFormatException e) {
                showError("Prezzo non valido");
                return false;
            }
        }

        try {
            Annuncio annuncio = new Annuncio(
                    generateAnnuncioCode(),
                    descrizione.trim(),
                    categoria.trim(),
                    tipologia.trim().toLowerCase(),
                    prezzo,
                    "attivo",
                    new Date(System.currentTimeMillis()),
                    utenteCorrente.getMatricola()
            );

            return service.creaAnnuncio(annuncio);
        } catch (SQLException e) {
            showError("Errore creazione annuncio: " + e.getMessage());
            return false;
        }
    }

    // Accetta un'offerta tramite Service
    public boolean accettaOfferta(String codiceOfferta) {
        try {
            boolean result = service.accettaOfferta(codiceOfferta);
            if (result) {
                showInfo("Offerta accettata con successo!");
            }
            return result;
        } catch (SQLException e) {
            showError("Errore accettazione offerta: " + e.getMessage());
            return false;
        }
    }

    // Rifiuta un'offerta tramite Service
    public boolean rifiutaOfferta(String codiceOfferta) {
        try {
            boolean result = service.rifiutaOfferta(codiceOfferta);
            if (result) {
                showInfo("Offerta rifiutata");
            }
            return result;
        } catch (SQLException e) {
            showError("Errore rifiuto offerta: " + e.getMessage());
            return false;
        }
    }

    // Recupera storico offerte utente (in formato ObservableList<String>)
    public ObservableList<String> getStoricoOfferte() {
        if (utenteCorrente == null) {
            return FXCollections.observableArrayList("Utente non autenticato");
        }
        try {
            List<Offerta> offerte = service.getOfferteByUtente(utenteCorrente.getMatricola());
            ObservableList<String> items = FXCollections.observableArrayList();

            for (Offerta offerta : offerte) {
                String formatted = String.format("[%s] %s - %s - %s",
                        offerta.getTipo().toUpperCase(),
                        offerta.getCodiceAnnuncio(),
                        offerta.getPrezzoOfferto() != null ? "€" + offerta.getPrezzoOfferto() : "N/A",
                        offerta.getStato().toUpperCase());
                items.add(formatted);
            }

            if (items.isEmpty()) {
                items.add("Nessuna offerta trovata");
            }
            return items;
        } catch (SQLException e) {
            showError("Errore caricamento offerte: " + e.getMessage());
            return FXCollections.observableArrayList("Errore caricamento dati");
        }
    }

    // Invia offerta semplice (vendita/regalo) - usa il metodo esistente del Service
    public boolean inviaOfferta(String codiceAnnuncio, String tipo, Double prezzoOfferto) {
        if (utenteCorrente == null) {
            showError("Utente non autenticato");
            return false;
        }

        if (codiceAnnuncio == null || codiceAnnuncio.trim().isEmpty()) {
            showError("Annuncio non selezionato");
            return false;
        }

        if (tipo == null || tipo.trim().isEmpty()) {
            showError("Tipo offerta obbligatorio");
            return false;
        }

        // Validazione prezzo per offerte di vendita
        if ("vendita".equalsIgnoreCase(tipo.trim()) && (prezzoOfferto == null || prezzoOfferto <= 0)) {
            showError("Prezzo obbligatorio per offerte di vendita");
            return false;
        }

        try {
            // Crea offerta con costruttore corretto
            Offerta offerta = new Offerta(
                    generateOffertaCode(),
                    codiceAnnuncio,
                    utenteCorrente.getMatricola(),
                    tipo.trim().toLowerCase(),
                    prezzoOfferto,
                    "inviata",
                    new Date(System.currentTimeMillis())
            );

            // Usa il metodo Service esistente per offerta semplice
            boolean result = service.inviaOfferta(offerta, null); // null per la lista oggetti
            if (result) {
                showInfo("Offerta inviata con successo!");
            }
            return result;
        } catch (SQLException e) {
            showError("Errore invio offerta: " + e.getMessage());
            return false;
        }
    }

    // Invia offerta con oggetti (per scambi) - usa il metodo esistente del Service
    public boolean inviaOffertaConOggetti(String codiceAnnuncio, List<String> codiciOggetti) {
        if (utenteCorrente == null) {
            showError("Utente non autenticato");
            return false;
        }

        if (codiceAnnuncio == null || codiceAnnuncio.trim().isEmpty()) {
            showError("Annuncio non selezionato");
            return false;
        }

        if (codiciOggetti == null || codiciOggetti.isEmpty()) {
            showError("Seleziona almeno un oggetto per lo scambio");
            return false;
        }

        try {
            // Crea offerta con costruttore corretto
            Offerta offerta = new Offerta(
                    generateOffertaCode(),
                    codiceAnnuncio,
                    utenteCorrente.getMatricola(),
                    "scambio",
                    null,
                    "inviata",
                    new Date(System.currentTimeMillis())
            );

            // Usa il metodo Service esistente per offerta con oggetti
            boolean result = service.inviaOfferta(offerta, codiciOggetti);
            if (result) {
                showInfo("Offerta di scambio inviata con successo!");
            }
            return result;
        } catch (SQLException e) {
            showError("Errore invio offerta: " + e.getMessage());
            return false;
        }
    }

    // Recupera offerte ricevute sui propri annunci
    public ObservableList<String> getOfferteRicevute() {
        if (utenteCorrente == null) {
            return FXCollections.observableArrayList("Utente non autenticato");
        }

        try {
            List<Offerta> offerte = service.getOfferteRicevuteByUtente(utenteCorrente.getMatricola());
            ObservableList<String> items = FXCollections.observableArrayList();

            for (Offerta offerta : offerte) {
                String prezzoStr = "N/A";
                if (offerta.getPrezzoOfferto() != null) {
                    prezzoStr = "€" + offerta.getPrezzoOfferto();
                }
                
                String formatted = String.format("[%s] %s - %s - %s - %s",
                        offerta.getStato().toUpperCase(),
                        offerta.getCodiceAnnuncio(),
                        offerta.getTipo().toUpperCase(),
                        prezzoStr,
                        offerta.getDataOfferta());
                items.add(formatted);
            }

            if (items.isEmpty()) {
                items.add("Nessuna offerta ricevuta");
            }
            return items;
        } catch (SQLException e) {
            showError("Errore caricamento offerte ricevute: " + e.getMessage());
            return FXCollections.observableArrayList("Errore caricamento dati");
        }
    }

    // Ottiene oggetti disponibili per un annuncio (per scambi)
    public List<Oggetto> getOggettiByAnnuncio(String codiceAnnuncio) throws SQLException {
        return service.getOggettiByAnnuncio(codiceAnnuncio);
    }

    private ObservableList<String> formatAnnunciForList(List<Annuncio> annunci) {
        ObservableList<String> items = FXCollections.observableArrayList();

        for (Annuncio a : annunci) {
            String formatted = String.format("[%s] %s - %s - %s",
                    a.getTipologia().toUpperCase(),
                    a.getCategoria(),
                    a.getPrezzo() != null ? "€" + a.getPrezzo() : "Gratis",
                    a.getStato().toUpperCase()
            );
            items.add(formatted);
        }

        if (items.isEmpty()) {
            items.add("Nessun annuncio trovato");
        }

        return items;
    }

    private String generateAnnuncioCode() {
        return "ANN_" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    // Genera codice univoco per offerta
    private String generateOffertaCode() {
        return "OFF_" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Errore");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showInfo(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Informazione");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public ReportData getReportData() {
        try {
            int totaleOfferte = service.getTotaleOfferte();
            int offerteVendita = service.getTotaleOffertePerTipologia("vendita");
            int offerteScambio = service.getTotaleOffertePerTipologia("scambio");
            int offerteRegalo = service.getTotaleOffertePerTipologia("regalo");

            int accettateVendita = service.getOfferteAccettatePerTipologia("vendita");
            int accettateScambio = service.getOfferteAccettatePerTipologia("scambio");
            int accettateRegalo = service.getOfferteAccettatePerTipologia("regalo");

            double[] statisticheVendite = service.getStatisticheVenditeAccettate();

            return new ReportData(
                    totaleOfferte, offerteVendita, offerteScambio, offerteRegalo,
                    accettateVendita, accettateScambio, accettateRegalo,
                    statisticheVendite[0], statisticheVendite[1], statisticheVendite[2]
            );
        } catch (SQLException e) {
            showError("Errore generazione report: " + e.getMessage());
            return null;
        }
    }

    public static class ReportData {
        public final int totaleOfferte;
        public final int offerteVendita, offerteScambio, offerteRegalo;
        public final int accettateVendita, accettateScambio, accettateRegalo;
        public final double minPrezzo, maxPrezzo, avgPrezzo;

        public ReportData(int totaleOfferte, int offerteVendita, int offerteScambio, int offerteRegalo,
                          int accettateVendita, int accettateScambio, int accettateRegalo,
                          double minPrezzo, double maxPrezzo, double avgPrezzo) {
            this.totaleOfferte = totaleOfferte;
            this.offerteVendita = offerteVendita;
            this.offerteScambio = offerteScambio;
            this.offerteRegalo = offerteRegalo;
            this.accettateVendita = accettateVendita;
            this.accettateScambio = accettateScambio;
            this.accettateRegalo = accettateRegalo;
            this.minPrezzo = minPrezzo;
            this.maxPrezzo = maxPrezzo;
            this.avgPrezzo = avgPrezzo;
        }
    }
}

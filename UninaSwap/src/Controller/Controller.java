package Controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import model.*;
import model.Offerta;
import service.Service;
import model.Oggetto;
import model.Utente;
import java.util.ArrayList;
import java.sql.Date;
import java.sql.SQLException;
import java.util.List;

public class Controller {
    private Service service;
    private Utente utenteCorrente;

    public Controller(Service service) {
        this.service = service;
    }

    // Login utente e memorizzazione
    public boolean login(String email, String password) {
        try {
            Utente utente = service.login(email, password);
            if (utente != null) {
                utenteCorrente = utente;
                return true;
            }
        } catch (SQLException e) {
            showError("Errore login: " + e.getMessage());
        }
        return false;
    }

    public Utente getUtenteCorrente() {
        return utenteCorrente;
    }

    public void logout() {
        utenteCorrente = null;
    }

    // Lista annunci attivi raw (oggetti Annuncio)
    public List<Annuncio> getAnnunciAttiviRaw() throws SQLException {
        return service.getAnnunciAttivi();
    }

    // Lista annunci attivi da mostrare (formattati)
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

    // Offerte per annuncio specifico
    public List<Offerta> getOfferteByAnnuncio(String codiceAnnuncio) throws SQLException {
        return service.getOfferteByAnnuncio(codiceAnnuncio);
    }

    // Invio offerta semplice (vendita, regalo)
    public boolean inviaOfferta(String codiceAnnuncio, String tipo, Double prezzoOfferto) throws SQLException {
        // Puoi implementare la logica di creazione offerta e chiamare service relativo
        return service.inviaOffertaLogica(codiceAnnuncio, tipo, prezzoOfferto, utenteCorrente.getMatricola());
    }

    // Invio offerta con oggetti (scambio)
    public boolean inviaOffertaConOggetti(String codiceAnnuncio, List<String> codiciOggetti) throws SQLException {
        return service.inviaOffertaConOggettiLogica(codiceAnnuncio, codiciOggetti, utenteCorrente.getMatricola());
    }

    // Accetta offerta
    public boolean accettaOfferta(String codiceOfferta) {
        try {
            return service.accettaOfferta(codiceOfferta);
        } catch (SQLException e) {
            showError("Errore accettazione offerta: " + e.getMessage());
            return false;
        }
    }

    // Rifiuta offerta
    public boolean rifiutaOfferta(String codiceOfferta) {
        try {
            return service.rifiutaOfferta(codiceOfferta);
        } catch (SQLException e) {
            showError("Errore rifiuto offerta: " + e.getMessage());
            return false;
        }
    }

    private void showError(String message) {
        System.err.println(message);
        // Puoi espandere con Alert JavaFX per messaggi grafici
    }
    
    // Recupera tutti gli annunci di uno specifico utente
    public List<Annuncio> getAnnunciByUtente(String matricola) throws SQLException {
        return service.getAnnunciByUtente(matricola);
    }

    // Recupera tutte le offerte fatte da un utente
    public List<Offerta> getOfferteByUtente(String matricola) throws SQLException {
        return service.getOfferteByUtente(matricola);
    }

    // Recupera oggetti di un utente, parametro matricola
    public List<String> getOggettiUtente(String matricola) throws SQLException {
        List<Oggetto> oggetti = service.getOggettiUtente(matricola);
        List<String> codiciOggetti = new ArrayList<>();
        for (Oggetto o : oggetti) {
            codiciOggetti.add(o.getCodiceOggetto()); // metti il campo che ti serve
        }
        return codiciOggetti;
    }
 // CREA NUOVO ANNUNCIO
    public boolean creaAnnuncio(String categoria,
            String tipologia,
            String descrizione,
            double prezzo,
            String stato) throws SQLException {
    	// Costruisci dataPubblicazione come java.sql.Date
    	Date oggi = new Date(System.currentTimeMillis());

    	Annuncio nuovoAnnuncio = new Annuncio(
    			null,               // codiceAnnuncio (autogenerato in DAO)
    			descrizione,        // descrizione
    			categoria,          // categoria
    			tipologia,          // tipologia
    			prezzo,             // prezzo (0.0 se non vendita)
    			stato,              // stato
    			oggi,               // dataPubblicazione
    			utenteCorrente.getMatricola()  // matricola
);
return service.creaAnnuncio(nuovoAnnuncio);
}

    // MODIFICA ANNUNCIO ESISTENTE
    public boolean modificaAnnuncio(String codiceAnnuncio, String categoria, String tipologia, String descrizione, double prezzo, String stato) throws SQLException {
        // Recupera l'annuncio esistente per mantenere dataPubblicazione e matricola originali
        Annuncio esistente = service.getAnnuncioByCodice(codiceAnnuncio);
        
        // Crea oggetto aggiornato mantenendo i campi che non cambiano
        Annuncio aggiornato = new Annuncio(
            codiceAnnuncio,
            descrizione,
            categoria,
            tipologia,
            prezzo,
            stato,
            esistente.getDataPubblicazione(), // mantieni data originale
            esistente.getMatricola() // mantieni matricola originale
        );
        return service.modificaAnnuncio(aggiornato);
    }

    // ELIMINA ANNUNCIO
    public void eliminaAnnuncio(String codiceAnnuncio) throws SQLException {
        service.eliminaAnnuncio(codiceAnnuncio);
    }

}

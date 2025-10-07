package Controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import model.*;
import service.Service;

import java.sql.Date;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class Controller {
    private Service service;
    private Utente utenteCorrente;

    public Controller(Service service) {
        this.service = service;
    }

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

    public List<Offerta> getOfferteByAnnuncio(String codiceAnnuncio) throws SQLException {
        return service.getOfferteByAnnuncio(codiceAnnuncio);
    }

    public boolean inviaOfferta(String codiceAnnuncio, String tipo, Double prezzoOfferto) throws SQLException {
        return service.inviaOffertaLogica(codiceAnnuncio, tipo, prezzoOfferto, utenteCorrente.getMatricola());
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

    private void showError(String message) {
        System.err.println(message);
    }

    public List<Annuncio> getAnnunciByUtente(String matricola) throws SQLException {
        return service.getAnnunciByUtente(matricola);
    }

    public List<Offerta> getOfferteByUtente(String matricola) throws SQLException {
        return service.getOfferteByUtente(matricola);
    }

    public List<String> getOggettiUtente(String matricola) throws SQLException {
        List<Oggetto> oggetti = service.getOggettiUtente(matricola);
        List<String> codici = new ArrayList<>();
        for (Oggetto o : oggetti) {
            codici.add(o.getCodiceOggetto());
        }
        return codici;
    }

    public boolean creaAnnuncio(String categoria, String tipologia, String descrizione, double prezzo) throws SQLException {
        Date oggi = new Date(System.currentTimeMillis());
        Annuncio nuovoAnnuncio = new Annuncio(
            null,
            descrizione,
            categoria,
            tipologia,
            prezzo,
            "attivo", // stato impostato automaticamente
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
        return service.modificaAnnuncio(aggiornato);
    }

    public void eliminaAnnuncio(String codiceAnnuncio) throws SQLException {
        service.eliminaAnnuncio(codiceAnnuncio);
    }
}

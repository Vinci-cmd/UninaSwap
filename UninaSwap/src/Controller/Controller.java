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

public class Controller {
    private AnnuncioDAO annuncioDAO;
    private OffertaDAO offertaDAO;
    private OffreDAO offreDAO;
    private OggettoDAO oggettoDAO;
    private UtenteDAO utenteDAO;
    private Utente utenteCorrente;
    private Connection conn;

    public Controller(Connection conn) {
        this.conn = conn;
        this.annuncioDAO = new AnnuncioDAO(conn);
        this.offertaDAO = new OffertaDAO(conn);
        this.offreDAO = new OffreDAO(conn);
        this.oggettoDAO = new OggettoDAO(conn);
        this.utenteDAO = new UtenteDAO(conn);
    }

    public boolean login(String email, String password) {
        try {
            String e = safeTrim(email);
            String p = safeTrim(password);
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

    public void logout() {
        utenteCorrente = null;
    }

    public Utente getUtenteCorrente() {
        return utenteCorrente;
    }

    public Utente getUtenteByMatricola(String matricola) throws SQLException {
        return utenteDAO.getUtenteByMatricola(matricola);
    }

    public boolean aggiornaUtente(Utente utente) throws SQLException {
        return utenteDAO.aggiornaUtente(utente);
    }

    public boolean eliminaUtente(String matricola) throws SQLException {
        return utenteDAO.eliminaUtente(matricola);
    }

    public List<Annuncio> getAnnunciAttiviRaw() throws SQLException {
        return annuncioDAO.getAnnunciAttivi();
    }

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

    public List<Annuncio> getAnnunciByUtente(String matricola) throws SQLException {
        return annuncioDAO.getAnnunciByUtente(matricola);
    }

    public Annuncio getAnnuncioByCodice(String codiceAnnuncio) throws SQLException {
        return annuncioDAO.getAnnuncioByCodice(codiceAnnuncio);
    }

    public List<Annuncio> getAnnunciFiltrati(String categoria, String tipologia) throws SQLException {
        return annuncioDAO.getAnnunciFiltrati(categoria, tipologia);
    }

    public void associaOggettoEAttivaAnnuncio(String codiceOggetto, String codiceAnnuncio) throws SQLException {
        boolean auto = conn.getAutoCommit();
        try {
            conn.setAutoCommit(false);
            oggettoDAO.aggiornaCodiceAnnuncioOggetto(codiceOggetto, codiceAnnuncio);
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

            Oggetto ogg = oggettoDAO.getOggettoByCodice(codiceOggetto);
            if (ogg == null) throw new SQLException("Oggetto non trovato: " + codiceOggetto);

            String codiceAnnuncio = ogg.getCodiceAnnuncio();
            oggettoDAO.rimuoviAssociazioneAnnuncio(codiceOggetto);

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

    public String getDescrizioneAnnuncio(String codiceAnnuncio) throws SQLException {
        Annuncio ann = annuncioDAO.getAnnuncioByCodice(codiceAnnuncio);
        return ann != null ? ann.getDescrizione() : "";
    }

    public boolean creaAnnuncio(String categoria, String tipologia, String descrizione, double prezzo, String codiceOggetto) throws SQLException {
        if (codiceOggetto == null || codiceOggetto.isEmpty()) {
            throw new SQLException("Devi associare obbligatoriamente un tuo oggetto all'annuncio!");
        }

        boolean auto = conn.getAutoCommit();
        try {
            conn.setAutoCommit(false);

            Date oggi = new Date(System.currentTimeMillis());
            Double prezzoBoxed = "vendita".equalsIgnoreCase(tipologia) ? prezzo : null;

            Annuncio nuovoAnnuncio = new Annuncio(
                null, descrizione, categoria, tipologia, prezzoBoxed, "attivo", oggi, utenteCorrente.getMatricola()
            );
            boolean created = annuncioDAO.creaAnnuncio(nuovoAnnuncio);
            if (!created) throw new SQLException("Creazione annuncio fallita");

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

    public boolean modificaAnnuncio(String codiceAnnuncio, String categoria, String tipologia, String descrizione, double prezzo, String stato) throws SQLException {
        Annuncio esistente = annuncioDAO.getAnnuncioByCodice(codiceAnnuncio);
        if (esistente == null) throw new SQLException("Annuncio non trovato");

        Double prezzoBoxed = "vendita".equalsIgnoreCase(tipologia) ? prezzo : null;

        Annuncio aggiornato = new Annuncio(
            codiceAnnuncio, descrizione, categoria, tipologia, prezzoBoxed,
            stato, esistente.getDataPubblicazione(), esistente.getMatricola()
        );

        return annuncioDAO.aggiornaAnnuncio(aggiornato);
    }

    public void eliminaAnnuncio(String codiceAnnuncio) throws SQLException {
        annuncioDAO.eliminaAnnuncio(codiceAnnuncio);
    }

    public List<Offerta> getOfferteRicevuteByUtente(String matricola) throws SQLException {
        return offertaDAO.getOfferteRicevuteByUtente(matricola);
    }

    public List<Offerta> getOfferteInviateByUtente(String matricola) throws SQLException {
        return offertaDAO.getOfferteInviateByUtente(matricola);
    }

    public List<Offerta> getOfferteByAnnuncio(String codiceAnnuncio) throws SQLException {
        return offertaDAO.getOfferteByAnnuncio(codiceAnnuncio);
    }

    public List<Offerta> getOfferteByUtente(String matricola) throws SQLException {
        return offertaDAO.getOfferteByUtente(matricola);
    }

    public Offerta getOffertaByCodice(String codiceOfferta) throws SQLException {
        return offertaDAO.getOffertaByCodice(codiceOfferta);
    }

    public List<Offerta> getOfferteByCodiceAnnuncio(String codiceAnnuncio) throws SQLException {
        return offertaDAO.getOfferteByCodiceAnnuncio(codiceAnnuncio);
    }

    public boolean inviaOfferta(Offerta offerta, List<String> codiciOggetti) throws SQLException {
        boolean autoCommitOriginale = conn.getAutoCommit();
        try {
            conn.setAutoCommit(false);

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
                    if (offerta.getCodiceOfferta() == null) {
                        throw new SQLException("Codice offerta mancante per associazione oggetto.");
                    }
                    offreDAO.aggiungiOggettoAScambio(offerta.getCodiceOfferta(), codiceOggetto);
                }
            }

            conn.commit();
            return true;
        } catch (SQLException e) {
            try { conn.rollback(); } catch (SQLException ex) { showError("Errore rollback: " + ex.getMessage()); }
            throw e;
        } finally {
            try { conn.setAutoCommit(autoCommitOriginale); } catch (SQLException ex) { showError("Errore ripristino autocommit: " + ex.getMessage()); }
        }
    }

    public boolean inviaOfferta(String codiceAnnuncio, String tipo, Double prezzoOfferto) throws SQLException {
        Offerta offerta = new Offerta();
        offerta.setCodiceAnnuncio(codiceAnnuncio);
        offerta.setTipo(tipo);
        offerta.setPrezzoOfferto(prezzoOfferto);
        offerta.setMatricola(utenteCorrente.getMatricola());
        offerta.setStato("inviata");
        offerta.setCodiceOfferta(UUID.randomUUID().toString());
        offerta.setData(new Date(System.currentTimeMillis()));
        return inviaOfferta(offerta, null);
    }

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
        return inviaOfferta(offerta, null);
    }

    public boolean inviaOffertaConOggetti(String codiceAnnuncio, List<String> codiciOggetti) throws SQLException {
        Offerta offerta = new Offerta();
        offerta.setCodiceAnnuncio(codiceAnnuncio);
        offerta.setTipo("scambio");
        offerta.setMatricola(utenteCorrente.getMatricola());
        offerta.setStato("inviata");
        offerta.setCodiceOfferta(UUID.randomUUID().toString());
        offerta.setData(new Date(System.currentTimeMillis()));
        return inviaOfferta(offerta, codiciOggetti);
    }

    public boolean accettaOfferta(String codiceOfferta) {
        boolean autoCommitOriginale = false;
        try {
            autoCommitOriginale = conn.getAutoCommit();
            conn.setAutoCommit(false);

            Offerta offerta = offertaDAO.getOffertaByCodice(codiceOfferta);
            if (offerta == null) throw new SQLException("Offerta non trovata");

            boolean ok = offertaDAO.accettaOfferta(codiceOfferta);
            if (!ok) throw new SQLException("Impossibile accettare l'offerta nel DB.");

            List<Offerta> altreOfferte = offertaDAO.getOfferteByCodiceAnnuncio(offerta.getCodiceAnnuncio());
            for (Offerta o : altreOfferte) {
                if ("inviata".equals(o.getStato()) && !o.getCodiceOfferta().equals(codiceOfferta)) {
                    offertaDAO.rifiutaOfferta(o.getCodiceOfferta());
                }
            }

            String nuovoStatoAnnuncio = switch (offerta.getTipo().toLowerCase()) {
                case "vendita" -> "venduto";
                case "scambio" -> "scambiato";
                case "regalo" -> "regalato";
                default -> "attivo";
            };

            boolean statoAnnuncioAggiornato = annuncioDAO.aggiornaStatoAnnuncio(offerta.getCodiceAnnuncio(), nuovoStatoAnnuncio);
            if (!statoAnnuncioAggiornato) throw new SQLException("Impossibile aggiornare lo stato dell'annuncio.");

            Annuncio annuncio = annuncioDAO.getAnnuncioByCodice(offerta.getCodiceAnnuncio());
            String matricolaAnnunciante = annuncio.getMatricola();
            String matricolaRichiedente = offerta.getMatricola();

            List<Oggetto> oggettiAnnuncio = getOggettiByAnnuncio(annuncio.getCodiceAnnuncio());
            for (Oggetto obj : oggettiAnnuncio) {
                oggettoDAO.aggiornaOggettoMatricola(obj.getCodiceOggetto(), matricolaRichiedente);
                oggettoDAO.rimuoviAssociazioneAnnuncio(obj.getCodiceOggetto());
            }

            if ("scambio".equalsIgnoreCase(offerta.getTipo())) {
                List<Offre> oggettiOfferti = offreDAO.getOggettiByOfferta(codiceOfferta);
                for (Offre o : oggettiOfferti) {
                    oggettoDAO.aggiornaOggettoMatricola(o.getCodiceOggetto(), matricolaAnnunciante);
                    oggettoDAO.rimuoviAssociazioneAnnuncio(o.getCodiceOggetto());
                }
            }

            conn.commit();
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

    public boolean rifiutaOfferta(String codiceOfferta) {
        try {
            return offertaDAO.rifiutaOfferta(codiceOfferta);
        } catch (SQLException e) {
            showError("Errore rifiuto offerta: " + e.getMessage());
            return false;
        }
    }

    public boolean eliminaOfferta(String codiceOfferta) throws SQLException {
        return offertaDAO.eliminaOfferta(codiceOfferta);
    }

    public boolean compraSubito(String codiceAnnuncio) throws SQLException {
        Annuncio annuncio = annuncioDAO.getAnnuncioByCodice(codiceAnnuncio);
        if (annuncio == null) {
            throw new SQLException("Annuncio non trovato");
        }

        if (!"attivo".equalsIgnoreCase(annuncio.getStato())) {
            throw new SQLException("L'annuncio non è più attivo");
        }
        if (!"vendita".equalsIgnoreCase(annuncio.getTipologia())) {
            throw new SQLException("Il compra subito è disponibile solo per annunci di vendita");
        }

        annuncio.setStato("venduto");
        annuncioDAO.aggiornaAnnuncio(annuncio);

        List<Oggetto> oggettiAnnuncio = oggettoDAO.getOggettiByAnnuncio(codiceAnnuncio);
        String compratore = getUtenteCorrente().getMatricola();

        for (Oggetto obj : oggettiAnnuncio) {
            oggettoDAO.aggiornaOggettoMatricola(obj.getCodiceOggetto(), compratore);
            oggettoDAO.rimuoviAssociazioneAnnuncio(obj.getCodiceOggetto());
        }

        return true;
    }

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

    public List<Oggetto> getOggettiUtenteObj(String matricola) throws SQLException {
        return oggettoDAO.getOggettiByMatricola(matricola);
    }

    public Oggetto getOggettoByCodice(String codiceOggetto) throws SQLException {
        return oggettoDAO.getOggettoByCodice(codiceOggetto);
    }

    public List<Oggetto> getOggettiByAnnuncio(String codiceAnnuncio) throws SQLException {
        return oggettoDAO.getOggettiByAnnuncio(codiceAnnuncio);
    }

    public boolean creaOggetto(Oggetto oggetto) throws SQLException {
        String matricola = getUtenteCorrente().getMatricola();
        return oggettoDAO.creaOggetto(oggetto, matricola);
    }

    public boolean modificaOggetto(Oggetto oggetto) throws SQLException {
        return oggettoDAO.aggiornaOggetto(oggetto);
    }

    public boolean eliminaOggetto(String codiceOggetto) throws SQLException {
        return oggettoDAO.eliminaOggetto(codiceOggetto);
    }

    public void aggiornaCodiceAnnuncioOggetto(String codiceOggetto, String codiceAnnuncio) throws SQLException {
        oggettoDAO.aggiornaCodiceAnnuncioOggetto(codiceOggetto, codiceAnnuncio);
    }

    public boolean rimuoviAssociazioneAnnuncioOggetto(String codiceOggetto) throws SQLException {
        return oggettoDAO.rimuoviAssociazioneAnnuncio(codiceOggetto);
    }

    public int getTotaleOfferte() {
        try {
            return offertaDAO.getTotaleOfferte();
        } catch (SQLException e) {
            showError("Errore getTotaleOfferte: " + e.getMessage());
            return 0;
        }
    }

    public int getTotaleOffertePerTipologia(String tipologia) {
        try {
            return offertaDAO.getTotaleOffertePerTipologia(tipologia);
        } catch (SQLException e) {
            showError("Errore getTotaleOffertePerTipologia: " + e.getMessage());
            return 0;
        }
    }

    public int getOfferteAccettatePerTipologia(String tipologia) {
        try {
            return offertaDAO.getOfferteAccettatePerTipologia(tipologia);
        } catch (SQLException e) {
            showError("Errore getOfferteAccettatePerTipologia: " + e.getMessage());
            return 0;
        }
    }

    public double[] getStatisticheVenditeAccettate() {
        try {
            return offertaDAO.getStatisticheVenditeAccettate();
        } catch (SQLException e) {
            showError("Errore getStatisticheVenditeAccettate: " + e.getMessage());
            return new double[0];
        }
    }

    	

    public List<String> getNotificheUtente(String matricola) {
        List<String> notif = new ArrayList<>();
        try {
            List<Annuncio> mieiAnnunci = getAnnunciByUtente(matricola);
            int offerteRicevute = 0;

            for (Annuncio a : mieiAnnunci) {
                if ("attivo".equalsIgnoreCase(a.getStato())) {
                    List<Offerta> offerteSuAnnuncio = offertaDAO.getOfferteByCodiceAnnuncio(a.getCodiceAnnuncio());
                    for (Offerta o : offerteSuAnnuncio) {
                        if ("inviata".equalsIgnoreCase(o.getStato())) {
                            offerteRicevute++;
                        }
                    }
                }
            }
            if (offerteRicevute == 1)
                notif.add("Hai " + offerteRicevute + " offerta ricevuta da valutare.");
            if (offerteRicevute > 1)
                notif.add("Hai " + offerteRicevute + " offerta ricevuta da valutare.");

            int annunciScaduti = 0;
            for (Annuncio a : mieiAnnunci) {
                if ("scaduto".equalsIgnoreCase(a.getStato())) {
                    annunciScaduti++;
                }
            }
            if (annunciScaduti == 1)
                notif.add("Hai " + annunciScaduti + " annuncio scaduto.");
            if (annunciScaduti > 1)
                notif.add("Hai " + annunciScaduti + " annuncio scaduto.");
            
        } catch (Exception e) {
            notif.add("Errore nel caricamento notifiche.");
        }
        return notif;
    }

    public void associaOggettoAdOfferta(String codiceOfferta, String codiceOggetto) throws SQLException {
        offreDAO.creaOffre(new Offre(codiceOfferta, codiceOggetto));
    }

    public String getUltimaOffertaScambioUtente() throws SQLException {
        List<Offerta> offerte = offertaDAO.getOfferteByUtente(getUtenteCorrente().getMatricola());
        for (int i = offerte.size() - 1; i >= 0; i--) {
            if ("scambio".equalsIgnoreCase(offerte.get(i).getTipo())) {
                return offerte.get(i).getCodiceOfferta();
            }
        }
        return null;
    }

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
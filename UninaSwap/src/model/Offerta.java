package model;

import java.sql.Date;

public class Offerta {
    private String codiceOfferta;
    private String codiceAnnuncio;
    private String matricola;
    private String tipo;
    private Double prezzoOfferto;
    private String stato;
    private Date data;
    private String messaggio;
    private String descrizioneAnnuncio;

    // Costruttore completo
    public Offerta(String codiceOfferta, String codiceAnnuncio, String matricola, String tipo,
                   Double prezzoOfferto, String stato, Date data) {
        this.codiceOfferta = codiceOfferta;
        this.codiceAnnuncio = codiceAnnuncio;
        this.matricola = matricola;
        this.tipo = tipo;
        this.prezzoOfferto = prezzoOfferto;
        this.stato = stato;
        this.data = data;
    }

    // Costruttore vuoto
    public Offerta() {}

    // Getters e Setters
    public String getCodiceOfferta() {
        return codiceOfferta;
    }
    public void setCodiceOfferta(String codiceOfferta) {
        this.codiceOfferta = codiceOfferta;
    }

    public String getCodiceAnnuncio() {
        return codiceAnnuncio;
    }
    public void setCodiceAnnuncio(String codiceAnnuncio) {
        this.codiceAnnuncio = codiceAnnuncio;
    }

    public String getMatricola() {
        return matricola;
    }
    public void setMatricola(String matricola) {
        this.matricola = matricola;
    }

    public String getTipo() {
        return tipo;
    }
    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public Double getPrezzoOfferto() {
        return prezzoOfferto;
    }
    public void setPrezzoOfferto(Double prezzoOfferto) {
        this.prezzoOfferto = prezzoOfferto;
    }

    public String getStato() {
        return stato;
    }
    public void setStato(String stato) {
        this.stato = stato;
    }

    public Date getData() {
        return data;
    }
    public void setdata(Date data) {
        this.data = data;
    }

    public String getMessaggio() {
        return messaggio;
    }
    public void setMessaggio(String messaggio) {
        this.messaggio = messaggio;
    }

    public String getDescrizioneAnnuncio() {
        return descrizioneAnnuncio;
    }
    public void setDescrizioneAnnuncio(String descrizioneAnnuncio) {
        this.descrizioneAnnuncio = descrizioneAnnuncio;
    }

    @Override
    public String toString() {
        return "Offerta{" +
                "codiceOfferta='" + codiceOfferta + '\'' +
                ", codiceAnnuncio='" + codiceAnnuncio + '\'' +
                ", matricola='" + matricola + '\'' +
                ", tipo='" + tipo + '\'' +
                ", prezzoOfferto=" + prezzoOfferto +
                ", stato='" + stato + '\'' +
                ", data=" + data +
                ", messaggio='" + messaggio + '\'' +
                ", descrizioneAnnuncio='" + descrizioneAnnuncio + '\'' +
                '}';
    }
}

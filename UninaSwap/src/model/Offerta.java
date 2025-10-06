package model;

import java.sql.Date;

public class Offerta {
    private String codiceOfferta;
    private String codiceAnnuncio;
    private String matricola;
    private String tipo;
    private Double prezzoOfferto;
    private String stato;
    private Date dataOfferta;

    // Costruttore completo
    public Offerta(String codiceOfferta, String codiceAnnuncio, String matricola, String tipo, 
                   Double prezzoOfferto, String stato, Date dataOfferta) {
        this.codiceOfferta = codiceOfferta;
        this.codiceAnnuncio = codiceAnnuncio;
        this.matricola = matricola;
        this.tipo = tipo;
        this.prezzoOfferto = prezzoOfferto;
        this.stato = stato;
        this.dataOfferta = dataOfferta;
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

    public Date getDataOfferta() {
        return dataOfferta;
    }

    public void setDataOfferta(Date dataOfferta) {
        this.dataOfferta = dataOfferta;
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
                ", dataOfferta=" + dataOfferta +
                '}';
    }
}

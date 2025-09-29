package model;

import java.util.Date;

public class Offerta {
    private String codiceOfferta;
    private Date dataOfferta;
    private String stato; // inviata, accettata, rifiutata, annullata
    private Double prezzoOfferto;
    private String tipo; // vendita, scambio, regalo
    private String matricola; // riferimento Utente
    private String codiceAnnuncio; // riferimento Annuncio

    public Offerta(String codiceOfferta, Date dataOfferta, String stato, Double prezzoOfferto, String tipo,
                   String matricola, String codiceAnnuncio) {
        this.codiceOfferta = codiceOfferta;
        this.dataOfferta = dataOfferta;
        this.stato = stato;
        this.prezzoOfferto = prezzoOfferto;
        this.tipo = tipo;
        this.matricola = matricola;
        this.codiceAnnuncio = codiceAnnuncio;
    }

	public String getCodiceOfferta() {
		return codiceOfferta;
	}

	public void setCodiceOfferta(String codiceOfferta) {
		this.codiceOfferta = codiceOfferta;
	}

	public Date getDataOfferta() {
		return dataOfferta;
	}

	public void setDataOfferta(Date dataOfferta) {
		this.dataOfferta = dataOfferta;
	}

	public String getStato() {
		return stato;
	}

	public void setStato(String stato) {
		this.stato = stato;
	}

	public Double getPrezzoOfferto() {
		return prezzoOfferto;
	}

	public void setPrezzoOfferto(Double prezzoOfferto) {
		this.prezzoOfferto = prezzoOfferto;
	}

	public String getTipo() {
		return tipo;
	}

	public void setTipo(String tipo) {
		this.tipo = tipo;
	}

	public String getMatricola() {
		return matricola;
	}

	public void setMatricola(String matricola) {
		this.matricola = matricola;
	}

	public String getCodiceAnnuncio() {
		return codiceAnnuncio;
	}

	public void setCodiceAnnuncio(String codiceAnnuncio) {
		this.codiceAnnuncio = codiceAnnuncio;
	}


}
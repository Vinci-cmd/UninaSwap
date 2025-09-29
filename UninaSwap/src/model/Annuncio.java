package model;

import java.sql.Date;
public class Annuncio {
    private String codiceAnnuncio;
    private String descrizione;
    private String categoria;
    private String tipologia; // vendita, scambio, regalo
    private Double prezzo;
    private String stato; // attivo, venduto, scambiato, regalato, scaduto
    private Date dataPubblicazione;
    private String matricola; // riferimento a Utente

    public Annuncio(String codiceAnnuncio, String descrizione, String categoria, String tipologia,
                    Double prezzo, String stato, Date dataPubblicazione, String matricola) {
        this.codiceAnnuncio = codiceAnnuncio;
        this.descrizione = descrizione;
        this.categoria = categoria;
        this.tipologia = tipologia;
        this.prezzo = prezzo;
        this.stato = stato;
        this.dataPubblicazione = dataPubblicazione;
        this.matricola = matricola;
    }

	public String getCodiceAnnuncio() {
		return codiceAnnuncio;
	}

	public void setCodiceAnnuncio(String codiceAnnuncio) {
		this.codiceAnnuncio = codiceAnnuncio;
	}

	public String getDescrizione() {
		return descrizione;
	}

	public void setDescrizione(String descrizione) {
		this.descrizione = descrizione;
	}

	public String getCategoria() {
		return categoria;
	}

	public void setCategoria(String categoria) {
		this.categoria = categoria;
	}

	public String getTipologia() {
		return tipologia;
	}

	public void setTipologia(String tipologia) {
		this.tipologia = tipologia;
	}

	public Double getPrezzo() {
		return prezzo;
	}

	public void setPrezzo(Double prezzo) {
		this.prezzo = prezzo;
	}

	public String getStato() {
		return stato;
	}

	public void setStato(String stato) {
		this.stato = stato;
	}

	public Date getDataPubblicazione() {
		return dataPubblicazione;
	}

	public void setDataPubblicazione(Date dataPubblicazione) {
		this.dataPubblicazione = dataPubblicazione;
	}

	public String getMatricola() {
		return matricola;
	}

	public void setMatricola(String matricola) {
		this.matricola = matricola;
	}
    
}
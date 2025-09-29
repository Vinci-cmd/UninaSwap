package model;

public class TipoConsegna {
    private String codiceConsegna;
    private String sede;
    private String descrizione;
    private String fasciaOraria; // "8" o "20"
    private String codiceAnnuncio; // riferimento all'annuncio

    public TipoConsegna(String codiceConsegna, String sede, String descrizione, String fasciaOraria, String codiceAnnuncio) {
        this.codiceConsegna = codiceConsegna;
        this.sede = sede;
        this.descrizione = descrizione;
        this.fasciaOraria = fasciaOraria;
        this.codiceAnnuncio = codiceAnnuncio;
    }

	public String getCodiceConsegna() {
		return codiceConsegna;
	}

	public void setCodiceConsegna(String codiceConsegna) {
		this.codiceConsegna = codiceConsegna;
	}

	public String getSede() {
		return sede;
	}

	public void setSede(String sede) {
		this.sede = sede;
	}

	public String getDescrizione() {
		return descrizione;
	}

	public void setDescrizione(String descrizione) {
		this.descrizione = descrizione;
	}

	public String getFasciaOraria() {
		return fasciaOraria;
	}

	public void setFasciaOraria(String fasciaOraria) {
		this.fasciaOraria = fasciaOraria;
	}

	public String getCodiceAnnuncio() {
		return codiceAnnuncio;
	}

	public void setCodiceAnnuncio(String codiceAnnuncio) {
		this.codiceAnnuncio = codiceAnnuncio;
	}

}
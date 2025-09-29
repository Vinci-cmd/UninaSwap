package model;

public class Offre {
    private String codiceOfferta;   // riferimento a Offerta
    private String codiceOggetto;   // riferimento a Oggetto

    public Offre(String codiceOfferta, String codiceOggetto) {
        this.codiceOfferta = codiceOfferta;
        this.codiceOggetto = codiceOggetto;
    }

	public String getCodiceOfferta() {
		return codiceOfferta;
	}

	public void setCodiceOfferta(String codiceOfferta) {
		this.codiceOfferta = codiceOfferta;
	}

	public String getCodiceOggetto() {
		return codiceOggetto;
	}

	public void setCodiceOggetto(String codiceOggetto) {
		this.codiceOggetto = codiceOggetto;
	}

 
}
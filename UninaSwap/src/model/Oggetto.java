package model;

public class Oggetto {
    private String codiceOggetto;
    private String nome;
    private String descrizione;
    private String categoria;
    private String codiceAnnuncio; // riferimento all'annuncio a cui appartiene

    public Oggetto(String codiceOggetto, String nome, String descrizione, String categoria, String codiceAnnuncio) {
        this.codiceOggetto = codiceOggetto;
        this.nome = nome;
        this.descrizione = descrizione;
        this.categoria = categoria;
        this.codiceAnnuncio = codiceAnnuncio;
    }

	public String getCodiceOggetto() {
		return codiceOggetto;
	}

	public void setCodiceOggetto(String codiceOggetto) {
		this.codiceOggetto = codiceOggetto;
	}

	public String getNome() {
		return nome;
	}

	public void setNome(String nome) {
		this.nome = nome;
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

	public String getCodiceAnnuncio() {
		return codiceAnnuncio;
	}

	public void setCodiceAnnuncio(String codiceAnnuncio) {
		this.codiceAnnuncio = codiceAnnuncio;
	}


}
package model;

public class Utente {
    private String matricola;
    private String nome;
    private String cognome;
    private String email;
    private String password;
    private String universita;

    public Utente(String matricola, String nome, String cognome, String email, String password, String universita) {
        this.matricola = matricola;
        this.nome = nome;
        this.cognome = cognome;
        this.email = email;
        this.password = password;
        this.universita = universita;
    }

	public String getMatricola() {
		return matricola;
	}

	public void setMatricola(String matricola) {
		this.matricola = matricola;
	}

	public String getNome() {
		return nome;
	}

	public void setNome(String nome) {
		this.nome = nome;
	}

	public String getCognome() {
		return cognome;
	}

	public void setCognome(String cognome) {
		this.cognome = cognome;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getUniversita() {
		return universita;
	}

	public void setUniversita(String universita) {
		this.universita = universita;
	}


}
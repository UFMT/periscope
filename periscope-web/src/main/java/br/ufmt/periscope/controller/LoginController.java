package br.ufmt.periscope.controller;

import javax.enterprise.inject.Model;

/**
 * - @Model<BR/>
 * Modelo controller de login
 */
@Model
public class LoginController {
	
	private String login;
	private String password;
	
    /**
     *
     * @return
     */
    public String getLogin() {
		return login;
	}

    /**
     *
     * @param login
     */
    public void setLogin(String login) {
		this.login = login;
	}

    /**
     *
     * @return
     */
    public String getPassword() {
		return password;
	}

    /**
     *
     * @param password
     */
    public void setPassword(String password) {
		this.password = password;
	}
	
	
	
}

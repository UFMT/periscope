package br.ufmt.periscopeEsb.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

@Entity
public class Service {
	
	@Id
	@GeneratedValue(strategy=GenerationType.IDENTITY)
	private Long id;
	
	@Column
	private String name;
	
	@Column
	private String realPath;
	
	@Column
	private String token;
	
	
	
	

	public Service(){
		
	}
	
	public String getToken() {
		return token;
	}



	public void setToken(String token) {
		this.token = token;
	}



	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getRealPath() {
		return realPath;
	}

	public void setRealPath(String realPath) {
		this.realPath = realPath;
	}

	public Long getId() {
		return id;
	}
	
	
	

}

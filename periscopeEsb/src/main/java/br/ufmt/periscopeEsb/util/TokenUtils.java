package br.ufmt.periscopeEsb.util;

import java.io.Serializable;
import java.math.BigInteger;
import java.security.SecureRandom;

import javax.ejb.EJB;
import javax.ejb.Stateless;

import br.ufmt.periscopeEsb.dao.ServiceDao;

@Stateless
public class TokenUtils implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -2223795002988725532L;
	

	@EJB
	private ServiceDao dao;
	
	private SecureRandom secure;
	
	
	public TokenUtils(){
		
	}
	
	
	
	public String generateToken(){
		if(secure == null){
			secure = new SecureRandom();
		}
		String str =  new BigInteger(500, secure).toString(32);
		while(dao.findByToken(str) != null){
			str =  new BigInteger(500, secure).toString(32);
		}
		return str;
		
	}




	
	
}

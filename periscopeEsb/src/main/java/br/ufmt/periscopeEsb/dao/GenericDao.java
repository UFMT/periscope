package br.ufmt.periscopeEsb.dao;

import java.io.Serializable;
import java.util.List;

/**
 * Interface a ser implementada pelas entidade que queirar prover acesso ao banco de dados
 *
 * @param		<T>	a entidade que vai ser manejada
 * @param		<ID> a classe utilizada pela id da entidade
 * @author      Alvaro Viebrantz
 * @version     1.0
 * @since       03/05/2012
 */
public interface GenericDao<T,ID extends Serializable>{
	
	
	/**
	* Retorna a entitidade que possui como id o parâmetro passado, senão retorna null
	* 
	* @param id identificador da entidade
	* @return a entidade se ela existe ou null senão existir
	* @author Alvaro Viebrantz
	*/
	T findById(ID id);
	
	/**
	* Retorna todas as entidades persistidas que sejam da classe manejada
	* 
	* @return uma lista de entidades
	* @author Alvaro Viebrantz
	*/
	List<T> findAll();
	
	/**
	* Persiste a entidade passada ou se ela existe a atualiza
	* 
	* @param a entidade que vai ser persistidade
	* @author Alvaro Viebrantz
	*/
	void save(T entity);
	
	/**
	* Atualiza a entidade passada ou se ela não existe a persiste
	* 
	* @param a entidade que vai ser atualizada
	* @author Alvaro Viebrantz
	*/
	void update(T entity);
	
	/**
	* Deleta a entidade passada se ela existir
	* 
	* @param a entidade que vai ser deletada
	* @author Alvaro Viebrantz
	*/
	void delete(T entity);
	
	
	
	
}

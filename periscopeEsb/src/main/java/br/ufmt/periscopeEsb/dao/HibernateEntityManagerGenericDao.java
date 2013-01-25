package br.ufmt.periscopeEsb.dao;

import java.io.Serializable;
import java.util.List;

import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceUnit;
import javax.persistence.Query;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import javax.persistence.metamodel.EntityType;

import org.hibernate.Session;
import org.hibernate.SessionFactory;

public abstract class HibernateEntityManagerGenericDao<T,ID extends Serializable> implements GenericDao<T, ID> {

	@Inject
	private EntityManager em;
	
	@PersistenceContext
	private Session session;
	
	@PersistenceUnit
	private SessionFactory sf;
			
	private Class<T> persistenceClass;// = (Class<T>)((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[0];					
	
	public HibernateEntityManagerGenericDao(Class<T> entity){				
	    persistenceClass = entity;	    
	}
	
	@Override
	public T findById(ID id) {		
		T entity = getEntityManager().find(persistenceClass,id);		
		return entity;
	}

	@Override
	public List<T> findAll() {
		
		CriteriaQuery<T> criteria = getEntityManager().getCriteriaBuilder().createQuery(persistenceClass);
		Root<T> entityRoot = criteria.from(persistenceClass);
		criteria.select(entityRoot);
		
		List<T> entities = getEntityManager().createQuery(criteria).getResultList();
		
		return entities;
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public void save(T entity) {	
		
		getEntityManager().persist(entity);		
		
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public void update(T entity) {
		getEntityManager().merge(entity);		
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public void delete(T entity) {
		getEntityManager().remove(entity);
	}
	
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public void delete(ID id){
		EntityType<T> entity = em.getMetamodel().entity(persistenceClass);	
		
		String classIDName = entity.getId(id.getClass()).getName();		
		Query q = em.createQuery("DELETE FROM  "+persistenceClass.getName()+" WHERE "+classIDName+" = :idValue");
		q.setParameter("idValue", id);
		q.executeUpdate();
		
	}

	public EntityManager getEntityManager() {
		return em;
	}

	public Session getSession(){
		return session;
	}

	public Session getSession(String tenant){
		return sf.withOptions().tenantIdentifier(tenant).openSession();
	}
}

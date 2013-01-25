package br.ufmt.periscopeEsb.dao;

import java.util.List;

import javax.ejb.Stateless;

import org.hibernate.Criteria;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;

import br.ufmt.periscopeEsb.model.Service;

@Stateless
public class ServiceDao extends HibernateEntityManagerGenericDao<Service, Long>{

	public ServiceDao() {
		super(Service.class);
	}
	
	
	@SuppressWarnings("unchecked")
	public List<Service> findLike(String serviceName){
		Criteria crit = getSession().createCriteria(Service.class);
		crit.add(Restrictions.ilike("name", serviceName, MatchMode.ANYWHERE));
		crit.addOrder(Order.asc("name"));
		return crit.list();
	}
	
	@SuppressWarnings("unchecked")
	public Service findByName(String serviceName){
		Criteria crit = getSession().createCriteria(Service.class);
		crit.add(Restrictions.eq("name",serviceName));
		List<Service> services = crit.list(); 
		Service service = null;
		if(services.size() > 0)
			service = services.get(0);
		return service;
	}
	
	@SuppressWarnings("unchecked")
	public Service findByToken(String token){
		Criteria crit = getSession().createCriteria(Service.class);
		crit.add(Restrictions.eq("token",token));
		List<Service> services = crit.list(); 
		Service service = null;
		if(services.size() > 0)
			service = services.get(0);
		return service;
	}
	
	@SuppressWarnings("unchecked")
	public List<Service> findAllOrdered(){
		Criteria crit = getSession().createCriteria(Service.class);
		crit.addOrder(Order.asc("name"));
		return crit.list();
	}

}

package br.ufmt.periscope.repository;

import javax.inject.Inject;
import javax.inject.Named;

import br.ufmt.periscope.model.Country;

import com.github.jmkgreen.morphia.Datastore;

@Named
public class CountryRepository {

	@Inject
	private Datastore ds;
		
	public Country getCountryByAcronym(String acronym){
		return ds.createQuery(Country.class).field("acronym").equal(acronym).get();		
	}
}

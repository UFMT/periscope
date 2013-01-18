package br.ufmt.periscope.bean;

import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.inject.Inject;

import br.ufmt.periscope.model.Country;
import br.ufmt.periscope.model.User;

import com.bigfatgun.fixjures.Fixjure;
import com.bigfatgun.fixjures.yaml.YamlSource;
import com.github.jmkgreen.morphia.Datastore;

@Singleton
@Startup
public class SeedBean {

	private @Inject Datastore ds;
	private @Inject Logger log;

	@PostConstruct
	public void atStartup() {
		log.info("Inicializando seeder");
		initUsers();
		initCountries();		
	}

	private void initCountries() {
		if(ds.getCount(Country.class) == 0l){
			log.info("Nenhum país encontrado.");
			List<Country> countries =Fixjure.listOf(Country.class)
					        		.from(YamlSource.newYamlResource("country-inicial-data.yaml"))
					        		.create();
			Iterator<Country> it = countries.iterator();
			while (it.hasNext()) {
				ds.save(it.next());
			}
			log.info("Cadastrado "+ countries.size() + " países.");
		}
	}

	private void initUsers() {
		if (ds.getCount(User.class) == 0l) {
			log.info("Nenhum usuário encontrado.");
			List<User> users =Fixjure.listOf(User.class)
						        		.from(YamlSource.newYamlResource("user-inicial.yaml"))
						        		.create();
			Iterator<User> it = users.iterator();
			while (it.hasNext()) {
				ds.save(it.next());
			}
			log.info("Cadastrado "+ users.size() + " usuários.");
		}
	}
}

package br.ufmt.periscope.importer;

import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class PatentImporterFactory {
		
	private Map<String,Class<? extends PatentImporter>> implementations = new HashMap<String, Class<? extends PatentImporter>>();
	private String[] ORIGINS;
	private @Inject @Any Instance<PatentImporter> patentImporterInstance;
	private @Inject Logger log;
	
	@PostConstruct
	public void init(){		
		Iterator<PatentImporter> iterator = patentImporterInstance.iterator();
		while(iterator.hasNext()){
			PatentImporter pi = iterator.next();
			//Tem decorator ?
			if(pi.getClass().getSuperclass() == Object.class){
				register(pi.provider(),pi.getClass());
			}else{
				register(pi.provider(),(Class<? extends PatentImporter>) pi.getClass().getGenericSuperclass());
			}			
		}
		ORIGINS = implementations.keySet().toArray(new String[0]);
	}	
	
	public PatentImporter getImporter(String origin){
		if(implementations.containsKey(origin)){
			return patentImporterInstance.select(implementations.get(origin), new Annotation[]{}).get();			
		}
		throw new NoClassDefFoundError("Ainda não foi implementado importador para arquivos provenientes do "+origin);		
	}
			
	private void register(String origin,Class<? extends PatentImporter> klass){
		implementations.put(origin, klass);		
	}

	public String[] getORIGINS() {
		return ORIGINS;
	}
}

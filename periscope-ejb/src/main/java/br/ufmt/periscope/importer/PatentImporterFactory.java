package br.ufmt.periscope.importer;

import java.util.HashMap;
import java.util.Map;

public class PatentImporterFactory {
		
	private static Map<String,Class<? extends PatentImporter>> implementations = new HashMap<String, Class<? extends PatentImporter>>();
	public static String[] ORIGINS;
	
	static{
				
		register("DPMA", DPMAPatentImporter.class);	
		ORIGINS = implementations.keySet().toArray(new String[0]);
	}
	
	public static PatentImporter getImporter(String origin){
		if(implementations.containsKey(origin)){
			try {
				return implementations.get(origin).newInstance();
			} catch (InstantiationException e) {			
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			}
		}		
		throw new NoClassDefFoundError("Ainda não foi implementado importador para arquivos provenientes do "+origin);		
	}
	
	
	
	private static void register(String origin,Class<? extends PatentImporter> klass){
		implementations.put(origin, klass);		
	}

}

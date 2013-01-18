package br.ufmt.periscope.importer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.ParseException;
import java.text.SimpleDateFormat;

import br.ufmt.periscope.model.Patent;

public class DPMAPatentImporter implements PatentImporter{

	private BufferedReader bur = null;
	private String line = null;
	private Patent patent = null;
	
	private SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy");
	private SimpleDateFormat sdf2 = new SimpleDateFormat("yyyyMMdd");
	
	private String vet[] = null;
	private String[] array = null;
	
	
	@Override
	public void initWithStream(InputStream is) {
		InputStreamReader isr = new InputStreamReader(is);
		bur =  new BufferedReader(isr);
		//Passa o cabeçalho
		nextLine();		
		//Inicia na primeira linha dos dados
		nextLine();
		patent = new Patent();		
		
	}

	private void nextLine() {
		try {
			line = bur.readLine();
		} catch (IOException e) {
			line = null;
			e.printStackTrace();
		}		
	}

	@Override
	public boolean hasNext() {	
		if(line == null) return false;
		if(line.trim().length() <= 0) return false;
		return true;
	}

	@Override
	public Patent next() {
		patent = new Patent();
		vet = line.split("\t", -2);

		//patent.setLanguage(lang);
		patent.setTitleSelect(vet[7].substring(5));
		try {
			patent.setPublicationNumber(vet[0] + " " + 	sdf2.format(sdf.parse(vet[3])));
		} catch (ParseException e) {
			e.printStackTrace();
		}
		nextLine();
		return patent;

	}

	@Override
	public void remove() {
		
	}


}

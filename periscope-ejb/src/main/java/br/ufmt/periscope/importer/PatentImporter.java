package br.ufmt.periscope.importer;

import java.io.InputStream;
import java.util.Iterator;
import java.util.List;

import br.ufmt.periscope.model.Patent;

public interface PatentImporter extends Iterator<Patent>{
	
	public void initWithStream(InputStream is);

}

package br.ufmt.periscope.importer;

import java.io.InputStream;

import br.ufmt.periscope.model.Patent;

public interface PatentImporter {
	
	public boolean initWithStream(InputStream is);
	public String provider();
        public boolean hasNext();
        public Patent next();
        public void remove();

}

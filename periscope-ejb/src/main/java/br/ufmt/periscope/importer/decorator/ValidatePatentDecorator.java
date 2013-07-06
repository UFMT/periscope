package br.ufmt.periscope.importer.decorator;

import javax.decorator.Decorator;
import javax.decorator.Delegate;
import javax.enterprise.inject.Any;
import javax.inject.Inject;

import br.ufmt.periscope.importer.PatentImporter;
import br.ufmt.periscope.model.Patent;

@Decorator
public abstract class ValidatePatentDecorator implements PatentImporter {

	@Inject
	@Delegate
	@Any
	private PatentImporter patentImporter;
	
	@Inject
	private PatentValidator validator;


	@Override
	public Patent next() {
		Patent patent = patentImporter.next();
		if(patent == null) return null;
		validator.validate(patent);
		return patent;
	}

	
}

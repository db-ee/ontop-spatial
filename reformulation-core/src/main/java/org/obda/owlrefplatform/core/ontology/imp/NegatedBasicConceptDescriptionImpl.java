package org.obda.owlrefplatform.core.ontology.imp;

import inf.unibz.it.obda.model.Predicate;

import org.obda.owlrefplatform.core.ontology.BasicConceptDescription;
import org.obda.owlrefplatform.core.ontology.GeneralConceptDescription;

public class NegatedBasicConceptDescriptionImpl implements GeneralConceptDescription{

	private BasicConceptDescription basicConcept = null;

	protected NegatedBasicConceptDescriptionImpl(BasicConceptDescription basicConceptDescriptionImpl){
		this.basicConcept = basicConceptDescriptionImpl;
	}

	public BasicConceptDescription getBasicConceptDescriptionImpl(){
		return basicConcept;
	}

	public Predicate getPredicate() {
		return basicConcept.getPredicate();
	}

	public boolean isInverse() {

		return basicConcept.isInverse();
	}
}

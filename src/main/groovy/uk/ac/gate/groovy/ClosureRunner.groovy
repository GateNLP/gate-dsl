package uk.ac.gate.groovy;

import gate.Annotation;
import gate.Gate;
import gate.Factory;
import gate.Document;

import gate.creole.AbstractLanguageAnalyser
import gate.creole.ExecutionException;
import gate.creole.metadata.CreoleResource;
import gate.creole.metadata.Optional;
import gate.creole.metadata.CreoleParameter;

import gate.creole.ResourceData;

import gate.creole.metadata.RunTime;
import groovy.transform.TypeChecked;
import gate.creole.SerialAnalyserController

class AddClosureToPR {
    public void add(Closure<gate.Document> task) {
        def initFeatures = Factory.newFeatureMap()
        initFeatures.targetClosure = task
        this.add(Factory.createResource("uk.ac.gate.groovy.ClosureRunner", initFeatures))
    }
}

@CreoleResource(name="ClosureRunner", 
	comment="Runs a Groovy closure")
class ClosureRunner extends AbstractLanguageAnalyser {
	@CreoleParameter(comment="Closure to use")
	Closure<Document> targetClosure = null
	
	public void execute() throws ExecutionException {
		targetClosure(document)
	}

	static register() {

		def resourceData = new gate.creole.ResourceData()

		resourceData.name = "Closure Runner"
		resourceData.resourceClass = ClosureRunner
		resourceData.className = ClosureRunner.getName()
		resourceData.features = Factory.newFeatureMap()
		Gate.getCreoleRegister().put(resourceData.className, resourceData)

		SerialAnalyserController.mixin(AddClosureToPR)
	}

}


package uk.ac.gate.groovy
import gate.Utils
import gate.Annotation
import gate.Gate
import gate.Factory
import gate.Document
import gate.creole.SerialAnalyserController


class PRBuilder {
	static pr (@DelegatesTo(strategy=Closure.DELEGATE_ONLY, value=PRSpec) Closure cl) {
		def pr = new PRSpec()
		def code = cl.rehydrate(pr, cl.owner, cl.thisObject)
	    code.resolveStrategy = Closure.DELEGATE_ONLY
	    def result = code.delegate
	    code()
	    use(gate.Utils) {
		   	return Factory.createResource(result._cls, 
		   								  (result._init + result._process),
		    							  result._features, 
		    							  result._name)
		}
	}
}
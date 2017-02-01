package uk.ac.gate.groovy
import gate.Factory
import gate.creole.ResourceInstantiationException
import gate.Gate

class PRSpec {
	def _cls 
	def _name = ""

	// These can't be constructed without a context
	def _runtime
	def _features 
	def _init 

	def _context

	def instance = null // This is super nasty. Should really create a new object


	def PRSpec(PipelineContext context) {
		_context = context

		if (context == null) {
			throw new NullPointerException("PRSpec created with null context")
		}
		_features = new FeatureMapSpec(context)
		_init = new FeatureMapSpec(context)	
		_runtime = new FeatureMapSpec(context)	
	}

    void cls(String cls) { _cls = cls }
    void name(String name) { _name = name }

    void init(Map init) { _init = new FeatureMapSpec(_context, init) }
    void features(Map features) { _features = new FeatureMapSpec(_context, features) }
    void runtime(Map runtime) { _runtime = new FeatureMapSpec(_context, runtime) }
	
	def runtime(Closure cl) {
			cl.resolveStrategy = Closure.DELEGATE_FIRST
	    	cl.delegate = _runtime
	    	cl()
		}

	def init(Closure cl) {
			cl.resolveStrategy = Closure.DELEGATE_FIRST

	    	cl.delegate = _init
	    	cl()
		}

	def features(Closure cl) {
			cl.resolveStrategy = Closure.DELEGATE_FIRST

	    	cl.delegate = _features
	    	cl()
		}

	def setRuntime(Closure cl) {
			cl.resolveStrategy = Closure.DELEGATE_FIRST
	    	cl.delegate = _runtime
	    	cl()
		}

	def setInit(Closure cl) {
			cl.resolveStrategy = Closure.DELEGATE_FIRST

	    	cl.delegate = _init
	    	cl()
		}

	def setFeatures(Closure cl) {
			cl.resolveStrategy = Closure.DELEGATE_FIRST

	    	cl.delegate = _features
	    	cl()
		}


    String toString() {
    	return "${_cls}: ${_init} ${_runtime} ${_features}"
    }

    def toDSL(tablevel = 0) {
		// Represents this spec in DSL format
		def prepareValue = { value ->
			if (value instanceof URL) {	
				return new File("").toURI().relativize(value.toURI()).getPath().inspect()
			} else {
				return value.inspect()
			}
		}

		def result = []
		if (_name || _runtime || _init || _features) {
			result << "pr \"${_cls}\", {"
			
			if (_name) {
				result << "\tname \"${_name}\""
			}		

			if (_runtime) {
				result << "\t runtime {"
				_runtime.each { key, value -> 
					result << "\t\t${key}= ${prepareValue(value)}"
				}
				result << "\t }"
			}	

			if (_init) {
				result << "\t init {"
				_init.each { key, value -> 
					result << "\t\t${key}= ${prepareValue(value)}"
				}
				result << "\t }"
			}	

			if (_features) {
				result << "\t features {"
				_features.each { key, value -> 
					result << "\t\t${key}= ${prepareValue(value)}"
				}
				result << "\t }"
			}	

			result << "}"
		} else {
			result << "pr \"${_cls}\""
		}


		result = result.collect { line -> "\t" * tablevel + line} join "\n"

		return result 
	}

	def build() {
		try {
			def initRuntime = (this._init + this._runtime)
			def initRuntimeFeatureMap = Factory.newFeatureMap()
			if (!initRuntime.isEmpty()) {
				initRuntimeFeatureMap = initRuntime.toDeepFeatureMap()
			}
		   	instance = Factory.createResource(this._cls, 
	   		  initRuntimeFeatureMap,
			  this._features.toDeepFeatureMap(), 
			  this._name)

			return instance 			
		} catch (ResourceInstantiationException e) {
			if (e.getMessage().contains("Couldn't find parameter named")) {
				def resource_data = Gate.getCreoleRegister().get(this._cls)

				def message = e.message << ":\n"

				message << "Allowed init parameters: ${resource_data.parameterList.initimeParameters.flatten()}\n"
				message << "Allowed runtime parameters: ${resource_data.parameterList.runtimeParameters.flatten()}"

				throw new ResourceInstantiationException(message.toString(), e)
			} else {
				throw new ResourceInstantiationException("Couldn't initialise processing resource ${this}", e)
			}
		}
	}

}

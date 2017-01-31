package uk.ac.gate.groovy
import gate.*
import gate.creole.SerialAnalyserController
import java.io.File
import java.util.Base64
import gate.Resource
import java.net.URISyntaxException
import java.util.List

class PipelineSpec {
	private _cls = "gate.creole.SerialAnalyserController"
	private _name = ""
	private _plugins = []
	private _prs = []
	def prs_index = [:]

	// _features and _init need a context so can't be set right now.
	private _features
	private _init

	private PipelineContext _context

	PipelineSpec(PipelineContext context) {
		_features = new FeatureMapSpec(context)
		_init = new FeatureMapSpec(context)	
		_context = context
	}

 	def rel(String... path) {
		return _context.rel(path)
	}

	def home(String... path) {
		return _context.home(path)
	}

	def getPlugins() {
		return _plugins
	}
	
	def getPrs() {
		return _prs
	}

	/*
	 * Gets the value for a parameter explicitly
	 */
	def param(String key) {
		return _context.params[key]
	}

	/**
	 * Used to explicitly set the value for parameters in the pipeline context.
	 */
	def params(Closure cl) {
		cl.delegate = _context.params
    	cl()
	}

	/**
	 * Used to explicitly set the value for parameters in the pipeline context.
	 */
	def params(Map params) {
		_context.params = new FeatureMapSpec(_context, params)
	}

	/** 
	 * Set parameters if they're not already set
	 */
	def defaultParams(Map params) {
		def newParams = new FeatureMapSpec(_context, params)
		newParams.putAll(_context.params)
		_context.params = newParams
	}

	/** 
	 * Set parameters if they're not already set
	 */
	def defaultParams(Closure cl) {
		def newParams = FeatureMapSpec.fromClosure(_context, cl)
		newParams.putAll(_context.params)
		_context.params = newParams
	}
	
    void cls(String cls) { _cls = cls }

    def plugin(String pluginPath) { 
    	// Convert the path to a relative one and use the URL version of this.
    	plugin(_context.rel(pluginPath)) 
	}

    def plugin(URL pluginUrl) { 
		_plugins << pluginUrl
   	}

    /** 
     * Loads a plugin from the GATE directory
     *
     * @deprecated use plugin home(path) for now, though in future 
     * 	there will be no concept of gate home plugins at all.
     */
    @Deprecated
    def defaultPlugin(String pluginPath) { 
    	plugin(_context.home("plugins", pluginPath))
   	}

    def features(Closure cl) {
    	cl.delegate = _features
    	cl()
	}
	

    def features(Map features) { _features = features }

    def init(Closure cl) {
    	cl.delegate = _init
    	cl()
	}
	
    def init(Map init) { _init = init }

    def name(String name) { _name = name }

	private addPr(pr) {
		if (pr._name) {
	    	prs_index[pr._name] = pr
	    }
	    _prs << pr
	}


    def pr (cls = "", @DelegatesTo(strategy=Closure.DELEGATE_FIRST, value=PRSpec) Closure cl = {}) {
		def pr = new PRSpec(_context)
		def code = cl.rehydrate(pr, cl.owner, cl.thisObject)
	    code.resolveStrategy = Closure.DELEGATE_FIRST
	    def result = code.delegate
	    result._cls = cls
	    code()

	    addPr(pr)

	    pr
	}


	def pipeline(@DelegatesTo(strategy=Closure.DELEGATE_FIRST, value=PipelineSpec) Closure cl) {
		addPr(PipelineBuilder.pipeline(_context, cl))
	}


	/**
	 * Loads another groovy pipeline, with options defined by the closure.
	 * Options will replace the options passed to pipeline initially.
	 */
	def pipeline(URL inputFile, Closure cl) {
		def newContext = new PipelineContext(_context, 
			FeatureMapSpec.fromClosure(_context, cl)) // Features will come from the closure
		addPr(new GroovyShell(PipelineContext.classLoader).parse(inputFile.toURI()).loadSpec(_context))
	}

	def pipeline(URL inputFile) {
		if (inputFile.getPath().endsWith(".groovy")) {
			addPr(new GroovyShell(PipelineContext.classLoader).parse(inputFile.toURI()).loadSpec(_context))
		} else {
			addPr(new PipelineGappSpec(url: inputFile)	)
		}
	}

	def pipeline(PipelineSpec spec) {
		addPr(spec)
	}

	def run(Closure<gate.Document> task) {
		def pr = new PRSpec(_context)
		pr.name "Closure Runner"
		pr.cls "uk.ac.gate.groovy.ClosureRunner"
		pr.init "targetClosure": task
		addPr(pr)
	}

	def toDSL(tablevel = 0) {
		// Represents this spec in DSL format
		def result = ["pipeline {"]

		result << "\tcls \"${_cls}\""
		if (_name) {
			result << "\tname \"${_name}\""
		}		

		def sanitiseValue = { value ->
			if (value instanceof File) {
					return value.toURI().relativize(new File(".".toURI())).getPath()
			}
			else {
				return value
			}
		}

		if (_features) {
			result << "\tfeatures {"
			_features.each { key, value -> 
				result << "\t\t${key} ${sanitiseValue(value).inspect()}"
			}
			result << "\t}"
		}	

		if (_init) {
			result << "\t init {"
			_init.each { key, value -> 
				result << "\t\t${key} ${value.inspect()}"
			}
			result << "\t }"
		}	

		this._plugins.each { directory ->
			result << "\tplugin \"${directory}\""
		} 

		this._prs.each { pr ->
			result << pr.toDSL(tablevel + 1) 
		} 
		
		result << "}"

		result = result.collect { line -> "\t" * tablevel + line} join "\n"

		return result
	}

	def build() {
		ClosureRunner.register()
    	_plugins.each { plugin ->
		 	try {
				def pluginFile = new File(plugin.toURI())
	    		Utils.loadPlugin(pluginFile)
			} catch(URISyntaxException e) {
				throw new IllegalArgumentException(
					"Plugin URL ${pluginUrl} doesn't give a valid URI", e)
			} catch(IllegalArgumentException e) {
				throw new IllegalArgumentException(
					"Plugin URL ${pluginUrl} can't be converted to File", e)
			}
    	}

		def target = Factory.createResource(_cls, 
			_init.toDeepFeatureMap(), 
			_features.toDeepFeatureMap(), 
			_name)			

		_prs.each { pr_spec -> 
			if (pr_spec.respondsTo("build")) {
				target.add(pr_spec.build())
			}
		}

		target.metaClass.mixin(RunnablePipeline)
		return target
    }
}

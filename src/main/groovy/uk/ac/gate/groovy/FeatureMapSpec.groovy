package uk.ac.gate.groovy 

import gate.FeatureMap
import java.util.LinkedHashMap
import gate.Factory


/**
 * Dictionary that you can write to with a closure, and which
 * knows how to turn itself into a FeatureMap
 */
class FeatureMapSpec extends LinkedHashMap<Object, Object> {
	PipelineContext context
	Closure closure
	public FeatureMapSpec(PipelineContext context) {
		super()

		if (context == null) {
			throw new NullPointerException("FeatureMapSpec created with null context")
		}
		this.context = context
	}

	public FeatureMapSpec(PipelineContext context, Closure cl) {
		super()

		if (context == null) {
			throw new NullPointerException("FeatureMapSpec created with null context")
		}
		this.context = context
		this.closure = closure
	}

	public FeatureMapSpec(PipelineContext, Map<Object, Object> values) {
		super(values)
		this.context = context
	}

	/**
	 * Builds a new instance from the given closure
	 */
	public static FeatureMapSpec fromClosure(PipelineContext context, Closure cl) {
		def instance = new FeatureMapSpec(context, cl)
		cl.delegate = instance
		cl()

		return instance
	}

	/**
	 * Allows you to use method invocation to set keys if you access the 
	 * feature map as a closure.
	 * This is usually not a fantastic idea if you want to make any method calls
	 * at all, including accessing the context.
	 */
	Object methodMissing(String name, Object args) {
		def value = args[0]

		// Defer method calls to the context object if needed. 
		if (context.metaClass.respondsTo(context, name, args)) {
			context.metaClass.invokeMethod(context, name, args)
		} else if (value instanceof Closure) {
			// Make this syntax available recursively.
			this[name] = new FeatureMapSpec(context)
			value.delegate = this[name]
			value.resolveStrategy = Closure.DELEGATE_FIRST
			value()
			this[name]
		} else {
		    this[name] = value
		}
	}

	/*
	void setProperty(String name, Object value) {
		if (value instanceof Closure) {
			// Make this syntax available recursively.
			this.put(name, new FeatureMapSpec(context))
			value.delegate = this[name]
			value.resolveStrategy = Closure.DELEGATE_FIRST
			value()
			this[name]
		} else {
		    this.put(name, value)
		}
	}*/

	FeatureMap toDeepFeatureMap() {
		// Convert to a feature map, but recursively.
	    def fm = Factory.newFeatureMap()

	    // Convert all values
	    def values = this.collectEntries { key, val ->
	    	if (val instanceof Map && !val.getMetaClass().respondsTo(val, "toDeepFeatureMap")) {
	    		[key, new FeatureMapSpec(context, val).toDeepFeatureMap()]
	    	} else if (val instanceof Map) {
	    		[key, val.toDeepFeatureMap()]
	    	} else {
	    		[key, val]
	    	}
	    }

	    fm.putAll(values)
	    return fm
	}

}
package uk.ac.gate.groovy
import java.nio.file.Paths

import java.net.URL
import gate.Gate

/**
 * Defines arbitrary context for building a pipeline. 
 * The most important data is:
 * 1) The GATE home and relative path for the pipeline to use.
 * 2) Initialisation arguments used by the pipeline.
 * You don't have to use the context, but it will make your life easier if you do.
 */
class PipelineContext {
	URL gateHome
	URL basePath
	FeatureMapSpec params // FeatureMapSpec because it gives closure syntax

	PipelineContext(URL gateHome, URL basePath, Map<Object, Object> params) {
		this.gateHome = gateHome
		this.basePath = basePath
		this.params   = new FeatureMapSpec(this, params)   
	}

	PipelineContext(URL gateHome, URL basePath) {
		this.gateHome = gateHome
		this.basePath = basePath
		this.params   = new FeatureMapSpec(this)
	}

	PipelineContext(PipelineContext pc) {
		this.gateHome = pc.gateHome
		this.basePath = pc.basePath
		this.params   = new FeatureMapSpec(this, pc.params)
	}
	
	PipelineContext(PipelineContext pc, Map<Object, Object> params) {
		this.gateHome = pc.gateHome
		this.basePath = pc.basePath
		this.params   = new FeatureMapSpec(this, params + pc.params)
	}

	static PipelineContext detectGateHome(Map params = [:]) {
		new PipelineContext(
			Gate.getGateHome().toURI().toURL(), 
			new File(".").toURI().toURL(),
			params
		) 
	}

	URL relativise(URL originalUrl, String... relativePath) {
		def originalPath = originalUrl.getPath()
		def newPath = Paths.get(originalPath, relativePath).normalize()
			
		// There's no copy constructor of URL, or setter for the path.
		return new URL(
			originalUrl.protocol,
			originalUrl.host,
			originalUrl.port,
			newPath.toString() + (originalUrl.query ?: ""))
	}

	URL rel(String... relativePath) {
		return relativise(basePath, relativePath)
	}

	URL home(String... relativePath) {
		return relativise(gateHome, relativePath)
	}

	def param(String key) {
		return params.get(key)
	}

	String toString() {
		return "Context home:${gateHome} base:${basePath} params:${params}"
	}
}
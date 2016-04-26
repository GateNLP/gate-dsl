package uk.ac.gate.groovy

import gate.util.persistence.PersistenceManager

/**
 * Specifies a pipeline that is just a GAPP file
 */
class PipelineGappSpec {
	def url

	def build() {
		return PersistenceManager.loadObjectFromUrl(url)
	}

	def toDSL(int tabLevel) {
		return "\t" * tabLevel + "pipeline new URL(\"${url.toString()}\")"
	}
}
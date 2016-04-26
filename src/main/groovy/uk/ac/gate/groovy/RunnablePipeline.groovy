package uk.ac.gate.groovy

import gate.Factory
import gate.Document
import gate.Controller

class RunnablePipeline {
	def withPipeline(Closure cl) {
		def corpus = Factory.newCorpus("RunnablePipeline Corpus")

		try {
			this.setCorpus(corpus)
			this.invokeControllerExecutionStarted()
			cl(this)
			this.invokeControllerExecutionFinished()
		} catch (Exception e) {
			this.invokeControllerExecutionAborted(e)
			throw e
		} finally {
			this.setCorpus(null)
			Factory.deleteResource(corpus)
		}
	}

	def execute(Document document) {
		this.setDocument(document)
		this.execute()
		this.setDocument(null)
	}
}
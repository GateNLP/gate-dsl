package uk.ac.gate.groovy
import gate.util.persistence.PersistenceManager
import gate.*


class PipelineBuilder {
	static pipeline (
			PipelineContext context, 
			@DelegatesTo(strategy=Closure.DELEGATE_FIRST, value=PipelineSpec) Closure cl) {
		if (!Gate.isInitialised()) {
			throw new IllegalStateException("GATE must be initialised before defining a pipeline")
		}
		
		def pipeline_spec = new PipelineSpec(context)
		def code = cl.rehydrate(pipeline_spec, cl.owner, cl.thisObject)
	    code.resolveStrategy = Closure.DELEGATE_FIRST
	    def result = code.delegate
	    code()

	    return result
	} 

	static pipeline (
			@DelegatesTo(strategy=Closure.DELEGATE_FIRST, value=PipelineSpec) Closure cl) {
	    return PipelineBuilder.pipeline(PipelineContext.detectGateHome(), cl)
	} 

	static fromController(controller) {
		return PipelineBuilder.pipeline {
			name controller.name
			features controller.features

			controller.getPRs().each { filePR ->
				if (filePR instanceof Controller) {
					pipeline fromController(filePR)
				} else {
					pr filePR.getMetaClass().theClass.name, {
						def resource_data = Gate.getCreoleRegister().get(filePR.metaClass.theClass.name)

						name filePR.name
						init {
							def init_target = delegate // Store this as it will be unbound in the deeper scope
							resource_data.parameterList.initimeParameters.each {
								it.each { param_data ->
									def param_value = filePR."${param_data.name}"

									if (param_value != param_data.getDefaultValue()) {
										init_target."${param_data.name}"(param_value)
									}
								}
							}
						}
		
						runtime {
							def feature_target = delegate // Store this as it will be unbound in the deeper scope
							resource_data.parameterList.runtimeParameters.each {
								it.each { param_data ->
									def param_value = filePR."${param_data.name}"
									if (param_value != param_data.getDefaultValue()) {
										feature_target."${param_data.name}"(param_value)
									}
								}
							}
						}
					}				
				}
			}
		}
	}
	static fromXGAPP(File input_file) {
		def controller = PersistenceManager.loadObjectFromFile(input_file)

		return fromController(controller)
	}
}
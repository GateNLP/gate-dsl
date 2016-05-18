package uk.ac.gate.groovy

import org.yaml.snakeyaml.Yaml
import groovy.lang.MetaClass
import groovy.lang.MissingMethodException
import gate.Gate
import gate.Factory 
import java.util.ArrayList
import java.net.URL
import groovy.io.FileType

abstract class PipelineScript extends Script {
    def pipeline
    def context

    // I don't really understand why, but if we use properties here, they
    // just get unset when we leave the scope of the PipelineBuilder closure
    private _builderDelegate
    def setBuilderDelegate(val) {
        _builderDelegate = val
    }

    def getBuilderDelegate() {
        return _builderDelegate 
    }

    /**
     * Will load the pipeline and run the provided (or default) main script.
     * Will init GATE if needed.
     */
    def run() {
        def cli = new CliBuilder(usage:"groovy pipeline.groovy [options] <>")
        cli._(longOpt:"config", args:1, argName:"optionsYaml", 
           "Location of the (yaml) options file to use.")
        cli._(longOpt:"gate", args:1, argName:"gate", 
           "Path to use for GATE home.")
        cli._(longOpt:"base", args:1, argName:"base", 
           "Base path to use for the script")
        cli.P(args:2, valueSeparator:'=', argName:'property=value',
               'use value for given property')

        def options = cli.parse(args)

        Gate.init()

        def gateHome = 
            options.gate ? new File(options.gate).toURI().toURL() : null
        def basePath = 
            options.base ? new File(options.base).toURI().toURL() : null

        def params = [:]
        if (options.Ps) {
             params << options.Ps.collate(2).collectEntries(Closure.IDENTITY)
        }

        // The context is needed to parse paths in the YAML properly, so build a partial context
        // to use while loading the config.
        def partialContext = buildContext(gateHome, basePath) 
        if (options.config) {
            new File(options.config).withInputStream { inputStream ->
                def yamlLoader = new Yaml(new URLConstructor(partialContext))
                params << yamlLoader.load(inputStream)
            }            
        }

        context = buildContext(gateHome,
                                   basePath,
                                   params)

        loadSpec(context)
        runScript(options.arguments(), pipeline)
    }

    def buildContext(URL home = null, URL rel = null, params = [:]) {
        home = home ?: Gate.getGateHome().toURI().toURL()
        rel = rel ?: new File(".").toURI().toURL()

        return new PipelineContext(home, rel, params)
    }

    /**
     * Just loads the pipeline spec which you can build later. 
     */
    def loadSpec(PipelineContext context) {
        // This is NOT threadsafe, because we can't predict the flow 
        // from the pipeline closure to the invokeMethod calls. 
        // Really nothing we can do about that though.
        pipeline = PipelineBuilder.pipeline(context, {
            this.setBuilderDelegate(delegate)
            runPipelineSpec()
        })
        
        return this.pipeline
    }

    /** 
     * Just loads the pipeline spec that you can build later. Uses default context
     */
    def loadSpec() {
        loadSpec(buildContext())
    }

    /**
     * Initialise the pipeline spec and builds an instance of it. 
     * Use this when importing a pipeline.
     * Assumes GATE has already been initialised.
     */
    def build() {
        return loadSpec().build()
    }

    /**
     * Delegates method calls from the contained script onto the pipeline 
     * builder closure as needed.
     */
    @Override
    def invokeMethod(String name, Object args) {
        // This is pretty much just the code from DelegatingScript, which
        // for some reason wouldn't work with the BaseScript tag.
        try {
            if (builderDelegate instanceof GroovyObject) {
               return builderDelegate.invokeMethod(name, args)
            }
            return metaClass.invokeMethod(builderDelegate, name, args)
        } catch (MissingMethodException mme) {
            try {
                super.invokeMethod(name, args)
            } catch (MissingMethodException mme2) {
                throw mme // we *REALLY* don't want to break the stack for all MMEs
            }
        }
    }

    // Placeholder for the code to define the pipeline.
    abstract def runPipelineSpec()

    /**
     * Provides a useful basic script which can be used to carry out common
     * tasks you might want to do with pipelines.
     * Can be overriden by providing a runScript method in your pipeline.groovy
     */
    def runScript(ArrayList<String> args, PipelineSpec pipeline) {
        // The way this is implemented is temporary - I want to use GCP eventually
        if (args.size() == 0) {
            println "No action given. Available actions: "
            println "gapp"
            println "run"
            println "deps"

        } else {
            def action = args[0]

            switch (action) {
                case "gapp": 
                    println "I would export a gapp, but I don't support this yet."
                    break;
                case "run":
                    runPipeline(pipeline, args[1..-2], args[-1])
                    break;
                case "runDir":
                    def inputFiles = []

                    new File(args[1]).eachFile FileType.FILES, { inputFile ->
                        inputFiles << "${inputFile}"
                    }

                    runPipeline(pipeline, inputFiles, args[-1])
                    break;
                case "deps":
                    println urlDependencies(pipeline).join("\n")
                    break;
                default:
                    println "Don't know how to ${action}"
                    break;
            }          
        }
    }

    def urlDependencies(PipelineSpec pipelineSpec) {
        def dependencies = [] as Set
        dependencies += pipelineSpec.plugins

        pipelineSpec.prs.each { pr ->
            dependencies += urlDependencies(pr)
        }
        return dependencies
    }

    def urlDependencies(PRSpec prSpec) {
        def dependencies = []

        dependencies += urlDependencies(prSpec._features)
        dependencies += urlDependencies(prSpec._init)
        dependencies += urlDependencies(prSpec._runtime)
        return dependencies
    }
    
    def urlDependencies(Map featureMap) {
        featureMap.values().findAll {
            it instanceof URL
        }
    }

    /**
     * Runs the pipeline on a selection of documents. Needs replacing with 
     * call to GHC 
     */
    def runPipeline(pipelineSpec, inputFiles, outputFolderName) {
        def pipeline = pipelineSpec.build()

        def exporter = Gate.getCreoleRegister()
                        .get("gate.corpora.export.GateXMLExporter")
                        .getInstantiations().first()

        def outputFolder = new File(outputFolderName)

        pipeline.withPipeline {
            inputFiles.each { fileName ->
                def outputFile = new File(fileName)

                def document = Factory.newDocument(outputFile.toURI().toURL())
                pipeline.execute(document)

                exporter.export(document, 
                    new File(outputFolder, outputFile.name), 
                    [:].toFeatureMap())
            }
        }
    }
}
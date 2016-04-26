package uk.ac.gate.groovy 

import org.yaml.snakeyaml.constructor.Constructor
import org.yaml.snakeyaml.constructor.AbstractConstruct
import org.yaml.snakeyaml.nodes.Tag
import org.yaml.snakeyaml.nodes.Node
class URLConstructor extends Constructor {
    public URLConstructor(PipelineContext context) {
        this.yamlConstructors.put(new Tag("!local"), new ConstructLocalURL(context));
        this.yamlConstructors.put(new Tag("!home"), new ConstructLocalURL(context));
    }

    private class ConstructLocalURL extends AbstractConstruct {
        private PipelineContext context;
        
        ConstructLocalURL(PipelineContext context) {
            super();
            this.context = context;
        }

        public Object construct(Node node) {
            String val = (String) constructScalar(node);
            return context.rel(val);
        }
    }

    private class ConstructHomeURL extends AbstractConstruct {
        ConstructHomeURL(PipelineContext context) {
            super();
            this.context = context;
        }

        public Object construct(Node node) {
            String val = (String) constructScalar(node);
            return context.home(val);
        }    
    }
}
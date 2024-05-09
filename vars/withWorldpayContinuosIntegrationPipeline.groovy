import com.worldpay.*

def call(String type, String tenant, String component, Closure body) {

    def pipelineTypes = [
    java: new SpringBootPipelineType(this, tenant, component),
//    nodejs: new NodePipelineType(this, product, component),
    ]

    PipelineType pipelineType = pipelineTypes.get(type) as PipelineType

    assert pipelineType != null

    pipelineType.call()

    body.call() // register pipeline config

}

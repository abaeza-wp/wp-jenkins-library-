import com.worldpay.*

def call(String type, String tenant, String component) {

    def pipelineTypes = [
    java: new SpringBootPipeline(tenant, component),
//    nodejs: new NodePipelineType(this, product, component),
    ]

    //TODO: For now use SpringBootPipeline but this should probably be a generic PipelineType
    SpringBootPipeline pipelineType = pipelineTypes.get(type)

    assert pipelineType != null

    pipelineType.call()

}

import com.oracle.bmc.auth.ConfigFileAuthenticationDetailsProvider
import com.oracle.bmc.core.ComputeClient
import com.oracle.bmc.core.model.Instance
import com.oracle.bmc.core.model.InstancePowerActionDetails
import com.oracle.bmc.core.model.ResetActionDetails
import com.oracle.bmc.core.requests.GetInstanceRequest
import com.oracle.bmc.core.requests.InstanceActionRequest
import com.oracle.bmc.core.requests.ListInstancesRequest
import com.oracle.bmc.core.responses.InstanceActionResponse
import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import java.io.File


class OracleInstanceExample(confFile: String) {
    private val PROFILE_DEFAULT = "DEFAULT"
    private val configurationFilePath = "~/.oci/config"  // Path to your OCI configuration file
    private val client: ComputeClient
    private val config: Config
    init {
        val authenticationDetailsProvider = ConfigFileAuthenticationDetailsProvider(configurationFilePath, PROFILE_DEFAULT)
        client = ComputeClient.builder().build(authenticationDetailsProvider)
//        config = ConfigFactory.load("oracle.conf")
        config = ConfigFactory.parseFile(File(confFile))
    }

    fun startInstances() {
        val instanceIds = listInstance()
        instanceIds.forEach {
            startInstance(it)
        }

    }

    fun listInstance(): List<String> {
        val compartmentId = config.getString("oracle.compartmentId")
        val listInstancesRequest = ListInstancesRequest.builder()
            .compartmentId(compartmentId)
            .lifecycleState(Instance.LifecycleState.Stopped)
            .build()

        val response = client.listInstances(listInstancesRequest)
        response.items.forEach {
            println(it.id)
            println( it.lifecycleState)
        }

        return response.items.map {
            return@map it.id
        }

    }

    fun startInstance(instanceId: String) {

        val instancePowerActionDetails: InstancePowerActionDetails = ResetActionDetails.builder()
            .allowDenseRebootMigration(false).build()

        val instanceActionRequest = InstanceActionRequest.builder()
            .instanceId(instanceId)
            .action("START")
//            .opcRetryToken("EXAMPLE-opcRetryToken-Value")
//            .ifMatch("EXAMPLE-ifMatch-Value")
            .instancePowerActionDetails(instancePowerActionDetails).build()

        /* Send request to the Client */
        try {
            val response: InstanceActionResponse = client.instanceAction(instanceActionRequest)
            println(response.toString())
        } catch (e: Exception) {
           e.printStackTrace()
//        } finally {
//            client.close()
        }

    }

    fun getInstanceState(instanceId: String) {
        // Build the request to get instance details
        val getInstanceRequest = GetInstanceRequest.builder()
            .instanceId(instanceId)
            .build()

        // Get the instance details
        val instanceResponse = client.getInstance(getInstanceRequest)
        val instance: Instance = instanceResponse.instance

        // Check the instance's lifecycleState
        if (instance.lifecycleState == Instance.LifecycleState.Running) {
            println("Instance is started.")
        } else if (instance.lifecycleState == Instance.LifecycleState.Stopped) {
            println("Instance is stopped.")
        } else {
            println("Instance is in an unknown state.")
        }

        client.close()
    }
}
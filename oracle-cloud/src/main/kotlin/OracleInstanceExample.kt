import com.oracle.bmc.Region
import com.oracle.bmc.auth.ConfigFileAuthenticationDetailsProvider
import com.oracle.bmc.core.ComputeClient
import com.oracle.bmc.core.ComputeWaiters
import com.oracle.bmc.core.VirtualNetworkClient
import com.oracle.bmc.core.model.*
import com.oracle.bmc.core.requests.*
import com.oracle.bmc.core.responses.InstanceActionResponse
import com.oracle.bmc.identity.IdentityClient
import com.oracle.bmc.identity.model.AvailabilityDomain
import com.oracle.bmc.identity.requests.ListAvailabilityDomainsRequest
import com.oracle.bmc.workrequests.WorkRequestClient
import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import java.io.File
import java.util.function.Consumer


class OracleInstanceExample(confFile: String) {
    private val PROFILE_DEFAULT = "DEFAULT"
    private val configurationFilePath = "~/.oci/config"  // Path to your OCI configuration file
    private val config: Config
    private val client: ComputeClient
    private val virtualNetworkClient: VirtualNetworkClient
    private val identityClient: IdentityClient
    private val computeWaiters: ComputeWaiters
    private val ad: AvailabilityDomain
    private val compartmentId: String
    private val sshPublicKey: String
    init {
        config = ConfigFactory.parseFile(File(confFile))
        val authenticationDetailsProvider = ConfigFileAuthenticationDetailsProvider(configurationFilePath, PROFILE_DEFAULT)
        client = ComputeClient.builder().build(authenticationDetailsProvider)
        client.setRegion(Region.AP_SINGAPORE_1)
        val workRequestClient = WorkRequestClient.builder().build(authenticationDetailsProvider)
        computeWaiters = client.newWaiters(workRequestClient)
        virtualNetworkClient = VirtualNetworkClient.builder().build(authenticationDetailsProvider)
        virtualNetworkClient.setRegion(Region.AP_SINGAPORE_1)
        identityClient = IdentityClient.builder().build(authenticationDetailsProvider)
        identityClient.setRegion(Region.AP_SINGAPORE_1)

        compartmentId = config.getString("oracle.compartmentId")
        sshPublicKey = config.getString("oracle.sshPublicKey")
        ad = getAvailabilityDomain()
    }

    // https://docs.oracle.com/en-us/iaas/api/#/en/iaas/20160918/ComputeCapacityReport/CreateComputeCapacityReport
    fun createComputeCapacityReport(shapeName: String, ocpus: Float, memory: Float): Boolean {
        val shapeConfig = CapacityReportInstanceShapeConfig.builder()
            .ocpus(ocpus)
            .memoryInGBs(memory)
            .build()
        val createCapacityReportShapeAvailabilityDetails = CreateCapacityReportShapeAvailabilityDetails.builder()
            .instanceShape(shapeName)
            .instanceShapeConfig(shapeConfig)
            .build()
        val createComputeCapacityReportDetails = CreateComputeCapacityReportDetails.builder()
            .compartmentId(compartmentId)
            .availabilityDomain(ad.name)
            .shapeAvailabilities(listOf(createCapacityReportShapeAvailabilityDetails))
            .build()

        val createComputeCapacityReportRequest = CreateComputeCapacityReportRequest.builder()
            .createComputeCapacityReportDetails(createComputeCapacityReportDetails)
            .build()
        val response = client.createComputeCapacityReport(createComputeCapacityReportRequest)
        val state = response.computeCapacityReport.shapeAvailabilities.first()
        println(state.availabilityStatus.value)

        return state.availabilityStatus == CapacityReportShapeAvailability.AvailabilityStatus.Available

    }

    fun tryCreateInstance(shapeName: String, ocpus: Float, memory: Float) {
        println("tryCreateInstance")
        if(!createComputeCapacityReport(shapeName, ocpus, memory)) {
            println("No available capacity for shape $shapeName")
            return
        }

        val shape = getShape(shapeName) ?: throw Exception("Shape is null")
        val image = getImage(shape)
        val vcn = createVcn()
        createInternetGateway(vcn)
        val subnet = createSubnet(vcn)
        val launchInstanceDetails = createLaunchInstanceDetails(
            shape,
            ocpus,
            memory,
            image,
            subnet,
            sshPublicKey
        )
        createInstance(launchInstanceDetails)
    }

    fun tryStartInstances(shapeName: String, ocpus: Float, memory: Float) {
        println("tryStartInstances")
        if(!createComputeCapacityReport(shapeName, ocpus, memory)) {
            println("No available capacity for shape $shapeName")
            return
        }
        val instanceIds = listInstance()
        instanceIds.forEach {
            startInstance(it)
        }

    }

    fun listInstance(): List<String> {
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

//        client.close()
    }

    fun getAvailabilityDomain(): AvailabilityDomain {
        val listAvailabilityDomainsRequest = ListAvailabilityDomainsRequest.builder()
            .compartmentId(compartmentId)
            .build()
        val listAvailabilityDomainsResponse = identityClient.listAvailabilityDomains(listAvailabilityDomainsRequest)

        return listAvailabilityDomainsResponse.items.first()
    }

    fun getShape(shapeName: String): Shape? {
        val listShapesRequest = ListShapesRequest.builder()
            .availabilityDomain(ad.name)
            .compartmentId(compartmentId)
            .build()
        val listShapesResponse = client.listShapes(listShapesRequest)
//        val shapes = listShapesResponse.items
//        shapes.forEach {
//            println(it.shape)
//        }
        val shape = listShapesResponse.items.find {
            it.shape == shapeName
        }
        println("get shape: ${shape?.shape}")

        return shape
    }

    fun getImage(shape: Shape): Image {
        // TODO: remove hardcoded values
        val listImagesRequest = ListImagesRequest.builder()
            .shape(shape.shape)
            .compartmentId(compartmentId)
            .operatingSystem("Oracle Linux")
            .operatingSystemVersion("8")
            .build()
        val listImagesResponse = client.listImages(listImagesRequest)
        val image = listImagesResponse.items.first()
        println("get image: ${image.displayName}")

        return image
    }

    fun createVcn(): Vcn {
        val vcnName = "java-sdk-vcn"
        // TODO: remove hardcoded values
        val cidrBlocks = listOf("10.0.0.0/16")
        val compartmentId = config.getString("oracle.compartmentId")
        val listVcnRequest = ListVcnsRequest.builder()
            .compartmentId(compartmentId)
            .displayName(vcnName)
            .build()
        val listVcnResponse = virtualNetworkClient.listVcns(listVcnRequest)
        if(listVcnResponse.items.size > 0) {
            println("Vcn(${vcnName}) already exists.")
            return listVcnResponse.items.first()
        }

        val createVcnDetails = CreateVcnDetails.builder()
            .cidrBlocks(cidrBlocks)
            .compartmentId(compartmentId)
            .displayName(vcnName)
            .dnsLabel("javasdkvcn") //TODO: remove hardcoded value
            .build()
        val createVcnRequest = CreateVcnRequest.builder().createVcnDetails(createVcnDetails).build()
        val createVcnResponse = virtualNetworkClient.createVcn(createVcnRequest)
        println("Vcn(${vcnName}) created.")

        return createVcnResponse.vcn
    }

    fun createInternetGateway(vcn: Vcn): InternetGateway {
        val internetGatewayName = "java-sdk-internet-gateway"
        val vcnId = vcn.id
        // List internet gateway before create, If the internet gateway exists, return the internet gateway
        val listInternetGatewaysRequest = ListInternetGatewaysRequest.builder()
            .compartmentId(compartmentId)
            .vcnId(vcnId)
            .displayName(internetGatewayName)
            .build()
        val listInternetGatewaysResponse = virtualNetworkClient.listInternetGateways(listInternetGatewaysRequest)
        if(listInternetGatewaysResponse.items.size > 0) {
            println("Internet Gateway(${internetGatewayName}) already exists.")
            return listInternetGatewaysResponse.items.first()
        }

        val createInternetGatewayDetails = CreateInternetGatewayDetails.builder()
            .compartmentId(compartmentId)
            .displayName(internetGatewayName)
            .isEnabled(true)
            .vcnId(vcnId)
            .build()
        val createInternetGatewayRequest = CreateInternetGatewayRequest.builder()
            .createInternetGatewayDetails(createInternetGatewayDetails)
            .build()
        val createInternetGatewayResponse = virtualNetworkClient.createInternetGateway(createInternetGatewayRequest)
        println("Internet Gateway(${internetGatewayName}) created.")

        val internetGateway = createInternetGatewayResponse.internetGateway
        addInternetGatewayToDefaultRouteTable(vcn, internetGateway)

        return internetGateway
    }

    private fun addInternetGatewayToDefaultRouteTable(
        vcn: Vcn, internetGateway: InternetGateway
    ) {
        val getRouteTableRequest = GetRouteTableRequest.builder().rtId(vcn.defaultRouteTableId).build()
        var getRouteTableResponse = virtualNetworkClient.getRouteTable(getRouteTableRequest)
        var routeRules = getRouteTableResponse.routeTable.routeRules
        println("Current Route Rules in Default Route Table")
        println("==========================================")
        routeRules.forEach(Consumer { x: RouteRule? -> println(x) })
        println()
        // TODO: remove hardcoded values
        val internetAccessRoute = RouteRule.builder()
            .destination("0.0.0.0/0")
            .destinationType(RouteRule.DestinationType.CidrBlock)
            .networkEntityId(internetGateway.id)
            .build()
        routeRules.add(internetAccessRoute)
        val updateRouteTableDetails = UpdateRouteTableDetails.builder().routeRules(routeRules).build()
        val updateRouteTableRequest = UpdateRouteTableRequest.builder()
            .updateRouteTableDetails(updateRouteTableDetails)
            .rtId(vcn.defaultRouteTableId)
            .build()
        virtualNetworkClient.updateRouteTable(updateRouteTableRequest)
        getRouteTableResponse = virtualNetworkClient
            .waiters
            .forRouteTable(getRouteTableRequest, RouteTable.LifecycleState.Available)
            .execute()
        routeRules = getRouteTableResponse.routeTable.routeRules
        println("Updated Route Rules in Default Route Table")
        println("==========================================")
        routeRules.forEach(Consumer { x: RouteRule? -> println(x) })
        println()
    }

    fun createSubnet(vcn: Vcn): Subnet {
        // TODO: remove hardcoded values
        val cidr = "10.0.0.0/16"
        val subnetName = "java-sdk-subnet"
        val vcnId = vcn.id
        // list subnet before create, If the subnet exists, return the subnet
        val listSubnetRequest = ListSubnetsRequest.builder()
            .compartmentId(compartmentId)
            .vcnId(vcnId)
            .displayName(subnetName)
            .build()
        val listSubnetResponse = virtualNetworkClient.listSubnets(listSubnetRequest)
        if(listSubnetResponse.items.size > 0) {
            println("Subnet(${subnetName}) already exists.")
            return listSubnetResponse.items.first()
        }

        val createSubnetDetails = CreateSubnetDetails.builder()
            .availabilityDomain(ad.name)
            .compartmentId(compartmentId)
            .displayName(subnetName)
            .cidrBlock(cidr)
            .vcnId(vcnId)
            .routeTableId(vcn.defaultRouteTableId)
            .build()

        val createSubnetRequest = CreateSubnetRequest.builder()
            .createSubnetDetails(createSubnetDetails)
            .build()
        val createSubnetResponse = virtualNetworkClient.createSubnet(createSubnetRequest)
        println("Subnet(${subnetName}) created.")

        return createSubnetResponse.subnet
    }

    fun createLaunchInstanceDetails(shape: Shape,
                                    ocpus: Float,
                                    memory: Float,
                                    image: Image,
                                    subnet: Subnet,
                                    sshPublicKey: String): LaunchInstanceDetails {
        val instanceName = "java-sdk-launch-instance"
        val metadata = mapOf(
            "ssh_authorized_keys" to sshPublicKey,
        )

        val instanceSourceViaImageDetails = InstanceSourceViaImageDetails.builder()
            .imageId(image.id)
            .build()
        val createVnicDetails = CreateVnicDetails.builder()
            .subnetId(subnet.id)
            .assignPublicIp(true)
            .build()
        val shapeConfig = LaunchInstanceShapeConfigDetails.builder()
            .ocpus(ocpus)
            .memoryInGBs(memory)
            .build()

        val launchInstanceDetails = LaunchInstanceDetails.builder()
            .availabilityDomain(ad.name)
            .compartmentId(compartmentId)
            .displayName(instanceName)
            .sourceDetails(instanceSourceViaImageDetails)
            .createVnicDetails(createVnicDetails)
            .metadata(metadata)
            .shape(shape.shape)
            .shapeConfig(shapeConfig)
            .createVnicDetails(createVnicDetails)
            .build()

        return launchInstanceDetails
    }

    // https://docs.oracle.com/en-us/iaas/api/#/en/iaas/20160918/Instance/LaunchInstance
    fun createInstance(launchInstanceDetails: LaunchInstanceDetails) {
        val launchInstanceRequest = LaunchInstanceRequest.builder()
            .launchInstanceDetails(launchInstanceDetails)
            .build()

        val launchInstanceResponse = computeWaiters.forLaunchInstance(launchInstanceRequest).execute()
        println("instance ${launchInstanceResponse.instance.displayName} was launched" )

    }
}
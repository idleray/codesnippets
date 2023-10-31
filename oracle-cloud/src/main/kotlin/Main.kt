val smaple = """
            BM.Standard.A1.160
            VM.Standard.A1.Flex
            VM.Standard.E2.1.Micro
        """

fun main(args: Array<String>) {
    val confFile = args[0]
    val oracleInstanceExample = OracleInstanceExample(confFile)
//    oracleInstanceExample.getShape()

//    oracleInstanceExample.createComputeCapacityReport("VM.Standard.A1.Flex", 2f, 12f)
//    oracleInstanceExample.createComputeCapacityReport("VM.Standard.E2.1.Micro", 1f, 1f)
    try {
        oracleInstanceExample.tryStartInstances("VM.Standard.E2.1.Micro", 1f, 1f)
        oracleInstanceExample.tryCreateInstance("VM.Standard.A1.Flex", 2f, 12f)
//        oracleInstanceExample.tryCreateInstance("VM.Standard.E2.1.Micro", 1f, 1f)
    } catch (e: Exception) {
        println(e.message)
    }
}
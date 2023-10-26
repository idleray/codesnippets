fun main(args: Array<String>) {
    val confFile = args[0]
    val oracleInstanceExample = OracleInstanceExample(confFile)
    oracleInstanceExample.startInstances()
}
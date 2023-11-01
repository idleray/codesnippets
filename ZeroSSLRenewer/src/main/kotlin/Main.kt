import org.bouncycastle.util.io.pem.PemObject
import org.bouncycastle.util.io.pem.PemWriter
import java.io.StringWriter
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.nio.file.Files
import java.nio.file.Paths
import java.time.Duration
import java.time.Instant

fun getCertificateExpirationTime(certificatePath: String): Instant? {
    val certificateBytes = Files.readAllBytes(Paths.get(certificatePath))
    val certificateFactory = CertificateFactory.getInstance("X.509")
    val certificate = certificateFactory.generateCertificate(certificateBytes.inputStream()) as X509Certificate
    return certificate.notAfter.toInstant()
}

fun isCertificateExpiringSoon(expirationTime: Instant, checkDays: Long): Boolean {
    val currentTime = Instant.now()
    val timeUntilExpiration = Duration.between(currentTime, expirationTime)

    val thirtyDays = Duration.ofDays(checkDays)
    return timeUntilExpiration <= thirtyDays
}

fun main(args: Array<String>) {
    if(args.size < 2) {
        println("Usage: <certificateName> <apiKey>")
        return
    }
    val certificateName = args[0]
    val apiKey = args[1]

    val zeroSSLWrapper = ZeroSSLWrapper(certificateName, apiKey)
    zeroSSLWrapper.createCertificate()
//    zeroSSLWrapper.validateCSR("")

}
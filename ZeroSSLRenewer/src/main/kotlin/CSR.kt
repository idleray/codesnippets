import org.bouncycastle.asn1.x500.X500Name
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo
import org.bouncycastle.operator.ContentSigner
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder
import org.bouncycastle.pkcs.PKCS10CertificationRequest
import org.bouncycastle.pkcs.PKCS10CertificationRequestBuilder
import org.bouncycastle.pkcs.jcajce.JcaPKCS10CertificationRequestBuilder
import org.bouncycastle.operator.OperatorCreationException
import org.bouncycastle.util.io.pem.PemObject
import org.bouncycastle.util.io.pem.PemWriter
import java.io.StringWriter
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.NoSuchAlgorithmException

fun generateCSR(commonName: String, organization: String = "", organizationalUnit: String = "", locality: String = "",
                state: String = "", country: String = ""): String {
    try {
        // Generate key pair
        val keyPair = generateKeyPair()

        // Create CSR subject
        val subject = X500Name("CN=$commonName, O=$organization, OU=$organizationalUnit, L=$locality, ST=$state, C=$country")

        // Create CSR builder
        val csrBuilder: PKCS10CertificationRequestBuilder = JcaPKCS10CertificationRequestBuilder(subject, keyPair.public)

        // Create content signer
        val contentSigner: ContentSigner = JcaContentSignerBuilder("SHA256withRSA").build(keyPair.private)

        // Build CSR
        val csr: PKCS10CertificationRequest = csrBuilder.build(contentSigner)

        val stringWriter = StringWriter()
        val pemWriter = PemWriter(stringWriter)
        pemWriter.writeObject(PemObject("CERTIFICATE REQUEST", csr.encoded))
        pemWriter.close()

        val csrPEM = stringWriter.toString()

        return csrPEM
    } catch (e: NoSuchAlgorithmException) {
        e.printStackTrace()
    } catch (e: OperatorCreationException) {
        e.printStackTrace()
    }
    return ""
}

fun generateKeyPair(): KeyPair {
    val keyPairGenerator = KeyPairGenerator.getInstance("RSA")
    keyPairGenerator.initialize(2048)
    return keyPairGenerator.generateKeyPair()
}


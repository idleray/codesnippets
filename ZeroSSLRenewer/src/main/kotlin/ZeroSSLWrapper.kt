import com.google.gson.Gson
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

data class CreateCertificateRequest(
    val certificate_domains: String,
    val certificate_validity_days: Int,
    val certificate_csr: String
)

data class ValidateCSRRequest(
    val csr: String
)

class ZeroSSLWrapper(private val certificateName: String, private val apiKey: String) {
    fun createCertificate() {
        val csr = generateCSR(certificateName)

        val client = OkHttpClient()

        val requestPayload = CreateCertificateRequest(
            certificate_domains = certificateName,
            certificate_validity_days = 90,
            certificate_csr = csr
        )
        val gson = Gson()
        val reqStr = gson.toJson(requestPayload)
        println(reqStr)

        val requestBody = reqStr.toRequestBody("application/json".toMediaType())

        // Create the request
        val request = Request.Builder()
            .url("https://api.zerossl.com/certificates?access_key=$apiKey")
            .post(requestBody)
            .build()

        // Make the API call
        val response = client.newCall(request).execute()

        // Handle the response
        if (response.isSuccessful) {
            val responseBody = response.body?.string()
//            println("Certificate created successfully!")
            println(responseBody)
        } else {
            println("Failed to create certificate. Error: ${response.body?.string()}")
        }
    }

    fun validateCSR(csr1: String) {
        val csr = generateCSR(certificateName)
        val validateCSRRequest = ValidateCSRRequest(
            csr
        )
        val gson = Gson()
        val reqStr = gson.toJson(validateCSRRequest)
        println(reqStr)

        val request = Request.Builder()
            .url("https://api.zerossl.com/validation/csr?access_key=$apiKey")
            .post(reqStr.toRequestBody("application/json".toMediaType()))
            .build()

        val response = OkHttpClient().newCall(request).execute()
        if(response.isSuccessful) {
            val responseBody = response.body?.string()
            println(responseBody)
        } else {
            println("Error: ${response.body?.string()}")
        }
    }
}
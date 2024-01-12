package kp.ran.kotlinfirebaseotp

import android.app.Activity
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.google.firebase.FirebaseException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthMissingActivityForRecaptchaException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kp.ran.kotlinfirebaseotp.ui.theme.KotlinFirebaseOTPTheme
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {

    companion object {
        private const val TAG = "otploghere"
    }


    // [START on_start_check_user]
    /*  override fun onStart() {
        super.onStart()
        // Check if user is signed in (non-null) and update UI accordingly.
        val currentUser = auth.currentUser
        updateUI(currentUser)
    }*/
    // [END on_start_check_user]
    // [START declare_auth]
    private lateinit var auth: FirebaseAuth
// [END declare_auth]

    private var storedVerificationId: String? = ""
    private lateinit var resendToken: PhoneAuthProvider.ForceResendingToken
    private lateinit var callbacks: PhoneAuthProvider.OnVerificationStateChangedCallbacks

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            KotlinFirebaseOTPTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // [START initialize_auth]
                    // Initialize Firebase Auth
                    auth = Firebase.auth
                    // [END initialize_auth]

                    // Initialize phone auth callbacks
                    // [START phone_auth_callbacks]
                    callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {

                        override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                            // This callback will be invoked in two situations:
                            // 1 - Instant verification. In some cases the phone number can be instantly
                            //     verified without needing to send or enter a verification code.
                            // 2 - Auto-retrieval. On some devices Google Play services can automatically
                            //     detect the incoming verification SMS and perform verification without
                            //     user action.

                            Log.d(TAG, "onVerificationCompleted:$credential")
                            signInWithPhoneAuthCredential(credential)
                        }

                        override fun onVerificationFailed(e: FirebaseException) {
                            // This callback is invoked in an invalid request for verification is made,
                            // for instance if the the phone number format is not valid.
                            Log.w(TAG, "onVerificationFailed", e)

                            if (e is FirebaseAuthInvalidCredentialsException) {
                                // Invalid request
                            } else if (e is FirebaseTooManyRequestsException) {
                                // The SMS quota for the project has been exceeded
                            } else if (e is FirebaseAuthMissingActivityForRecaptchaException) {
                                // reCAPTCHA verification attempted with null Activity
                            }

                            // Show a message and update the UI
                        }

                        override fun onCodeSent(
                            verificationId: String,
                            token: PhoneAuthProvider.ForceResendingToken,
                        ) {
                            // The SMS verification code has been sent to the provided phone number, we
                            // now need to ask the user to enter the code and then construct a credential
                            // by combining the code with a verification ID.
                            Log.d(TAG, "onCodeSent:$verificationId")

                            // Save verification ID and resending token so we can use them later
                            storedVerificationId = verificationId
                            resendToken = token
                        }
                    }
                    // [END phone_auth_callbacks]
                    Column {

                        var num by remember {
                            mutableStateOf("")
                        }
                        var otp by remember {
                            mutableStateOf("")
                        }
                        var result by remember {
                            mutableStateOf("")
                        }
                        var send by remember {
                            mutableStateOf(false)
                        }
                        var verfiy by remember {
                            mutableStateOf(false)
                        }
                        if (send) {
                            startPhoneNumberVerification(num)
                        }

                        if (verfiy) {
                            verifyPhoneNumberWithCode(verificationId = storedVerificationId, otp)
                        }


                        OutlinedTextField(value = num, onValueChange = { num = it })
                        Button(onClick = { /*TODO*/ }) {
                            Text(text = "send OTP")
                        }
                        OutlinedTextField(value = otp, onValueChange = { otp = it })
                        Button(onClick = { /*TODO*/ }) {
                            Text(text = "Sign in")
                        }
                        Text(text = result)
                    }
                }
            }
        }
    }

    private fun startPhoneNumberVerification(phoneNumber: String) {
        // [START start_phone_auth]
        val options = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(phoneNumber) // Phone number to verify
            .setTimeout(60L, TimeUnit.SECONDS) // Timeout and unit
            .setActivity(applicationContext as Activity) // Activity (for callback binding)
            .setCallbacks(callbacks) // OnVerificationStateChangedCallbacks
            .build()
        PhoneAuthProvider.verifyPhoneNumber(options)

        // [END start_phone_auth]
    }

    private fun verifyPhoneNumberWithCode(verificationId: String?, code: String) {
        // [START verify_with_code]
        val credential = PhoneAuthProvider.getCredential(verificationId!!, code)
        // [END verify_with_code]
    }

    // [START resend_verification]
    private fun resendVerificationCode(
        phoneNumber: String,
        token: PhoneAuthProvider.ForceResendingToken?,
    ) {
        val optionsBuilder = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(phoneNumber) // Phone number to verify
            .setTimeout(60L, TimeUnit.SECONDS) // Timeout and unit
            .setActivity(applicationContext as Activity) // (optional) Activity for callback binding
            // If no activity is passed, reCAPTCHA verification can not be used.
            .setCallbacks(callbacks) // OnVerificationStateChangedCallbacks
        if (token != null) {
            optionsBuilder.setForceResendingToken(token) // callback's ForceResendingToken
        }
        PhoneAuthProvider.verifyPhoneNumber(optionsBuilder.build())
    }
// [END resend_verification]

    // [START sign_in_with_phone]
    private fun signInWithPhoneAuthCredential(credential: PhoneAuthCredential) {
        auth.signInWithCredential(credential)
            .addOnCompleteListener(applicationContext as Activity) { task ->
                if (task.isSuccessful) {
                    // Sign in success, update UI with the signed-in user's information
                    Log.d("otploghere", "signInWithCredential:success")

                    val user = task.result?.user
                } else {
                    // Sign in failed, display a message and update the UI
                    Log.w("otploghere", "signInWithCredential:failure", task.exception)
                    if (task.exception is FirebaseAuthInvalidCredentialsException) {
                        // The verification code entered was invalid
                    }
                    // Update UI
                }
            }
    }
// [END sign_in_with_phone]

    private fun updateUI(user: FirebaseUser? = auth.currentUser) {
        //  user.phoneNumber
    }

}
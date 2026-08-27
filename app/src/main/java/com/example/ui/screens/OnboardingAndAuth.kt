package com.example.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.ui.components.GlassCard
import com.example.ui.components.ProfileCameraCaptureDialog
import com.example.ui.components.VenzoraLogoEmblem
import com.example.ui.theme.*
import com.example.ui.viewmodel.BharatChatViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.viewmodel.AuthenticationViewModel
import com.example.ui.viewmodel.PhoneAuthStatus
import com.example.ui.viewmodel.findActivity
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import kotlinx.coroutines.delay
import java.util.concurrent.TimeUnit

data class Country(
    val name: String,
    val code: String,
    val flag: String,
    val iso: String,
    val placeholder: String
)

val INTERNATIONAL_COUNTRIES = listOf(
    Country("India", "+91", "🇮🇳", "IN", "98765 43210"),
    Country("United States", "+1", "🇺🇸", "US", "(555) 000-0000"),
    Country("United Kingdom", "+44", "🇬🇧", "GB", "7911 123456"),
    Country("United Arab Emirates", "+971", "🇦🇪", "AE", "50 123 4567"),
    Country("Saudi Arabia", "+966", "🇸🇦", "SA", "50 123 4567"),
    Country("Canada", "+1", "🇨🇦", "CA", "(555) 000-0000"),
    Country("Australia", "+61", "🇦🇺", "AU", "412 345 678"),
    Country("Germany", "+49", "🇩🇪", "DE", "151 12345678"),
    Country("France", "+33", "🇫🇷", "FR", "6 12 34 56 78"),
    Country("Singapore", "+65", "🇸🇬", "SG", "8123 4567"),
    Country("Brazil", "+55", "🇧🇷", "BR", "11 91234-5678"),
    Country("Japan", "+81", "🇯🇵", "JP", "90-1234-5678"),
    Country("Nigeria", "+234", "🇳🇬", "NG", "802 123 4567"),
    Country("South Africa", "+27", "🇿🇦", "ZA", "71 123 4567"),
    Country("Pakistan", "+92", "🇵🇰", "PK", "300 1234567"),
    Country("Bangladesh", "+880", "🇧🇩", "BD", "1712-345678"),
    Country("Indonesia", "+62", "🇮🇩", "ID", "812-3456-7890"),
    Country("Russia", "+7", "🇷🇺", "RU", "912 345-67-89"),
    Country("Mexico", "+52", "🇲🇽", "MX", "55 1234 5678"),
    Country("Italy", "+39", "🇮🇹", "IT", "312 345 6789"),
    Country("Spain", "+34", "🇪🇸", "ES", "612 34 56 78"),
    Country("Nepal", "+977", "🇳🇵", "NP", "984-1234567"),
    Country("Sri Lanka", "+94", "🇱🇰", "LK", "71 234 5678"),
    Country("Malaysia", "+60", "🇲🇾", "MY", "12-345 6789"),
    Country("Philippines", "+63", "🇵🇭", "PH", "917 123 4567"),
    Country("Egypt", "+20", "🇪🇬", "EG", "100 123 4567"),
    Country("Turkey", "+90", "🇹🇷", "TR", "532 123 4567"),
    Country("Netherlands", "+31", "🇳🇱", "NL", "6 12345678"),
    Country("Switzerland", "+41", "🇨🇭", "CH", "78 123 45 67"),
    Country("Sweden", "+46", "🇸🇪", "SE", "70 123 45 67"),
    Country("Qatar", "+974", "🇶🇦", "QA", "3312 3456"),
    Country("Kuwait", "+965", "🇰🇼", "KW", "5123 4567"),
    Country("Oman", "+968", "🇴🇲", "OM", "9123 4567"),
    Country("Bahrain", "+973", "🇧🇭", "BH", "3600 1234"),
    Country("New Zealand", "+64", "🇳🇿", "NZ", "21 123 4567"),
    Country("Kenya", "+254", "🇰🇪", "KE", "712 345678"),
    Country("Ghana", "+233", "🇬🇭", "GH", "24 123 4567"),
    Country("South Korea", "+82", "🇰🇷", "KR", "10-1234-5678"),
    Country("Thailand", "+66", "🇹🇭", "TH", "81 234 5678"),
    Country("Vietnam", "+84", "🇻🇳", "VN", "91 234 56 78"),
    Country("Argentina", "+54", "🇦🇷", "AR", "11 1234-5678"),
    Country("Colombia", "+57", "🇨🇴", "CO", "300 1234567"),
    Country("Ireland", "+353", "🇮🇪", "IE", "85 123 4567"),
    Country("Norway", "+47", "🇳🇴", "NO", "412 34 567"),
    Country("Denmark", "+45", "🇩🇰", "DK", "21 23 45 67"),
    Country("Finland", "+358", "🇫🇮", "FI", "40 1234567"),
    Country("Poland", "+48", "🇵🇱", "PL", "512 345 678"),
    Country("Portugal", "+351", "🇵🇹", "PT", "912 345 678"),
    Country("Austria", "+43", "🇦🇹", "AT", "664 1234567"),
    Country("Greece", "+30", "🇬🇷", "GR", "691 234 5678"),
    Country("Israel", "+972", "🇮🇱", "IL", "50-123-4567")
)

data class OnboardingSlide(
    val title: String,
    val subtitle: String,
    val badge: String,
    val icon: ImageVector,
    val gradientColors: List<Color>
)

@Composable
fun OnboardingScreen(
    onContinueToAuth: () -> Unit
) {
    val slides = listOf(
        OnboardingSlide(
            title = "Simple. Reliable. Private.",
            subtitle = "With VenzoInd, you'll get fast, simple, secure messaging and calling with end-to-end Signal encryption, available all over the world.",
            badge = "End-to-End Encrypted",
            icon = Icons.Default.Security,
            gradientColors = listOf(BharatGreenLight, Color(0xFF0F172A))
        ),
        OnboardingSlide(
            title = "International Mobile Verification",
            subtitle = "Connect seamlessly with friends and family worldwide across 50+ countries with instant SMS OTP and sovereign data privacy.",
            badge = "Global Sovereign Network",
            icon = Icons.Default.Public,
            gradientColors = listOf(BharatElectricCyan, BharatNavy)
        ),
        OnboardingSlide(
            title = "Stay Connected with Groups & Calls",
            subtitle = "Crystal-clear HD voice and video calls, rich group chats, status updates, and peer-to-peer fast transfers.",
            badge = "WhatsApp-Grade Experience",
            icon = Icons.Default.Forum,
            gradientColors = listOf(BharatSaffron, Color(0xFF1E293B))
        )
    )

    var currentSlideIndex by remember { mutableStateOf(0) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header skip button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(
                    onClick = onContinueToAuth,
                    modifier = Modifier.testTag("skip_onboarding_button")
                ) {
                    Text("Skip", color = TextSecondaryDark, fontSize = 14.sp)
                }
            }

            // Central Visual Card
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                VenzoraLogoEmblem(size = 90.dp)

                Spacer(modifier = Modifier.height(28.dp))

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = BharatGreenLight.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, BharatGreenLight.copy(alpha = 0.4f))
                ) {
                    Text(
                        text = slides[currentSlideIndex].badge,
                        color = BharatGreenLight,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = slides[currentSlideIndex].title,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = BharatWhite,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = slides[currentSlideIndex].subtitle,
                    fontSize = 14.sp,
                    color = TextSecondaryDark,
                    textAlign = TextAlign.Center,
                    lineHeight = 22.sp,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Page Indicator Dots
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    slides.indices.forEach { index ->
                        Box(
                            modifier = Modifier
                                .height(6.dp)
                                .width(if (index == currentSlideIndex) 24.dp else 6.dp)
                                .clip(CircleShape)
                                .background(
                                    if (index == currentSlideIndex) BharatGreenLight else Color(0xFF334155)
                                )
                        )
                    }
                }
            }

            // Bottom Actions
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = {
                        if (currentSlideIndex < slides.size - 1) {
                            currentSlideIndex++
                        } else {
                            onContinueToAuth()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("onboarding_next_button"),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BharatGreenLight
                    )
                ) {
                    Text(
                        text = if (currentSlideIndex == slides.size - 1) "Agree and Continue" else "Continue",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = BharatWhite
                    )
                }

                Text(
                    text = "Tap 'Agree and continue' to accept the VenzoInd Terms of Service and Privacy Policy",
                    fontSize = 11.sp,
                    color = TextMutedDark,
                    textAlign = TextAlign.Center,
                    lineHeight = 15.sp,
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
            }
        }
    }
}

// ---------------------------------------------------------------------
// AUTH SCREEN: WHATSAPP-IDENTICAL INTERNATIONAL MOBILE & OTP FLOW
// ---------------------------------------------------------------------
enum class AuthStep {
    ENTER_PHONE,
    VERIFY_OTP,
    PROFILE_SETUP
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthScreen(
    onLoginSuccess: () -> Unit,
    viewModel: BharatChatViewModel? = null,
    authViewModel: AuthenticationViewModel = viewModel()
) {
    val context = LocalContext.current
    var currentStep by remember { mutableStateOf(AuthStep.ENTER_PHONE) }

    val isNewAccountMode by authViewModel.isNewAccountMode.collectAsState()
    val selectedCountry by authViewModel.selectedCountry.collectAsState()
    val phoneNumberInput by authViewModel.phoneNumberInput.collectAsState()
    val phoneValidationError by authViewModel.phoneValidationError.collectAsState()
    val detectedSims by authViewModel.detectedSims.collectAsState()
    val selectedSim by authViewModel.selectedSim.collectAsState()
    val authStatus by authViewModel.authStatus.collectAsState()
    val otpDigits by authViewModel.otpDigits.collectAsState()
    val generatedBackupOtp by authViewModel.generatedBackupOtp.collectAsState()
    val resendTimer by authViewModel.resendTimer.collectAsState()
    val callTimer by authViewModel.callTimer.collectAsState()
    val otpErrorMsg by authViewModel.otpErrorMessage.collectAsState()
    val isVerifying by authViewModel.isVerifying.collectAsState()

    var showCountryPickerSheet by remember { mutableStateOf(false) }
    var countrySearchQuery by remember { mutableStateOf("") }
    var showConfirmNumberDialog by remember { mutableStateOf(false) }
    val focusRequesters = remember { List(6) { FocusRequester() } }

    // Profile Setup states
    var userNameInput by remember { mutableStateOf("VenzoInd User") }
    var userStatusInput by remember { mutableStateOf("Hey there! I am using VenzoInd.") }
    var customDpUri by remember { mutableStateOf<String?>(null) }
    var showCameraCaptureDialog by remember { mutableStateOf(false) }
    var showAvatarOptionsSheet by remember { mutableStateOf(false) }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            customDpUri = uri.toString()
            showAvatarOptionsSheet = false
            Toast.makeText(context, "Profile photo selected", Toast.LENGTH_SHORT).show()
        }
    }

    // Trigger phone auth verification when moving to OTP step
    LaunchedEffect(currentStep) {
        if (currentStep == AuthStep.VERIFY_OTP) {
            val activity = context.findActivity()
            authViewModel.startPhoneVerification(activity)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        when (currentStep) {
            // STEP 1: WhatsApp Enter Phone Number
            AuthStep.ENTER_PHONE -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        // Mode Selection Tabs: Create Account vs Sign In
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF1E293B),
                            border = BorderStroke(1.dp, Color(0xFF334155)),
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(4.dp),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isNewAccountMode) BharatGreenLight else Color.Transparent,
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { authViewModel.setAuthMode(true) }
                                        .padding(vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "Create Account",
                                        textAlign = TextAlign.Center,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isNewAccountMode) BharatWhite else TextSecondaryDark,
                                        modifier = Modifier.padding(vertical = 8.dp)
                                    )
                                }
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (!isNewAccountMode) BharatGreenLight else Color.Transparent,
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { authViewModel.setAuthMode(false) }
                                        .padding(vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "Log In",
                                        textAlign = TextAlign.Center,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (!isNewAccountMode) BharatWhite else TextSecondaryDark,
                                        modifier = Modifier.padding(vertical = 8.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        Text(
                            text = if (isNewAccountMode) "Create your VenzoInd account" else "Log into your account",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = BharatWhite,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = if (isNewAccountMode)
                                "Register with your phone number to get started with end-to-end encrypted messaging."
                            else
                                "Enter your registered phone number to verify your identity and restore your chats.",
                            fontSize = 13.sp,
                            color = TextSecondaryDark,
                            textAlign = TextAlign.Center,
                            lineHeight = 19.sp,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        // WhatsApp Country Picker Selector Row
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showCountryPickerSheet = true }
                                .testTag("country_picker_button"),
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF1E293B),
                            border = BorderStroke(1.dp, Color(0xFF334155))
                        ) {
                            Row(
                                modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Text(selectedCountry.flag, fontSize = 22.sp)
                                    Text(
                                        text = selectedCountry.name,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = BharatWhite
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = "Select Country",
                                    tint = BharatGreenLight
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // WhatsApp Code + Phone Input Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Dial code box with Flag and Picker trigger
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFF1E293B),
                                border = BorderStroke(1.dp, Color(0xFF334155)),
                                modifier = Modifier
                                    .width(105.dp)
                                    .height(56.dp)
                                    .clickable { showCountryPickerSheet = true }
                                    .testTag("dial_code_picker_button")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Text(selectedCountry.flag, fontSize = 18.sp)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = selectedCountry.code,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = BharatGreenLight
                                    )
                                }
                            }

                            // Mobile number field
                            OutlinedTextField(
                                value = phoneNumberInput,
                                onValueChange = { input ->
                                    authViewModel.updatePhoneNumber(input)
                                },
                                placeholder = {
                                    Text(
                                        text = selectedCountry.placeholder,
                                        color = TextMutedDark,
                                        fontSize = 15.sp
                                    )
                                },
                                singleLine = true,
                                isError = phoneValidationError != null,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = BharatGreenLight,
                                    unfocusedBorderColor = if (phoneValidationError != null) Color(0xFFEF4444) else Color(0xFF334155),
                                    focusedContainerColor = Color(0xFF1E293B),
                                    unfocusedContainerColor = Color(0xFF1E293B),
                                    focusedTextColor = BharatWhite,
                                    unfocusedTextColor = BharatWhite,
                                    errorBorderColor = Color(0xFFEF4444)
                                ),
                                shape = RoundedCornerShape(12.dp),
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Phone,
                                    imeAction = ImeAction.Done
                                ),
                                keyboardActions = KeyboardActions(
                                    onDone = {
                                        if (authViewModel.validateCurrentPhone(context)) {
                                            showConfirmNumberDialog = true
                                        } else {
                                            Toast.makeText(context, phoneValidationError ?: "कृपया इसी मोबाइल के सिम कार्ड का नंबर दर्ज करें।", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(56.dp)
                                    .testTag("international_phone_input")
                            )
                        }

                        // Phone validation error feedback
                        if (phoneValidationError != null) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "⚠️ ${phoneValidationError ?: ""}",
                                color = Color(0xFFEF4444),
                                fontSize = 12.sp,
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Device SIM Card Detection Section
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = Color(0xFF1E293B),
                            border = BorderStroke(1.dp, Color(0xFF334155)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.SimCard, contentDescription = null, tint = BharatGreenLight, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "डिवाइस सिम कार्ड (VERIFIED SIM)",
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = BharatGreenLight,
                                            letterSpacing = 0.5.sp
                                        )
                                    }

                                    TextButton(
                                        onClick = {
                                            authViewModel.refreshSimCards(context)
                                            Toast.makeText(context, "Scanning active network...", Toast.LENGTH_SHORT).show()
                                        },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Text("Scan SIM", fontSize = 11.5.sp, color = BharatElectricCyan, fontWeight = FontWeight.SemiBold)
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                if (detectedSims.isNotEmpty()) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        detectedSims.forEach { sim ->
                                            val isSelected = selectedSim?.slotIndex == sim.slotIndex
                                            Surface(
                                                shape = RoundedCornerShape(10.dp),
                                                color = if (isSelected) Color(0x3310B981) else Color(0xFF0F172A),
                                                border = BorderStroke(1.dp, if (isSelected) BharatGreenLight else Color(0xFF334155)),
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .clickable {
                                                        authViewModel.selectSim(sim)
                                                        Toast.makeText(context, "Selected ${sim.carrierName}", Toast.LENGTH_SHORT).show()
                                                    }
                                            ) {
                                                Column(modifier = Modifier.padding(8.dp)) {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Text(
                                                            text = "SIM ${sim.slotIndex + 1}: ${sim.carrierName}",
                                                            fontSize = 12.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = if (isSelected) BharatGreenLight else BharatWhite
                                                        )
                                                    }
                                                    if (!sim.phoneNumber.isNullOrBlank()) {
                                                        Text(
                                                            text = "+91 ${sim.phoneNumber}",
                                                            fontSize = 11.sp,
                                                            color = TextSecondaryDark
                                                        )
                                                    } else {
                                                        Text(
                                                            text = "✅ इस सिम से सक्रिय करें",
                                                            fontSize = 10.5.sp,
                                                            color = BharatGreenLight
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                } else {
                                    Text(
                                        text = "⚠️ केवल उसी नंबर से अकाउंट एक्टिवेट होगा जिसकी सिम इस फ़ोन में लगी है।",
                                        fontSize = 11.5.sp,
                                        color = TextSecondaryDark
                                    )
                                }
                            }
                        }
                    }

                    // Next Button (SIM Card Check Enforced)
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                val isValid = authViewModel.validateCurrentPhone(context)
                                if (isValid) {
                                    showConfirmNumberDialog = true
                                } else {
                                    val err = phoneValidationError ?: "कृपया इसी फ़ोन में लगे सिम कार्ड का नंबर दर्ज करें।"
                                    Toast.makeText(context, err, Toast.LENGTH_LONG).show()
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("auth_next_button"),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = BharatGreenLight)
                        ) {
                            Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = BharatWhite, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("वेरीफाई और आगे बढ़ें (Verify SIM)", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = BharatWhite)
                        }
                    }
                }
            }

            // STEP 2: WhatsApp OTP Verification Screen
            AuthStep.VERIFY_OTP -> {
                val fullE164 = authViewModel.getCleanE164PhoneNumber()
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "सिम और नंबर सत्यापन",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = BharatWhite,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "इस मोबाइल में लगे सिम (${selectedCountry.flag} $fullE164) पर 6-अंकों का SMS कोड भेजा गया है।",
                            fontSize = 13.5.sp,
                            color = TextSecondaryDark,
                            textAlign = TextAlign.Center,
                            lineHeight = 18.sp
                        )

                        TextButton(
                            onClick = { currentStep = AuthStep.ENTER_PHONE },
                            contentPadding = PaddingValues(0.dp),
                            modifier = Modifier.testTag("wrong_number_button")
                        ) {
                            Text("गलत नंबर? बदलें (Edit Number)", color = BharatGreenLight, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Status Info Pill
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = Color(0xFF1E293B),
                            border = BorderStroke(1.dp, Color(0xFF334155)),
                            modifier = Modifier.padding(bottom = 12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(12.dp),
                                    strokeWidth = 1.5.dp,
                                    color = BharatGreenLight
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "डिवाइस पर SMS ऑटो-डिटेक्शन सक्रिय है...",
                                    fontSize = 11.5.sp,
                                    color = BharatGreenLight,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // 6-digit WhatsApp PIN boxes
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            otpDigits.indices.forEach { index ->
                                Surface(
                                    modifier = Modifier
                                        .size(46.dp)
                                        .border(
                                            1.5.dp,
                                            if (otpDigits[index].isNotEmpty()) BharatGreenLight else Color(0xFF334155),
                                            RoundedCornerShape(12.dp)
                                        ),
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (otpDigits[index].isNotEmpty()) Color(0x2210B981) else Color(0xFF1E293B)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        TextField(
                                            value = otpDigits[index],
                                            onValueChange = { v ->
                                                val clean = v.filter { it.isDigit() }.take(1)
                                                authViewModel.updateOtpDigit(index, clean)
                                                if (clean.isNotEmpty() && index < 5) {
                                                    focusRequesters[index + 1].requestFocus()
                                                }
                                                // Auto verify if all 6 digits entered
                                                val currentList = otpDigits.toMutableList()
                                                currentList[index] = clean
                                                if (currentList.all { it.isNotEmpty() }) {
                                                    authViewModel.verifyEnteredOtp {
                                                        currentStep = AuthStep.PROFILE_SETUP
                                                    }
                                                }
                                            },
                                            singleLine = true,
                                            textStyle = androidx.compose.ui.text.TextStyle(
                                                fontSize = 20.sp,
                                                fontWeight = FontWeight.Bold,
                                                textAlign = TextAlign.Center,
                                                color = BharatWhite
                                            ),
                                            colors = TextFieldDefaults.colors(
                                                focusedContainerColor = Color.Transparent,
                                                unfocusedContainerColor = Color.Transparent,
                                                focusedIndicatorColor = Color.Transparent,
                                                unfocusedIndicatorColor = Color.Transparent
                                            ),
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            modifier = Modifier
                                                .size(46.dp)
                                                .focusRequester(focusRequesters[index])
                                                .testTag("otp_box_$index")
                                        )
                                    }
                                }
                            }
                        }

                        if (otpErrorMsg != null) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(text = otpErrorMsg ?: "", color = Color(0xFFEF4444), fontSize = 12.5.sp)
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Resend SMS and Call me options
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Sms, contentDescription = null, tint = TextSecondaryDark, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (resendTimer > 0) "Resend SMS in 0:${resendTimer.toString().padStart(2, '0')}" else "Resend SMS",
                                        fontSize = 13.sp,
                                        color = if (resendTimer > 0) TextMutedDark else BharatGreenLight,
                                        fontWeight = if (resendTimer == 0) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                                if (resendTimer == 0) {
                                    TextButton(
                                        onClick = {
                                            val activity = context.findActivity()
                                            authViewModel.startPhoneVerification(activity, isResend = true)
                                            Toast.makeText(context, "New OTP requested for $fullE164", Toast.LENGTH_SHORT).show()
                                        }
                                    ) {
                                        Text("RESEND", color = BharatGreenLight, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Phone, contentDescription = null, tint = TextSecondaryDark, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (callTimer > 0) "Call me in 0:${callTimer.toString().padStart(2, '0')}" else "Call me",
                                        fontSize = 13.sp,
                                        color = if (callTimer > 0) TextMutedDark else BharatGreenLight,
                                        fontWeight = if (callTimer == 0) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                                if (callTimer == 0) {
                                    TextButton(
                                        onClick = {
                                            Toast.makeText(context, "Voice call verification dispatched to $fullE164", Toast.LENGTH_SHORT).show()
                                        }
                                    ) {
                                        Text("CALL ME", color = BharatGreenLight, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }

                    // Verify Button
                    Button(
                        onClick = {
                            val entered = otpDigits.joinToString("")
                            if (entered.length < 6) {
                                Toast.makeText(context, "Please enter complete 6-digit OTP", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            authViewModel.verifyEnteredOtp {
                                currentStep = AuthStep.PROFILE_SETUP
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("auth_verify_button"),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = BharatGreenLight)
                    ) {
                        Text(
                            text = if (isVerifying) "Verifying..." else "Verify & Continue",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = BharatWhite
                        )
                    }
                }
            }

            // STEP 3: WhatsApp Profile Info Setup
            AuthStep.PROFILE_SETUP -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "Profile info",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = BharatWhite,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "Please provide your name and an optional profile photo",
                            fontSize = 13.sp,
                            color = TextSecondaryDark,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(32.dp))

                        // Profile Picture Circle with Camera Badge
                        Box(
                            modifier = Modifier
                                .size(110.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF1E293B))
                                .border(2.dp, BharatGreenLight, CircleShape)
                                .clickable { showAvatarOptionsSheet = true }
                                .testTag("auth_avatar_picker"),
                            contentAlignment = Alignment.Center
                        ) {
                            if (customDpUri != null) {
                                AsyncImage(
                                    model = customDpUri,
                                    contentDescription = "Profile DP",
                                    modifier = Modifier.fillMaxSize().clip(CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.CameraAlt,
                                    contentDescription = "Add Photo",
                                    tint = BharatGreenLight,
                                    modifier = Modifier.size(40.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(28.dp))

                        // Type your name text field
                        OutlinedTextField(
                            value = userNameInput,
                            onValueChange = { if (it.length <= 25) userNameInput = it },
                            placeholder = { Text("Type your name here", color = TextMutedDark) },
                            singleLine = true,
                            trailingIcon = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(end = 8.dp)
                                ) {
                                    Text(
                                        text = "${userNameInput.length}/25",
                                        fontSize = 11.sp,
                                        color = TextMutedDark
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        imageVector = Icons.Default.SentimentSatisfied,
                                        contentDescription = "Emoji",
                                        tint = TextSecondaryDark,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = BharatGreenLight,
                                unfocusedBorderColor = Color(0xFF334155),
                                focusedContainerColor = Color(0xFF1E293B),
                                unfocusedContainerColor = Color(0xFF1E293B),
                                focusedTextColor = BharatWhite,
                                unfocusedTextColor = BharatWhite
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("auth_name_input")
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Status / About field
                        OutlinedTextField(
                            value = userStatusInput,
                            onValueChange = { userStatusInput = it },
                            placeholder = { Text("Status / About", color = TextMutedDark) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = BharatGreenLight,
                                unfocusedBorderColor = Color(0xFF334155),
                                focusedContainerColor = Color(0xFF1E293B),
                                unfocusedContainerColor = Color(0xFF1E293B),
                                focusedTextColor = BharatWhite,
                                unfocusedTextColor = BharatWhite
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("auth_status_input")
                        )
                    }

                    // Finish Setup Button
                    Button(
                        onClick = {
                            val formattedPhone = "${selectedCountry.code} $phoneNumberInput"
                            val finalName = userNameInput.ifBlank { if (isNewAccountMode) "VenzoInd User" else "Account User" }
                            if (viewModel != null) {
                                viewModel.completeAuthLogin(
                                    name = finalName,
                                    phone = formattedPhone,
                                    statusBio = userStatusInput,
                                    photoUri = customDpUri
                                )
                            } else {
                                onLoginSuccess()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("auth_finish_button"),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = BharatGreenLight)
                    ) {
                        Text(
                            text = if (isNewAccountMode) "Create Account & Finish" else "Log In to VenzoInd",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = BharatWhite
                        )
                    }
                }
            }
        }

        // WhatsApp Confirmation Alert Dialog
        if (showConfirmNumberDialog) {
            val fullPhone = "${selectedCountry.code} $phoneNumberInput"
            AlertDialog(
                onDismissRequest = { showConfirmNumberDialog = false },
                title = {
                    Text(
                        text = "You entered the phone number:",
                        fontSize = 15.sp,
                        color = BharatWhite
                    )
                },
                text = {
                    Column {
                        Text(
                            text = fullPhone,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = BharatGreenLight
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Is this OK, or would you like to edit the number?",
                            fontSize = 13.5.sp,
                            color = TextSecondaryDark
                        )
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showConfirmNumberDialog = false
                            currentStep = AuthStep.VERIFY_OTP
                        },
                        modifier = Modifier.testTag("confirm_number_ok_button")
                    ) {
                        Text("OK", color = BharatGreenLight, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { showConfirmNumberDialog = false },
                        modifier = Modifier.testTag("confirm_number_edit_button")
                    ) {
                        Text("EDIT", color = TextSecondaryDark)
                    }
                },
                containerColor = Color(0xFF1E293B)
            )
        }

        // Searchable Country Picker Bottom Sheet
        if (showCountryPickerSheet) {
            ModalBottomSheet(
                onDismissRequest = { showCountryPickerSheet = false },
                containerColor = Color(0xFF0F172A),
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "Choose a country",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = BharatWhite,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )

                    // Country search bar
                    OutlinedTextField(
                        value = countrySearchQuery,
                        onValueChange = { countrySearchQuery = it },
                        placeholder = { Text("Search country name or code", color = TextMutedDark) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextSecondaryDark) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = BharatGreenLight,
                            unfocusedBorderColor = Color(0xFF334155),
                            focusedContainerColor = Color(0xFF1E293B),
                            unfocusedContainerColor = Color(0xFF1E293B),
                            focusedTextColor = BharatWhite,
                            unfocusedTextColor = BharatWhite
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                            .testTag("country_search_input")
                    )

                    val filteredCountries = INTERNATIONAL_COUNTRIES.filter {
                        it.name.contains(countrySearchQuery, ignoreCase = true) ||
                                it.code.contains(countrySearchQuery) ||
                                it.iso.contains(countrySearchQuery, ignoreCase = true)
                    }

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 420.dp)
                    ) {
                        items(filteredCountries.size) { index ->
                            val country = filteredCountries[index]
                            val isSelected = country.name == selectedCountry.name

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        authViewModel.selectCountry(country)
                                        showCountryPickerSheet = false
                                    }
                                    .padding(vertical = 12.dp, horizontal = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Text(country.flag, fontSize = 22.sp)
                                    Text(
                                        text = country.name,
                                        fontSize = 15.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) BharatGreenLight else BharatWhite
                                    )
                                }
                                Text(
                                    text = country.code,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) BharatGreenLight else TextSecondaryDark
                                )
                            }
                            HorizontalDivider(color = Color(0xFF1E293B))
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }

        // Avatar Photo Choice Bottom Sheet (Camera vs Gallery)
        if (showAvatarOptionsSheet) {
            ModalBottomSheet(
                onDismissRequest = { showAvatarOptionsSheet = false },
                containerColor = Color(0xFF0F172A),
                scrimColor = Color.Black.copy(alpha = 0.6f),
                dragHandle = {
                    Box(
                        modifier = Modifier
                            .padding(vertical = 10.dp)
                            .size(width = 36.dp, height = 4.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF334155))
                    )
                }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Profile Photo",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = BharatWhite
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        // Camera Button
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clickable {
                                    showAvatarOptionsSheet = false
                                    showCameraCaptureDialog = true
                                }
                                .padding(12.dp)
                                .testTag("auth_pick_camera_button")
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(60.dp)
                                    .clip(CircleShape)
                                    .background(BharatGreenLight.copy(alpha = 0.15f))
                                    .border(1.dp, BharatGreenLight, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PhotoCamera,
                                    contentDescription = "Camera",
                                    tint = BharatGreenLight,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Camera", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = BharatWhite)
                        }

                        // Gallery Button
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clickable {
                                    showAvatarOptionsSheet = false
                                    galleryLauncher.launch("image/*")
                                }
                                .padding(12.dp)
                                .testTag("auth_pick_gallery_button")
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(60.dp)
                                    .clip(CircleShape)
                                    .background(BharatElectricCyan.copy(alpha = 0.15f))
                                    .border(1.dp, BharatElectricCyan, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PhotoLibrary,
                                    contentDescription = "Gallery",
                                    tint = BharatElectricCyan,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Gallery", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = BharatWhite)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }

        // CameraX Profile Picture Capture Dialog
        if (showCameraCaptureDialog) {
            ProfileCameraCaptureDialog(
                onDismiss = { showCameraCaptureDialog = false },
                onPhotoConfirmed = { uri, _ ->
                    customDpUri = uri.toString()
                    showCameraCaptureDialog = false
                    Toast.makeText(context, "Profile photo captured", Toast.LENGTH_SHORT).show()
                }
            )
        }
    }
}

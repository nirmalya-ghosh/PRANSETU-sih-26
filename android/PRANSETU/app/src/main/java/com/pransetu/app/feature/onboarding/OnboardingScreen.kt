package com.pransetu.app.feature.onboarding

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pransetu.app.R
import com.pransetu.app.core.localization.LanguageOption

@Composable
fun OnboardingScreen(
    viewModel: OnboardingViewModel,
    onFinishOnboarding: () -> Unit,
    onSendOtpClick: (() -> Unit)? = null,
    onVerifyOtpClick: (() -> Unit)? = null,
    otpCode: String = "",
    onOtpCodeChange: ((String) -> Unit)? = null
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = androidx.compose.ui.platform.LocalContext.current
    val activity = remember(context) {
        var ctx = context
        while (ctx is android.content.ContextWrapper) {
            if (ctx is android.app.Activity) break
            ctx = ctx.baseContext
        }
        ctx as? android.app.Activity
    }
    var simpleStep by androidx.compose.runtime.saveable.rememberSaveable { mutableIntStateOf(1) } // 1: Language, 2: Permissions, 3: Citizen Name
    var localOtpCode by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf("") }
    val effectiveOtpCode = if (otpCode.isNotEmpty()) otpCode else localOtpCode

    // Permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        viewModel.handleIntent(OnboardingIntent.PermissionsResult(results))
        simpleStep = 3
    }

    androidx.activity.compose.BackHandler(enabled = simpleStep > 1) {
        simpleStep--
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize()) {
            if (simpleStep > 1) {
                androidx.compose.material3.IconButton(
                    onClick = { simpleStep-- },
                    modifier = Modifier
                        .padding(paddingValues)
                        .padding(8.dp)
                        .align(Alignment.TopStart)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 24.dp, vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
            // Step Progress Dots
            Row(
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp)
            ) {
                listOf(1, 2, 3).forEach { stepNum ->
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .size(if (simpleStep == stepNum) 12.dp else 8.dp)
                            .clip(CircleShape)
                            .background(
                                if (simpleStep == stepNum)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.surfaceVariant
                            )
                    )
                }
            }

            AnimatedContent(
                targetState = simpleStep,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                modifier = Modifier.weight(1f)
            ) { targetStep ->
                when (targetStep) {
                    1 -> {
                        // ==========================================
                        // STEP 1: CHOOSE LANGUAGE (80DP LARGE CARDS)
                        // ==========================================
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(vertical = 16.dp),
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "Choose Your Language",
                                fontSize = 26.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = "ଭାଷା ବାଛନ୍ତୁ • भाषा चुनें",
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 4.dp, bottom = 24.dp)
                            )

                            LanguageSelectionCard(
                                code = "en",
                                title = "English",
                                subtitle = "PRANSETU keeps you safe during disasters",
                                isSelected = uiState.selectedLanguage == "en",
                                onSelect = {
                                    viewModel.handleIntent(OnboardingIntent.SelectLanguage("en"))
                                }
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            LanguageSelectionCard(
                                code = "or",
                                title = "ଓଡ଼ିଆ (Odia)",
                                subtitle = "ପ୍ରାଣସେତୁ ବିପର୍ଯ୍ୟୟ ସମୟରେ ଆପଣଙ୍କୁ ସୁରକ୍ଷିତ ରଖେ",
                                isSelected = uiState.selectedLanguage == "or",
                                onSelect = {
                                    viewModel.handleIntent(OnboardingIntent.SelectLanguage("or"))
                                }
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            LanguageSelectionCard(
                                code = "hi",
                                title = "हिन्दी (Hindi)",
                                subtitle = "प्राणसेतु आपदा के समय आपको सुरक्षित रखता है",
                                isSelected = uiState.selectedLanguage == "hi",
                                onSelect = {
                                    viewModel.handleIntent(OnboardingIntent.SelectLanguage("hi"))
                                }
                            )
                        }
                    }
                    2 -> {
                        // ==========================================
                        // STEP 2: EMERGENCY RESCUE PERMISSIONS
                        // ==========================================
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(vertical = 16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(96.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Security,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(52.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(24.dp))

                            Text(
                                text = when (uiState.selectedLanguage) {
                                    "or" -> "ଜରୁରୀକାଳୀନ ଅନୁମତି"
                                    "hi" -> "आपातकालीन अनुमतियां"
                                    else -> "Emergency Permissions"
                                },
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = when (uiState.selectedLanguage) {
                                    "or" -> "ମୋବାଇଲ୍ ନେଟୱାର୍କ ବିଚ୍ଛିନ୍ନ ହେଲେ ମଧ୍ୟ PRANSETU ନିକଟସ୍ଥ ଫୋନ୍ ଏବଂ ଜିପିଏସ୍ ମାଧ୍ୟମରେ ସାହାଯ୍ୟ ସଙ୍କେତ ପଠାଇବା ପାଇଁ ଅନୁମତି ଆବଶ୍ୟକ କରେ।"
                                    "hi" -> "मोबाइल नेटवर्क बंद होने पर भी PRANSETU नजदीकी फोन और जीपीएस के जरिए मदद भेजने के लिए अनुमति का उपयोग करता है。"
                                    else -> "PRANSETU requires location and nearby Bluetooth access to broadcast rescue signals to emergency responders even without internet."
                                },
                                fontSize = 16.sp,
                                lineHeight = 24.sp,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            
                        }
                    }
                    3 -> {
                        // ==========================================
                        // STEP 3: CITIZEN NAME & START
                        // ==========================================
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(vertical = 16.dp),
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = when (uiState.selectedLanguage) {
                                    "or" -> "ଉଦ୍ଧାରକାରୀ ଆପଣଙ୍କୁ କ’ଣ ବୋଲି ଡାକିବେ?"
                                    "hi" -> "बचावकर्मियों के लिए आपका नाम?"
                                    else -> "What should rescuers call you?"
                                },
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = when (uiState.selectedLanguage) {
                                    "or" -> "ଆପଣଙ୍କ ନାମ ଏବଂ ଫୋନ୍ ନମ୍ବର SOS ସଙ୍କେତ ସହିତ ପଠାଯିବ (ବାଧ୍ୟତାମୂଳକ)"
                                    "hi" -> "आपका नाम और फोन नंबर SOS संदेश के साथ भेजा जाएगा (अनिवार्य)"
                                    else -> "Your name and mobile number will be attached to your rescue signals (Required)"
                                },
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(24.dp))

                            OutlinedTextField(
                                value = uiState.userName,
                                onValueChange = { viewModel.handleIntent(OnboardingIntent.UpdateName(it)) },
                                placeholder = {
                                    Text("Full Name (e.g. Ramesh Chandra)", fontSize = 16.sp)
                                },
                                textStyle = MaterialTheme.typography.titleMedium,
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(60.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                                )
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            OutlinedTextField(
                                value = uiState.userPhone,
                                onValueChange = { viewModel.handleIntent(OnboardingIntent.UpdatePhone(it)) },
                                placeholder = {
                                    Text("Mobile Number (e.g. +91 9876543210)", fontSize = 16.sp)
                                },
                                textStyle = MaterialTheme.typography.titleMedium,
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(60.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                                )
                            )

                            if (uiState.otpSent && !uiState.otpVerified) {
                                Spacer(modifier = Modifier.height(16.dp))
                                OutlinedTextField(
                                    value = effectiveOtpCode,
                                    onValueChange = {
                                        localOtpCode = it
                                        onOtpCodeChange?.invoke(it)
                                    },
                                    placeholder = {
                                        Text("Enter 6-digit OTP", fontSize = 16.sp)
                                    },
                                    textStyle = MaterialTheme.typography.titleMedium,
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(60.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                                    )
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    TextButton(
                                        onClick = { viewModel.handleIntent(OnboardingIntent.EditPhone) }
                                    ) {
                                        Text(
                                            text = "✏️ Change Phone",
                                            color = MaterialTheme.colorScheme.primary,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                    TextButton(
                                        onClick = { viewModel.handleIntent(OnboardingIntent.ResendOtp(activity)) }
                                    ) {
                                        Text(
                                            text = "🔄 Resend OTP",
                                            color = MaterialTheme.colorScheme.primary,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }

                            if (uiState.otpVerified) {
                                Spacer(modifier = Modifier.height(12.dp))
                                Surface(
                                    color = Color(0xFFE8F5E9),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "✅ Phone Number Verified (Firebase Auth)",
                                            color = Color(0xFF2E7D32),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        )
                                    }
                                }
                            }

                            if (uiState.otpMessage != null && uiState.otpError == null && !uiState.otpVerified) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = uiState.otpMessage ?: "",
                                    color = MaterialTheme.colorScheme.primary,
                                    fontSize = 14.sp
                                )
                            }
                            
                            if (uiState.otpError != null) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = uiState.otpError ?: "",
                                    color = MaterialTheme.colorScheme.error,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                }
            }

            // Bottom Navigation Buttons (56dp height)
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                when (simpleStep) {
                    1 -> {
                        Button(
                            onClick = { simpleStep = 2 },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                        ) {
                            Text(
                                text = when (uiState.selectedLanguage) {
                                    "or" -> "ପରବର୍ତ୍ତୀ (Next)"
                                    "hi" -> "आगे बढ़ें (Next)"
                                    else -> "Next Step"
                                },
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    2 -> {
                        Button(
                            onClick = {
                                val perms = mutableListOf(
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                )
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                    perms.add(Manifest.permission.BLUETOOTH_ADVERTISE)
                                    perms.add(Manifest.permission.BLUETOOTH_CONNECT)
                                    perms.add(Manifest.permission.BLUETOOTH_SCAN)
                                }
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    perms.add(Manifest.permission.POST_NOTIFICATIONS)
                                    perms.add(Manifest.permission.NEARBY_WIFI_DEVICES)
                                }
                                permissionLauncher.launch(perms.toTypedArray())
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                        ) {
                            Text(
                                text = when (uiState.selectedLanguage) {
                                    "or" -> "ଅନୁମତି ଦିଅନ୍ତୁ"
                                    "hi" -> "अनुमति दें"
                                    else -> "Allow Permissions"
                                },
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        OutlinedButton(
                            onClick = { simpleStep = 3 },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                        ) {
                            Text(
                                text = when (uiState.selectedLanguage) {
                                    "or" -> "ପରେ ଦେବି (Skip)"
                                    "hi" -> "बाद में (Skip)"
                                    else -> "Skip for Now"
                                },
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    3 -> {
                        if (uiState.isLoading) {
                            androidx.compose.material3.CircularProgressIndicator(
                                modifier = Modifier.align(Alignment.CenterHorizontally)
                            )
                        } else if (!uiState.otpSent) {
                            Button(
                                onClick = {
                                    if (onSendOtpClick != null) {
                                        onSendOtpClick()
                                    } else {
                                        viewModel.handleIntent(OnboardingIntent.SendOtp(activity))
                                    }
                                },
                                enabled = uiState.userName.isNotBlank() && uiState.userPhone.isNotBlank(),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp)
                            ) {
                                Text(
                                    text = "Send OTP via Firebase",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        } else if (!uiState.otpVerified) {
                            Button(
                                onClick = {
                                    if (onVerifyOtpClick != null) {
                                        onVerifyOtpClick()
                                    } else {
                                        viewModel.handleIntent(OnboardingIntent.VerifyOtp(effectiveOtpCode))
                                    }
                                },
                                enabled = effectiveOtpCode.isNotBlank(),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp)
                            ) {
                                Text(
                                    text = "Verify OTP",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        } else {
                            // Watch for registrationComplete flag — only navigate AFTER Supabase upload finishes
                            androidx.compose.runtime.LaunchedEffect(uiState.registrationComplete) {
                                if (uiState.registrationComplete) {
                                    onFinishOnboarding()
                                }
                            }

                            if (uiState.isLoading) {
                                // Show loading while Supabase registration is in progress
                                Button(
                                    onClick = {},
                                    enabled = false,
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF2E7D32),
                                        disabledContainerColor = Color(0xFF2E7D32).copy(alpha = 0.6f)
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(56.dp)
                                ) {
                                    androidx.compose.material3.CircularProgressIndicator(
                                        modifier = Modifier.size(24.dp),
                                        color = Color.White,
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = "Registering...",
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            } else {
                                Button(
                                    onClick = {
                                        viewModel.handleIntent(OnboardingIntent.FinishOnboarding)
                                    },
                                    enabled = true,
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF2E7D32),
                                        disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(56.dp)
                                ) {
                                    Text(
                                        text = when (uiState.selectedLanguage) {
                                            "or" -> "ପ୍ରାଣସେତୁ ଆରମ୍ଭ କରନ୍ତୁ"
                                            "hi" -> "प्राणसेतु शुरू करें"
                                            else -> "Start Using PRANSETU"
                                        },
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
                }
            }
            }
        }
    }
}

@Composable
private fun LanguageSelectionCard(
    code: String,
    title: String,
    subtitle: String,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    Card(
        onClick = onSelect,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
            else
                MaterialTheme.colorScheme.surface
        ),
        border = if (isSelected)
            androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        else
            androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier
            .fillMaxWidth()
            .height(84.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (isSelected) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

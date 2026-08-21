package com.pransetu.app.feature.firstaid

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Dangerous
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WaterDamage
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pransetu.app.core.ui.components.PransetuTopAppBar

data class FirstAidTopic(
    val id: String,
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val badgeColor: Color,
    val steps: List<String>,
    val dos: List<String>,
    val donts: List<String>,
    val hasCprMetronome: Boolean = false
)

private val FIRST_AID_TOPICS = listOf(
    FirstAidTopic(
        id = "cpr",
        title = "CPR (Cardiopulmonary Resuscitation)",
        subtitle = "For unresponsive victims not breathing normally",
        icon = Icons.Default.Favorite,
        badgeColor = Color(0xFFE53935),
        hasCprMetronome = true,
        steps = listOf(
            "1. Check responsiveness — Tap shoulders firmly and shout 'Are you okay?'.",
            "2. Check breathing — Look for chest rise for max 5-10 seconds.",
            "3. Call for help — Direct someone to call 108 immediately.",
            "4. Hand placement — Heel of one hand in the center of the chest (lower half of sternum), interlock second hand fingers.",
            "5. Hard & Fast Compressions — Push down 2 inches (5-6 cm) at 100-120 beats per minute.",
            "6. 30:2 Cycle — Give 30 compressions followed by 2 gentle rescue breaths (pinch nose, seal mouth). If untrained, continue continuous chest compressions."
        ),
        dos = listOf(
            "Allow chest to fully recoil between each compression",
            "Keep your arms locked straight and use your upper body weight",
            "Switch rescuers every 2 minutes if tired"
        ),
        donts = listOf(
            "Do NOT stop compressions for more than 10 seconds",
            "Do NOT lean on the chest between compressions"
        )
    ),
    FirstAidTopic(
        id = "bleeding",
        title = "Severe Bleeding & Hemorrhage",
        subtitle = "Stop life-threatening blood loss immediately",
        icon = Icons.Default.MedicalServices,
        badgeColor = Color(0xFFC2185B),
        steps = listOf(
            "1. Apply DIRECT PRESSURE immediately on the wound with clean cloth, gauze, or bare hands if nothing else is available.",
            "2. Maintain firm, continuous pressure for at least 5-10 minutes without lifting to check.",
            "3. Pack the wound — If bleeding continues from a deep cavity, pack tightly with clean cloth and press.",
            "4. Elevate the injured limb above heart level if no fractures are suspected.",
            "5. Apply a pressure bandage over the pad.",
            "6. Improvised Tourniquet (Limb life-threat only) — Tie a cloth 2-3 inches above wound, insert a stick, and twist until bleeding stops. Note time applied!"
        ),
        dos = listOf(
            "Press hard directly on the exact bleeding point",
            "Add more dressings on top if blood soaks through (do not remove base layer)"
        ),
        donts = listOf(
            "Do NOT remove deeply impaled objects (stabilize around them)",
            "Do NOT release pressure to check the wound"
        )
    ),
    FirstAidTopic(
        id = "snakebite",
        title = "Snake Bite Protocol (Odisha)",
        subtitle = "Anti-Venom guidelines for Krait, Viper, Cobra bites",
        icon = Icons.Default.Warning,
        badgeColor = Color(0xFFE65100),
        steps = listOf(
            "1. Keep victim COMPLETELY STILL and calm — movement speeds venom spread through lymphatic system.",
            "2. Immobilize the bitten limb with a splint at or slightly below heart level.",
            "3. Remove rings, watches, bracelets, and tight clothing before swelling begins.",
            "4. Clean bite area gently with water and cover with a sterile dry dressing.",
            "5. Transport immediately to the nearest hospital with Anti-Snake Venom (ASV).",
            "6. Note snake appearance or photo from safe distance if possible."
        ),
        dos = listOf(
            "Reassure the victim (most snakebites are non-lethal if treated with ASV)",
            "Keep patient lying flat on side (recovery position) during transport",
            "Note exact time of bite and onset of symptoms"
        ),
        donts = listOf(
            "Do NOT cut or incise the bite wound",
            "Do NOT attempt to suck venom with mouth or suction device",
            "Do NOT apply tight arterial tourniquets (causes necrosis)",
            "Do NOT apply ice, potassium permanganate, or herbal pastes"
        )
    ),
    FirstAidTopic(
        id = "drowning",
        title = "Flood & Water Submersion Rescue",
        subtitle = "Post-drowning and water inhalation care",
        icon = Icons.Default.WaterDamage,
        badgeColor = Color(0xFF0288D1),
        steps = listOf(
            "1. Ensure rescuer safety — throw flotation device, do not enter turbulent floodwaters without rope/lifejacket.",
            "2. Remove victim from water and place on firm flat surface.",
            "3. If not breathing, start CPR IMMEDIATELY (begin with 5 rescue breaths for drowning, then 30:2 ratio).",
            "4. If breathing, place in RECOVERY POSITION on their side to prevent choking on aspirated water.",
            "5. Remove wet clothes and cover with dry blankets/tarps to prevent fatal hypothermia.",
            "6. Keep airway clear and monitor breathing continuously."
        ),
        dos = listOf(
            "Start rescue breaths immediately for drowning victims",
            "Wrap victim in dry clothing/foil blankets to retain heat"
        ),
        donts = listOf(
            "Do NOT attempt the Heimlich maneuver to drain water from lungs (causes vomiting)",
            "Do NOT leave a rescued victim unattended (secondary drowning risk)"
        )
    ),
    FirstAidTopic(
        id = "burns",
        title = "Burns & Electrical Hazards",
        subtitle = "Thermal, chemical, and electrical shock management",
        icon = Icons.Default.LocalHospital,
        badgeColor = Color(0xFF689F38),
        steps = listOf(
            "1. Stop the burning process — remove heat source, isolate power breaker before touching electrical victims.",
            "2. Cool the burn under gentle running cool water for 10-20 minutes. (Do NOT use ice water).",
            "3. Remove jewelry, belts, and constrictive items around burn area before swelling.",
            "4. Cover loosely with clean, non-stick sterile plastic cling film or clean dry cloth.",
            "5. Keep victim warm with a blanket to prevent shock.",
            "6. For chemical burns, flush with copious water for at least 20 minutes."
        ),
        dos = listOf(
            "Use clean cool water immediately to stop thermal progression",
            "Protect burn blisters from popping to prevent infection"
        ),
        donts = listOf(
            "Do NOT apply butter, toothpaste, turmeric, oil, or flour",
            "Do NOT pull off clothing that is stuck to melted skin",
            "Do NOT touch high-voltage wire victims without professional shutoff"
        )
    ),
    FirstAidTopic(
        id = "fractures",
        title = "Bone Fractures & Improvised Splints",
        subtitle = "Stabilize broken bones and joint dislocations",
        icon = Icons.Default.MedicalServices,
        badgeColor = Color(0xFF5D4037),
        steps = listOf(
            "1. Control any external bleeding with gentle pressure around wound edges.",
            "2. Do NOT try to straighten or push bone fragments back into place.",
            "3. Improvise a splint using rigid materials (sticks, rolled newspapers, cardboard, umbrellas).",
            "4. Pad the splint with cloth to cushion against the limb.",
            "5. Secure splint above and below the fractured joint with cloth ties.",
            "6. Check circulation (pulse/finger warmth) every 15 minutes."
        ),
        dos = listOf(
            "Immobilize the joint above and below the broken bone",
            "Apply ice packs wrapped in cloth to reduce swelling"
        ),
        donts = listOf(
            "Do NOT move an injured neck or spine unless in immediate danger",
            "Do NOT tie splint bandages directly over the fracture site"
        )
    )
)

@Composable
fun FirstAidScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    var expandedTopicId by remember { mutableStateOf<String?>("cpr") }

    Scaffold(
        topBar = {
            PransetuTopAppBar(
                title = com.pransetu.app.core.localization.tr("Offline First Aid Guide"),
                canNavigateBack = true,
                navigateUp = onNavigateBack
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(4.dp))
                // Quick Call 108 Emergency Banner
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFB71C1C))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Emergency Medical Helpline",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "Dial 108 (Ambulance) / 112 (National Disaster)",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.9f)
                            )
                        }
                        Button(
                            onClick = {
                                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:108"))
                                context.startActivity(intent)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White)
                        ) {
                            Text("CALL 108", color = Color(0xFFB71C1C), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            items(FIRST_AID_TOPICS) { topic ->
                val isExpanded = expandedTopicId == topic.id
                FirstAidTopicCard(
                    topic = topic,
                    isExpanded = isExpanded,
                    onToggle = {
                        expandedTopicId = if (isExpanded) null else topic.id
                    }
                )
            }

            item { Spacer(modifier = Modifier.height(32.dp)) }
        }
    }
}

@Composable
private fun FirstAidTopicCard(
    topic: FirstAidTopic,
    isExpanded: Boolean,
    onToggle: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(topic.badgeColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = topic.icon,
                        contentDescription = null,
                        tint = topic.badgeColor,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = topic.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = topic.subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            AnimatedVisibility(visible = isExpanded) {
                Column(modifier = Modifier.padding(top = 16.dp)) {
                    // CPR Metronome Tool
                    if (topic.hasCprMetronome) {
                        CprMetronomeWidget()
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    // Steps
                    Text(
                        text = "Immediate Action Protocol:",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    topic.steps.forEach { step ->
                        Text(
                            text = step,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 3.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // DOs
                    Surface(
                        color = Color(0xFF10B981).copy(alpha = 0.1f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("CRITICAL DO'S", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = Color(0xFF10B981))
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            topic.dos.forEach { doItem ->
                                Text("• $doItem", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // DON'Ts
                    Surface(
                        color = Color(0xFFD32F2F).copy(alpha = 0.1f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Dangerous, contentDescription = null, tint = Color(0xFFD32F2F), modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("CRITICAL DONT'S (FATAL MISTAKES)", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = Color(0xFFD32F2F))
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            topic.donts.forEach { dontItem ->
                                Text("• $dontItem", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CprMetronomeWidget() {
    var isRunning by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val infiniteTransition = rememberInfiniteTransition(label = "CprPulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(270), // ~110 BPM (545ms per cycle)
            repeatMode = RepeatMode.Reverse
        ),
        label = "PulseScale"
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF263238))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "CPR Rhythm Metronome (110 BPM)",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = "Match your chest compressions to the flashing rhythm",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.8f)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(80.dp)
                    .graphicsLayer(
                        scaleX = if (isRunning) pulseScale else 1f,
                        scaleY = if (isRunning) pulseScale else 1f
                    )
                    .background(if (isRunning) Color(0xFFE53935) else Color.Gray, CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Favorite,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(40.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { isRunning = !isRunning },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isRunning) Color(0xFFD32F2F) else Color(0xFF10B981)
                )
            ) {
                Icon(imageVector = if (isRunning) Icons.Default.Stop else Icons.Default.PlayArrow, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (isRunning) "STOP METRONOME" else "START CPR METRONOME", fontWeight = FontWeight.Bold)
            }
        }
    }
}

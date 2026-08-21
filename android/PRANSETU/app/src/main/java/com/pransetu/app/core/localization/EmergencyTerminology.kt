package com.pransetu.app.core.localization

import android.content.Context
import com.pransetu.app.R

/**
 * Deterministic enum→localized-string mapping for emergency concepts.
 * 
 * The backend stores language-independent codes:
 *   event_type = FLOOD
 *   severity = CRITICAL
 *   action = EVACUATE
 * 
 * This class resolves them to the user's selected language:
 *   Odia → ବନ୍ୟା / ଗୁରୁତର / ସ୍ଥାନାନ୍ତର ହୁଅନ୍ତୁ
 *   Hindi → बाढ़ / गंभीर / सुरक्षित स्थान पर जाएँ
 *   English → Flood / Critical / Evacuate
 */
object EmergencyTerminology {

    fun getEmergencyTypeName(context: Context, typeCode: String): String {
        return when (typeCode.uppercase()) {
            "CYCLONE" -> context.getString(R.string.emergency_cyclone)
            "FLOOD" -> context.getString(R.string.emergency_flood)
            "EARTHQUAKE" -> context.getString(R.string.emergency_earthquake)
            "TSUNAMI" -> context.getString(R.string.emergency_tsunami)
            "LANDSLIDE" -> context.getString(R.string.emergency_landslide)
            "LIGHTNING" -> context.getString(R.string.emergency_lightning)
            "HEATWAVE" -> context.getString(R.string.emergency_heatwave)
            "WILDFIRE" -> context.getString(R.string.emergency_wildfire)
            "FIRE" -> context.getString(R.string.emergency_fire)
            "STORM" -> context.getString(R.string.emergency_storm)
            "INDUSTRIAL" -> context.getString(R.string.emergency_industrial)
            "BIOLOGICAL" -> context.getString(R.string.emergency_biological)
            "OTHER" -> context.getString(R.string.emergency_other)
            else -> context.getString(R.string.emergency_unknown)
        }
    }

    fun getSeverityName(context: Context, severityCode: String): String {
        return when (severityCode.uppercase()) {
            "CRITICAL" -> context.getString(R.string.severity_critical)
            "HIGH" -> context.getString(R.string.severity_high)
            "MEDIUM" -> context.getString(R.string.severity_medium)
            "LOW" -> context.getString(R.string.severity_low)
            else -> severityCode
        }
    }

    fun getActionName(context: Context, actionCode: String): String {
        return when (actionCode.uppercase()) {
            "EVACUATE" -> context.getString(R.string.action_evacuate)
            "SHELTER" -> context.getString(R.string.action_shelter)
            "MONITOR" -> context.getString(R.string.action_monitor)
            "PREPARE" -> context.getString(R.string.action_prepare)
            "AVOID_AREA" -> context.getString(R.string.action_avoid_area)
            else -> actionCode
        }
    }

    fun getAlertSeverityName(context: Context, severity: String): String {
        return when (severity.uppercase()) {
            "INFO" -> context.getString(R.string.alerts_severity_info)
            "ADVISORY" -> context.getString(R.string.alerts_severity_advisory)
            "WARNING" -> context.getString(R.string.alerts_severity_warning)
            "CRITICAL" -> context.getString(R.string.alerts_severity_critical)
            else -> severity
        }
    }

    fun getDeliveryStateName(context: Context, stateCode: String): String {
        return when (stateCode.uppercase()) {
            "READY" -> context.getString(R.string.delivery_ready)
            "HOLDING" -> context.getString(R.string.delivery_holding)
            "SOS_CREATED" -> context.getString(R.string.delivery_sos_created)
            "SAVED_LOCALLY" -> context.getString(R.string.delivery_saved_locally)
            "SEARCHING_FOR_RELAY" -> context.getString(R.string.delivery_searching_relay)
            "RELAYING" -> context.getString(R.string.delivery_relaying)
            "GATEWAY_REACHED" -> context.getString(R.string.delivery_gateway_reached)
            "BACKEND_RECEIVED" -> context.getString(R.string.delivery_backend_received)
            "OPERATOR_ACKNOWLEDGED" -> context.getString(R.string.delivery_operator_acknowledged)
            "FAILED_RETRYING" -> context.getString(R.string.delivery_failed_retrying)
            "EXPIRED" -> context.getString(R.string.delivery_expired)
            else -> stateCode
        }
    }
}

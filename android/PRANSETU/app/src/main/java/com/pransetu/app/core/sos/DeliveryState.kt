package com.pransetu.app.core.sos

/**
 * Canonical SOS delivery lifecycle states.
 * 
 * CREATED → STORED → QUEUED → RELAYING → GATEWAY_RECEIVED → SERVER_RECEIVED → ACKNOWLEDGED → CLOSED
 * 
 * Failure and retry states are explicit:
 *   FAILED_RETRYING, EXPIRED
 */
enum class DeliveryState {
    /** SOS has been created in memory */
    CREATED,
    /** SOS has been persisted to Room (local database) */
    STORED,
    /** SOS is queued for transmission */
    QUEUED,
    /** SOS is being relayed through mesh peers */
    RELAYING,
    /** SOS has reached an internet-capable gateway device */
    GATEWAY_RECEIVED,
    /** SOS has been received by the PRANSETU backend */
    SERVER_RECEIVED,
    /** An operator has acknowledged the SOS */
    ACKNOWLEDGED,
    /** SOS lifecycle is complete */
    CLOSED,
    /** Delivery failed, automatic retry in progress */
    FAILED_RETRYING,
    /** All retry attempts exhausted or TTL expired */
    EXPIRED
}

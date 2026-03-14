package com.nitrous.docanalyzer.network

object NetworkConfig {
    const val CONNECT_TIMEOUT_SECONDS = 120L
    const val READ_TIMEOUT_SECONDS = 120L
    const val WRITE_TIMEOUT_SECONDS = 120L
    
    const val MAX_POLL_ATTEMPTS = 80
    const val POLL_DELAY_MILLIS = 1500L
    
    const val RETRY_AFTER_DEFAULT_SECONDS = 60
}

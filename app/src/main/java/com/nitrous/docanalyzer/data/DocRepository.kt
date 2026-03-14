package com.nitrous.docanalyzer.data

import com.nitrous.docanalyzer.network.*
import com.nitrous.docanalyzer.network.dto.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody

class DocRepository(private val apiService: ApiService) : BaseRepository() {

    // Removed duplicated/redundant file. Content is now correctly in repository/DocRepository.kt
}

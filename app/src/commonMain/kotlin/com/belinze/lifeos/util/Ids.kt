package com.belinze.lifeos.util

import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/** Random entity id in canonical UUID form (multiplatform replacement for `UUID.randomUUID().toString()`). */
@OptIn(ExperimentalUuidApi::class)
fun newId(): String = Uuid.random().toString()

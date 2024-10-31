package com.nkee.crimeintent.data

import android.net.Uri
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.contract.ActivityResultContracts

class ContactPicker (
    activityResultRegistry: ActivityResultRegistry,
    callback: (contactUri: Uri?) -> Unit
) {
    private val getContact = activityResultRegistry.register(REGISTRY_KEY, ActivityResultContracts.PickContact(), callback)

    fun pickContact() {
        getContact.launch(null)
    }

    companion object {
        private const val REGISTRY_KEY = "ContactPicker"
    }
}
package com.example.autofill

import android.app.assist.AssistStructure
import android.os.Build
import android.os.CancellationSignal
import android.service.autofill.AutofillService
import android.service.autofill.Dataset
import android.service.autofill.FillCallback
import android.service.autofill.FillRequest
import android.service.autofill.FillResponse
import android.service.autofill.SaveCallback
import android.service.autofill.SaveRequest
import android.view.View
import android.view.autofill.AutofillId
import android.view.autofill.AutofillValue
import android.widget.RemoteViews
import androidx.annotation.RequiresApi
import com.example.R
import com.example.data.VaultDatabase
import com.example.data.VaultPreferences
import com.example.data.VaultRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@RequiresApi(Build.VERSION_CODES.O)
class VaultAutofillService : AutofillService() {

    private val serviceScope = CoroutineScope(Dispatchers.IO)

    override fun onFillRequest(
        request: FillRequest,
        cancellationSignal: CancellationSignal,
        callback: FillCallback
    ) {
        val structure = request.fillContexts.lastOrNull()?.structure
        if (structure == null) {
            callback.onSuccess(null)
            return
        }

        val usernameIds = mutableListOf<AutofillId>()
        val passwordIds = mutableListOf<AutofillId>()
        var detectedDomain = ""

        fun traverseNode(node: AssistStructure.ViewNode) {
            val hints = node.autofillHints
            val hintText = node.hint?.toString()?.lowercase() ?: ""
            val idEntry = node.idEntry?.lowercase() ?: ""
            val domain = node.webDomain

            if (!domain.isNullOrBlank()) {
                detectedDomain = domain
            }

            val isPassword = hints?.any { it.equals(View.AUTOFILL_HINT_PASSWORD, ignoreCase = true) } == true
                    || hintText.contains("password")
                    || idEntry.contains("password")
                    || idEntry.contains("pass")

            val isUsername = hints?.any {
                it.equals(View.AUTOFILL_HINT_USERNAME, ignoreCase = true) ||
                it.equals(View.AUTOFILL_HINT_EMAIL_ADDRESS, ignoreCase = true)
            } == true || hintText.contains("user") || hintText.contains("email")
                    || idEntry.contains("user") || idEntry.contains("email")

            node.autofillId?.let { autofillId ->
                if (isPassword) {
                    passwordIds.add(autofillId)
                } else if (isUsername) {
                    usernameIds.add(autofillId)
                }
            }

            for (i in 0 until node.childCount) {
                traverseNode(node.getChildAt(i))
            }
        }

        for (i in 0 until structure.windowNodeCount) {
            val windowNode = structure.getWindowNodeAt(i)
            traverseNode(windowNode.rootViewNode)
        }

        if (usernameIds.isEmpty() && passwordIds.isEmpty()) {
            callback.onSuccess(null)
            return
        }

        serviceScope.launch {
            try {
                val db = VaultDatabase.getInstance(this@VaultAutofillService)
                val repository = VaultRepository(db.vaultItemDao(), VaultPreferences(this@VaultAutofillService))
                val allItems = db.vaultItemDao().getAllItemsSync()

                val matched = if (detectedDomain.isNotEmpty()) {
                    allItems.filter {
                        it.website.contains(detectedDomain, ignoreCase = true) ||
                        detectedDomain.contains(it.website, ignoreCase = true)
                    }.ifEmpty { allItems.take(4) }
                } else {
                    allItems.take(4)
                }

                if (matched.isEmpty()) {
                    callback.onSuccess(null)
                    return@launch
                }

                val responseBuilder = FillResponse.Builder()
                for (item in matched) {
                    val datasetBuilder = Dataset.Builder()
                    val presentation = RemoteViews(packageName, R.layout.autofill_dataset_item).apply {
                        setTextViewText(R.id.autofill_title, item.title)
                        setTextViewText(R.id.autofill_subtitle, item.username)
                    }

                    val plainPassword = repository.decryptPassword(item.encryptedPassword)

                    for (uId in usernameIds) {
                        datasetBuilder.setValue(uId, AutofillValue.forText(item.username), presentation)
                    }
                    for (pId in passwordIds) {
                        datasetBuilder.setValue(pId, AutofillValue.forText(plainPassword), presentation)
                    }
                    responseBuilder.addDataset(datasetBuilder.build())
                }

                callback.onSuccess(responseBuilder.build())
            } catch (e: Exception) {
                e.printStackTrace()
                callback.onSuccess(null)
            }
        }
    }

    override fun onSaveRequest(request: SaveRequest, callback: SaveCallback) {
        callback.onSuccess()
    }
}

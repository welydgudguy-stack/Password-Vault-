package com.example.ui

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.autofill.AutofillHelper
import com.example.data.VaultDatabase
import com.example.data.VaultItemEntity
import com.example.data.VaultPreferences
import com.example.data.VaultRepository
import com.example.security.CryptoManager
import com.example.security.PasswordStrength
import com.example.ui.components.BiometricResultState
import com.example.ui.components.VaultTab
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class AppScreen {
    SPLASH,
    ONBOARDING,
    CREATE_MASTER_PASSWORD,
    LOCKED,
    MAIN,
    ADD_ITEM,
    EDIT_ITEM
}

class VaultViewModel(application: Application) : AndroidViewModel(application) {

    private val preferences = VaultPreferences(application)
    private val database = VaultDatabase.getInstance(application)
    private val repository = VaultRepository(database.vaultItemDao(), preferences)

    // Current Screen
    private val _currentScreen = MutableStateFlow(AppScreen.SPLASH)
    val currentScreen: StateFlow<AppScreen> = _currentScreen.asStateFlow()

    // Selected Bottom Tab
    private val _selectedTab = MutableStateFlow(VaultTab.VAULT)
    val selectedTab: StateFlow<VaultTab> = _selectedTab.asStateFlow()

    // Search and Category
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow("All")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    // Vault Items Flow
    val vaultItems: StateFlow<List<VaultItemEntity>> = combine(
        repository.allItems,
        _searchQuery,
        _selectedCategory
    ) { items, query, category ->
        var list = items
        if (query.isNotBlank()) {
            list = list.filter {
                it.title.contains(query, ignoreCase = true) ||
                it.username.contains(query, ignoreCase = true) ||
                it.website.contains(query, ignoreCase = true)
            }
        }
        if (category == "Favorites") {
            list = list.filter { it.isFavorite }
        } else if (category != "All") {
            list = list.filter { it.category.equals(category, ignoreCase = true) }
        }
        list
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Onboarding slide index
    private val _onboardingIndex = MutableStateFlow(0)
    val onboardingIndex: StateFlow<Int> = _onboardingIndex.asStateFlow()

    // Master Password Setup
    private val _masterPasswordInput = MutableStateFlow("")
    val masterPasswordInput: StateFlow<String> = _masterPasswordInput.asStateFlow()

    private val _masterPasswordStrength = MutableStateFlow(CryptoManager.evaluateStrength(""))
    val masterPasswordStrength: StateFlow<PasswordStrength> = _masterPasswordStrength.asStateFlow()

    // Unlock Screen
    private val _unlockPasswordInput = MutableStateFlow("")
    val unlockPasswordInput: StateFlow<String> = _unlockPasswordInput.asStateFlow()

    private val _unlockErrorMessage = MutableStateFlow<String?>(null)
    val unlockErrorMessage: StateFlow<String?> = _unlockErrorMessage.asStateFlow()

    private val _biometricState = MutableStateFlow(BiometricResultState.IDLE)
    val biometricState: StateFlow<BiometricResultState> = _biometricState.asStateFlow()

    // Password Generator
    private val _generatorLength = MutableStateFlow(16)
    val generatorLength: StateFlow<Int> = _generatorLength.asStateFlow()

    private val _genUpper = MutableStateFlow(true)
    val genUpper: StateFlow<Boolean> = _genUpper.asStateFlow()

    private val _genLower = MutableStateFlow(true)
    val genLower: StateFlow<Boolean> = _genLower.asStateFlow()

    private val _genNumbers = MutableStateFlow(true)
    val genNumbers: StateFlow<Boolean> = _genNumbers.asStateFlow()

    private val _genSymbols = MutableStateFlow(true)
    val genSymbols: StateFlow<Boolean> = _genSymbols.asStateFlow()

    private val _generatedPassword = MutableStateFlow("")
    val generatedPassword: StateFlow<String> = _generatedPassword.asStateFlow()

    val generatorStrength: StateFlow<PasswordStrength> = combine(
        _generatedPassword
    ) { (pwd) ->
        CryptoManager.evaluateStrength(pwd)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), CryptoManager.evaluateStrength(""))

    // Viewing & Editing Item
    private val _activeDetailItem = MutableStateFlow<VaultItemEntity?>(null)
    val activeDetailItem: StateFlow<VaultItemEntity?> = _activeDetailItem.asStateFlow()

    private val _decryptedPassword = MutableStateFlow("")
    val decryptedPassword: StateFlow<String> = _decryptedPassword.asStateFlow()

    // Settings & Theme
    val themeMode: MutableStateFlow<String> = MutableStateFlow(preferences.themeMode)
    val isBiometricEnabled: MutableStateFlow<Boolean> = MutableStateFlow(preferences.isBiometricEnabled)
    val autoLockMinutes: MutableStateFlow<Int> = MutableStateFlow(preferences.autoLockMinutes)

    // User toast/snackbar feedback
    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    init {
        generateNewPassword()
        checkInitialDestination()
    }

    private fun checkInitialDestination() {
        viewModelScope.launch {
            delay(800) // Brief splash display
            if (!preferences.hasCompletedOnboarding) {
                _currentScreen.value = AppScreen.ONBOARDING
            } else if (!preferences.hasMasterPassword) {
                _currentScreen.value = AppScreen.CREATE_MASTER_PASSWORD
            } else {
                _currentScreen.value = AppScreen.LOCKED
            }
        }
    }

    fun completeOnboarding() {
        preferences.hasCompletedOnboarding = true
        _currentScreen.value = AppScreen.CREATE_MASTER_PASSWORD
    }

    fun nextOnboarding() {
        if (_onboardingIndex.value < 2) {
            _onboardingIndex.value += 1
        } else {
            completeOnboarding()
        }
    }

    fun updateMasterPasswordInput(input: String) {
        _masterPasswordInput.value = input
        _masterPasswordStrength.value = CryptoManager.evaluateStrength(input)
    }

    fun submitMasterPassword() {
        val pwd = _masterPasswordInput.value
        val strength = _masterPasswordStrength.value
        if (strength.hasMinLength && strength.hasUpper && strength.hasLower && strength.hasNumber && strength.hasSymbol) {
            repository.setupMasterPassword(pwd)
            viewModelScope.launch {
                repository.populateSampleDataIfEmpty()
                _toastMessage.value = "Master password created successfully!"
                _currentScreen.value = AppScreen.MAIN
            }
        }
    }

    fun triggerBiometricUnlock(forceSuccess: Boolean = true) {
        viewModelScope.launch {
            if (forceSuccess) {
                _biometricState.value = BiometricResultState.SUCCESS
                delay(800)
                repository.unlockWithBiometric()
                _biometricState.value = BiometricResultState.IDLE
                _currentScreen.value = AppScreen.MAIN
            } else {
                _biometricState.value = BiometricResultState.FAILURE
                delay(1200)
                _biometricState.value = BiometricResultState.IDLE
            }
        }
    }

    fun updateUnlockPasswordInput(input: String) {
        _unlockPasswordInput.value = input
        _unlockErrorMessage.value = null
    }

    fun submitUnlockPassword() {
        val input = _unlockPasswordInput.value
        val success = repository.unlockWithPassword(input)
        if (success) {
            _unlockPasswordInput.value = ""
            _unlockErrorMessage.value = null
            _currentScreen.value = AppScreen.MAIN
        } else {
            _unlockErrorMessage.value = "Incorrect master password. Please try again."
        }
    }

    fun lockVault() {
        repository.lock()
        _currentScreen.value = AppScreen.LOCKED
    }

    fun setTab(tab: VaultTab) {
        _selectedTab.value = tab
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSelectedCategory(cat: String) {
        _selectedCategory.value = cat
    }

    // Generator methods
    fun setGeneratorLength(length: Int) {
        _generatorLength.value = length
        generateNewPassword()
    }

    fun toggleUpper(checked: Boolean) {
        _genUpper.value = checked
        generateNewPassword()
    }

    fun toggleLower(checked: Boolean) {
        _genLower.value = checked
        generateNewPassword()
    }

    fun toggleNumbers(checked: Boolean) {
        _genNumbers.value = checked
        generateNewPassword()
    }

    fun toggleSymbols(checked: Boolean) {
        _genSymbols.value = checked
        generateNewPassword()
    }

    fun generateNewPassword() {
        val pwd = CryptoManager.generatePassword(
            length = _generatorLength.value,
            includeUpper = _genUpper.value,
            includeLower = _genLower.value,
            includeNumbers = _genNumbers.value,
            includeSymbols = _genSymbols.value
        )
        _generatedPassword.value = pwd
    }

    fun copyToClipboard(text: String, label: String = "VaultKeep") {
        val clipboard = getApplication<Application>().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(label, text)
        clipboard.setPrimaryClip(clip)
        _toastMessage.value = "Copied to clipboard!"
    }

    fun openItemDetail(item: VaultItemEntity) {
        _activeDetailItem.value = item
        _decryptedPassword.value = repository.decryptPassword(item.encryptedPassword)
    }

    fun closeItemDetail() {
        _activeDetailItem.value = null
        _decryptedPassword.value = ""
    }

    fun startAddItem() {
        _activeDetailItem.value = null
        _currentScreen.value = AppScreen.ADD_ITEM
    }

    fun startEditItem(item: VaultItemEntity) {
        _activeDetailItem.value = item
        _decryptedPassword.value = repository.decryptPassword(item.encryptedPassword)
        _currentScreen.value = AppScreen.EDIT_ITEM
    }

    fun saveItem(
        title: String,
        website: String,
        username: String,
        passwordPlain: String,
        notes: String,
        category: String
    ) {
        viewModelScope.launch {
            if (_currentScreen.value == AppScreen.ADD_ITEM) {
                repository.insertItem(
                    title = title.ifBlank { "Untitled" },
                    website = website,
                    username = username,
                    passwordPlain = passwordPlain,
                    notes = notes,
                    category = category
                )
                _toastMessage.value = "Login saved to Vault!"
            } else if (_currentScreen.value == AppScreen.EDIT_ITEM) {
                val current = _activeDetailItem.value
                if (current != null) {
                    val updated = current.copy(
                        title = title.ifBlank { "Untitled" },
                        website = website,
                        username = username,
                        encryptedPassword = repository.encryptPassword(passwordPlain),
                        notes = notes,
                        category = category
                    )
                    repository.updateItem(updated)
                    _toastMessage.value = "Login updated!"
                }
            }
            _currentScreen.value = AppScreen.MAIN
        }
    }

    fun deleteCurrentItem() {
        val item = _activeDetailItem.value ?: return
        viewModelScope.launch {
            repository.deleteItem(item)
            closeItemDetail()
            _currentScreen.value = AppScreen.MAIN
            _toastMessage.value = "Item removed from Vault"
        }
    }

    fun toggleFavorite(item: VaultItemEntity) {
        viewModelScope.launch {
            repository.toggleFavorite(item)
        }
    }

    fun cancelAddEdit() {
        _currentScreen.value = AppScreen.MAIN
    }

    fun toggleBiometricSetting(enabled: Boolean) {
        preferences.isBiometricEnabled = enabled
        isBiometricEnabled.value = enabled
        _toastMessage.value = if (enabled) "Biometric unlock enabled" else "Biometric unlock disabled"
    }

    fun setTheme(mode: String) {
        preferences.themeMode = mode
        themeMode.value = mode
    }

    fun setAutoLock(minutes: Int) {
        preferences.autoLockMinutes = minutes
        autoLockMinutes.value = minutes
        _toastMessage.value = "Auto-lock timeout updated"
    }

    fun exportEncryptedBackup(): String {
        var result = ""
        viewModelScope.launch {
            result = repository.exportBackupJson()
            copyToClipboard(result, "VaultKeep Encrypted Backup")
            _toastMessage.value = "Encrypted backup copied to clipboard!"
        }
        return result
    }

    fun exportCsv() {
        viewModelScope.launch {
            val csv = repository.exportCsv()
            copyToClipboard(csv, "VaultKeep CSV")
            _toastMessage.value = "CSV exported to clipboard!"
        }
    }

    fun importBackup(jsonString: String) {
        viewModelScope.launch {
            val count = repository.importBackupJson(jsonString)
            if (count >= 0) {
                _toastMessage.value = "Successfully restored $count items!"
            } else {
                _toastMessage.value = "Invalid backup format."
            }
        }
    }

    fun clearToast() {
        _toastMessage.value = null
    }

    fun isAutofillEnabledOnDevice(): Boolean {
        return AutofillHelper.isAutofillServiceEnabled(getApplication())
    }

    fun requestOpenAutofillSettings() {
        AutofillHelper.openAutofillSettings(getApplication())
    }
}

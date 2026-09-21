package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.AppScreen
import com.example.ui.VaultViewModel
import com.example.ui.components.BottomNavBar
import com.example.ui.components.VaultTab
import com.example.ui.screens.AddEditItemScreen
import com.example.ui.screens.AutofillScreen
import com.example.ui.screens.GeneratorScreen
import com.example.ui.screens.MasterPasswordScreen
import com.example.ui.screens.OnboardingScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SplashScreen
import com.example.ui.screens.UnlockScreen
import com.example.ui.screens.VaultListScreen
import com.example.ui.theme.VaultKeepTheme

class MainActivity : ComponentActivity() {

    private val viewModel: VaultViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()

            VaultKeepTheme(themeMode = themeMode) {
                VaultKeepApp(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun VaultKeepApp(viewModel: VaultViewModel) {
    val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()
    val selectedTab by viewModel.selectedTab.collectAsStateWithLifecycle()
    val toastMessage by viewModel.toastMessage.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(toastMessage) {
        toastMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearToast()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            if (currentScreen == AppScreen.MAIN) {
                BottomNavBar(
                    selectedTab = selectedTab,
                    onTabSelected = { viewModel.setTab(it) }
                )
            }
        },
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0)
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            AnimatedContent(
                targetState = currentScreen,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "ScreenTransition"
            ) { screen ->
                when (screen) {
                    AppScreen.SPLASH -> {
                        SplashScreen()
                    }

                    AppScreen.ONBOARDING -> {
                        val index by viewModel.onboardingIndex.collectAsStateWithLifecycle()
                        OnboardingScreen(
                            currentIndex = index,
                            onNext = { viewModel.nextOnboarding() },
                            onSkip = { viewModel.completeOnboarding() }
                        )
                    }

                    AppScreen.CREATE_MASTER_PASSWORD -> {
                        val input by viewModel.masterPasswordInput.collectAsStateWithLifecycle()
                        val strength by viewModel.masterPasswordStrength.collectAsStateWithLifecycle()
                        MasterPasswordScreen(
                            passwordInput = input,
                            strength = strength,
                            onPasswordChange = { viewModel.updateMasterPasswordInput(it) },
                            onSubmit = { viewModel.submitMasterPassword() }
                        )
                    }

                    AppScreen.LOCKED -> {
                        val biometricState by viewModel.biometricState.collectAsStateWithLifecycle()
                        val unlockInput by viewModel.unlockPasswordInput.collectAsStateWithLifecycle()
                        val errorMsg by viewModel.unlockErrorMessage.collectAsStateWithLifecycle()
                        UnlockScreen(
                            biometricState = biometricState,
                            passwordInput = unlockInput,
                            errorMessage = errorMsg,
                            onPasswordChange = { viewModel.updateUnlockPasswordInput(it) },
                            onSubmitPassword = { viewModel.submitUnlockPassword() },
                            onTriggerBiometric = { success -> viewModel.triggerBiometricUnlock(success) }
                        )
                    }

                    AppScreen.MAIN -> {
                        when (selectedTab) {
                            VaultTab.VAULT -> {
                                val items by viewModel.vaultItems.collectAsStateWithLifecycle()
                                val query by viewModel.searchQuery.collectAsStateWithLifecycle()
                                val category by viewModel.selectedCategory.collectAsStateWithLifecycle()
                                val activeItem by viewModel.activeDetailItem.collectAsStateWithLifecycle()
                                val decryptedPwd by viewModel.decryptedPassword.collectAsStateWithLifecycle()

                                VaultListScreen(
                                    items = items,
                                    searchQuery = query,
                                    selectedCategory = category,
                                    activeDetailItem = activeItem,
                                    decryptedPassword = decryptedPwd,
                                    onSearchChange = { viewModel.setSearchQuery(it) },
                                    onCategorySelect = { viewModel.setSelectedCategory(it) },
                                    onItemClick = { viewModel.openItemDetail(it) },
                                    onCloseDetail = { viewModel.closeItemDetail() },
                                    onEditItem = { viewModel.startEditItem(it) },
                                    onDeleteItem = { viewModel.deleteCurrentItem() },
                                    onToggleFavorite = { viewModel.toggleFavorite(it) },
                                    onCopy = { text, label -> viewModel.copyToClipboard(text, label) },
                                    onAddNewItem = { viewModel.startAddItem() }
                                )
                            }

                            VaultTab.GENERATOR -> {
                                val genPwd by viewModel.generatedPassword.collectAsStateWithLifecycle()
                                val length by viewModel.generatorLength.collectAsStateWithLifecycle()
                                val upper by viewModel.genUpper.collectAsStateWithLifecycle()
                                val lower by viewModel.genLower.collectAsStateWithLifecycle()
                                val nums by viewModel.genNumbers.collectAsStateWithLifecycle()
                                val symbols by viewModel.genSymbols.collectAsStateWithLifecycle()
                                val strength by viewModel.generatorStrength.collectAsStateWithLifecycle()

                                GeneratorScreen(
                                    generatedPassword = genPwd,
                                    length = length,
                                    includeUpper = upper,
                                    includeLower = lower,
                                    includeNumbers = nums,
                                    includeSymbols = symbols,
                                    strength = strength,
                                    onLengthChange = { viewModel.setGeneratorLength(it) },
                                    onToggleUpper = { viewModel.toggleUpper(it) },
                                    onToggleLower = { viewModel.toggleLower(it) },
                                    onToggleNumbers = { viewModel.toggleNumbers(it) },
                                    onToggleSymbols = { viewModel.toggleSymbols(it) },
                                    onGenerate = { viewModel.generateNewPassword() },
                                    onCopy = { viewModel.copyToClipboard(it, "Generated Password") }
                                )
                            }

                            VaultTab.AUTOFILL -> {
                                val items by viewModel.vaultItems.collectAsStateWithLifecycle()
                                val isEnabled = viewModel.isAutofillEnabledOnDevice()

                                AutofillScreen(
                                    isAutofillEnabled = isEnabled,
                                    vaultItems = items,
                                    onOpenAutofillSettings = { viewModel.requestOpenAutofillSettings() },
                                    onDecryptPassword = { viewModel.decryptedPassword.value.ifBlank { "••••••••" } }
                                )
                            }

                            VaultTab.SETTINGS -> {
                                val theme by viewModel.themeMode.collectAsStateWithLifecycle()
                                val biometrics by viewModel.isBiometricEnabled.collectAsStateWithLifecycle()
                                val autoLock by viewModel.autoLockMinutes.collectAsStateWithLifecycle()

                                SettingsScreen(
                                    themeMode = theme,
                                    isBiometricEnabled = biometrics,
                                    autoLockMinutes = autoLock,
                                    onThemeChange = { viewModel.setTheme(it) },
                                    onBiometricToggle = { viewModel.toggleBiometricSetting(it) },
                                    onAutoLockChange = { viewModel.setAutoLock(it) },
                                    onLockNow = { viewModel.lockVault() },
                                    onExportEncryptedBackup = { viewModel.exportEncryptedBackup() },
                                    onExportCsv = { viewModel.exportCsv() },
                                    onImportBackup = { viewModel.importBackup(it) },
                                    onOpenAutofillSettings = { viewModel.requestOpenAutofillSettings() }
                                )
                            }
                        }
                    }

                    AppScreen.ADD_ITEM -> {
                        AddEditItemScreen(
                            editingItem = null,
                            initialPassword = "",
                            onSave = { title, website, user, pwd, notes, cat ->
                                viewModel.saveItem(title, website, user, pwd, notes, cat)
                            },
                            onDelete = null,
                            onBack = { viewModel.cancelAddEdit() }
                        )
                    }

                    AppScreen.EDIT_ITEM -> {
                        val activeItem by viewModel.activeDetailItem.collectAsStateWithLifecycle()
                        val decryptedPwd by viewModel.decryptedPassword.collectAsStateWithLifecycle()

                        AddEditItemScreen(
                            editingItem = activeItem,
                            initialPassword = decryptedPwd,
                            onSave = { title, website, user, pwd, notes, cat ->
                                viewModel.saveItem(title, website, user, pwd, notes, cat)
                            },
                            onDelete = { viewModel.deleteCurrentItem() },
                            onBack = { viewModel.cancelAddEdit() }
                        )
                    }
                }
            }
        }
    }
}

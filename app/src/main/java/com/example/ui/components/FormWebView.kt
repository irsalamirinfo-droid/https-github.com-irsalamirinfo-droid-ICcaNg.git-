package com.example.ui.components

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.data.models.AutofillProfile
import com.example.data.models.GoogleForm
import com.example.ui.viewmodel.FormViewModel

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun FormWebView(
    form: GoogleForm,
    viewModel: FormViewModel,
    profiles: List<AutofillProfile>,
    activeProfile: AutofillProfile?,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var webViewInstance by remember { mutableStateOf<WebView?>(null) }
    
    val webViewProgress by viewModel.webViewProgress.collectAsState()
    val isWebViewLoading by viewModel.isWebViewLoading.collectAsState()

    var showProfileSelectorDialog by remember { mutableStateOf(false) }

    // Helper function to inject the smart auto-fill JS
    fun runSmartAutofill(selectedProfile: AutofillProfile?, webView: WebView?) {
        if (webView == null) return
        if (selectedProfile == null) {
            Toast.makeText(context, "Silakan pilih profil autofill terlebih dahulu!", Toast.LENGTH_SHORT).show()
            return
        }

        val cleanName = selectedProfile.fullName.replace("'", "\\'")
        val cleanEmail = selectedProfile.email.replace("'", "\\'")
        val cleanPhone = selectedProfile.phone.replace("'", "\\'")
        val cleanId = selectedProfile.identityNumber.replace("'", "\\'")
        val cleanCustomFields = selectedProfile.customFieldsJson.ifBlank { "{}" }

        val jsCode = """
            (function() {
              const profile = {
                fullName: '$cleanName',
                email: '$cleanEmail',
                phone: '$cleanPhone',
                identityNumber: '$cleanId',
                customFields: $cleanCustomFields
              };
              
              const mappings = {
                fullName: ["nama", "nama lengkap", "name", "full name", "fullname", "peserta", "pendaftar"],
                email: ["email", "e-mail", "surel", "mail", "pos elektronik", "alamat email", "alamat surel"],
                phone: ["telepon", "phone", "no hp", "no. hp", "no. handphone", "whatsapp", "wa", "no telp", "no. telp", "handphone", "telp"],
                identityNumber: ["nim", "nip", "npm", "nik", "id", "no identitas", "nomor induk", "no. identitas", "no.mhs", "nomor mahasiswa", "nomor registrasi", "no registrasi"]
              };

              let fillCount = 0;
              const inputs = document.querySelectorAll('input[type="text"], input[type="email"], textarea');
              
              inputs.forEach(input => {
                let labelText = "";
                
                // 1. Aria-label check
                if (input.getAttribute('aria-label')) {
                  labelText = input.getAttribute('aria-label').toLowerCase();
                }
                
                // 2. Traversal heading search if blank
                if (!labelText) {
                  let parent = input.parentElement;
                  for (let i = 0; i < 6 && parent; i++) {
                    const heading = parent.querySelector('[role="heading"], .M7Me3b, .Ho7oCc, .HeaderText, label, .ss-q-title');
                    if (heading) {
                      labelText = heading.textContent.toLowerCase();
                      break;
                    }
                    parent = parent.parentElement;
                  }
                }
                
                if (labelText) {
                  let matched = false;
                  
                  // Name Match
                  if (!matched && profile.fullName) {
                    for (const kw of mappings.fullName) {
                      if (labelText.includes(kw)) {
                        input.value = profile.fullName;
                        matched = true;
                        break;
                      }
                    }
                  }
                  
                  // Email Match
                  if (!matched && profile.email) {
                    for (const kw of mappings.email) {
                      if (labelText.includes(kw)) {
                        input.value = profile.email;
                        matched = true;
                        break;
                      }
                    }
                  }
                  
                  // Phone Match
                  if (!matched && profile.phone) {
                    for (const kw of mappings.phone) {
                      if (labelText.includes(kw)) {
                        input.value = profile.phone;
                        matched = true;
                        break;
                      }
                    }
                  }
                  
                  // Id Match
                  if (!matched && profile.identityNumber) {
                    for (const kw of mappings.identityNumber) {
                      if (labelText.includes(kw)) {
                        input.value = profile.identityNumber;
                        matched = true;
                        break;
                      }
                    }
                  }

                  // Custom fields Match
                  if (!matched && profile.customFields) {
                    for (const [key, value] of Object.entries(profile.customFields)) {
                      if (labelText.includes(key.toLowerCase())) {
                        input.value = value.toString();
                        matched = true;
                        break;
                      }
                    }
                  }
                  
                  if (matched) {
                    fillCount++;
                    input.dispatchEvent(new Event('input', { bubbles: true }));
                    input.dispatchEvent(new Event('change', { bubbles: true }));
                    input.dispatchEvent(new Event('blur', { bubbles: true }));
                  }
                }
              });

              // 3. Selection radio buttons & Checkboxes
              const choices = document.querySelectorAll('[role="radio"], [role="checkbox"]');
              let choiceCount = 0;
              
              choices.forEach(btn => {
                let btnLabel = btn.getAttribute('aria-label') || btn.textContent || "";
                btnLabel = btnLabel.trim().toLowerCase();
                
                let parentText = "";
                let parent = btn.parentElement;
                for (let i = 0; i < 6 && parent; i++) {
                  const qText = parent.querySelector('[role="heading"], .M7Me3b, .ss-q-title');
                  if (qText) {
                    parentText = qText.textContent.toLowerCase();
                    break;
                  }
                  parent = parent.parentElement;
                }

                if (profile.customFields && parentText) {
                  for (const [key, value] of Object.entries(profile.customFields)) {
                    const queryKey = key.toLowerCase();
                    const optionVal = value.toString().toLowerCase();
                    if (parentText.includes(queryKey) && (btnLabel === optionVal || btnLabel.includes(optionVal))) {
                      btn.click();
                      choiceCount++;
                      break;
                    }
                  }
                }
              });
              
              return "Otomatis mengisi " + fillCount + " kolom teks & memilih " + choiceCount + " opsi!";
            })();
        """.trimIndent()

        webView.evaluateJavascript(jsCode) { result ->
            val cleanedResult = result?.removeSurrounding("\"") ?: "Pengisian selesai"
            Toast.makeText(context, cleanedResult, Toast.LENGTH_LONG).show()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = form.title,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = activeProfile?.profileName ?: "Tanpa Profil Autofill",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.secondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Kembali ke Dashboard"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showProfileSelectorDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Ganti Profil Autofill"
                        )
                    }
                    IconButton(onClick = { webViewInstance?.reload() }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Muat Ulang"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    titleContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { runSmartAutofill(activeProfile, webViewInstance) },
                icon = { Icon(Icons.Filled.PlayArrow, "AutoFill Icon") },
                text = { Text("⚡ ISI OTOMATIS", fontWeight = FontWeight.Bold) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Progress loader
                AnimatedVisibility(
                    visible = isWebViewLoading,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    LinearProgressIndicator(
                        progress = { webViewProgress / 100f },
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                }

                // WebView view integration
                AndroidView(
                    factory = { ctx ->
                        WebView(ctx).apply {
                            settings.apply {
                                javaScriptEnabled = true
                                domStorageEnabled = true
                                databaseEnabled = true
                                useWideViewPort = true
                                loadWithOverviewMode = true
                                supportZoom()
                                builtInZoomControls = true
                                displayZoomControls = false
                            }
                            
                            webViewClient = object : WebViewClient() {
                                override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                    super.onPageStarted(view, url, favicon)
                                    viewModel.setWebViewLoading(true)
                                }

                                override fun onPageFinished(view: WebView?, url: String?) {
                                    super.onPageFinished(view, url)
                                    viewModel.setWebViewLoading(false)
                                    
                                    // Proactively auto fill as soon as the page loads!
                                    if (activeProfile != null) {
                                        runSmartAutofill(activeProfile, view)
                                    }
                                }
                            }

                            webChromeClient = object : WebChromeClient() {
                                override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                    super.onProgressChanged(view, newProgress)
                                    viewModel.setWebViewProgress(newProgress)
                                }
                            }

                            loadUrl(form.url)
                            webViewInstance = this
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                )
            }
        }
    }

    // Modal to switch Autofill Profile on-the-fly
    if (showProfileSelectorDialog) {
        AlertDialog(
            onDismissRequest = { showProfileSelectorDialog = false },
            title = { Text("Pilih Profil Pengisian") },
            text = {
                if (profiles.isEmpty()) {
                    Text("Belum ada profil autofill tersimpan. Buat profil terlebih dahulu di Dashboard.")
                } else {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Ganti profil pengisian aktif untuk form ini:",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        profiles.forEach { p ->
                            Button(
                                onClick = {
                                    viewModel.setActiveProfile(p)
                                    showProfileSelectorDialog = false
                                    // Trigger fill immediately
                                    runSmartAutofill(p, webViewInstance)
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (p.id == activeProfile?.id) 
                                        MaterialTheme.colorScheme.primaryContainer 
                                    else 
                                        MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor = if (p.id == activeProfile?.id) 
                                        MaterialTheme.colorScheme.onPrimaryContainer 
                                    else 
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            ) {
                                Text(p.profileName, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showProfileSelectorDialog = false }) {
                    Text("Tutup")
                }
            }
        )
    }
}

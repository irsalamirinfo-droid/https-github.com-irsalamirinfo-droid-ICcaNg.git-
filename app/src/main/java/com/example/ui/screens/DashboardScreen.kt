package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.R
import com.example.data.models.AutofillProfile
import com.example.data.models.GoogleForm
import com.example.data.models.GoogleFormWithProfile
import com.example.ui.components.FormWebView
import com.example.ui.viewmodel.FormViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: FormViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    
    // Core states
    val profiles by viewModel.allProfiles.collectAsState()
    val allFormsWithProfile by viewModel.allFormsWithProfile.collectAsState()
    val filteredForms by viewModel.filteredForms.collectAsState()
    val activeForm by viewModel.activeForm.collectAsState()
    val activeProfile by viewModel.activeProfile.collectAsState()
    
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()

    var activeTab by remember { mutableIntStateOf(0) } // 0 = Daftar Form, 1 = Profil Autofill
    
    // Dialog control
    var showAddFormDialog by remember { mutableStateOf(false) }
    var selectedFormToEdit by remember { mutableStateOf<GoogleForm?>(null) }

    var showAddProfileDialog by remember { mutableStateOf(false) }
    var selectedProfileToEdit by remember { mutableStateOf<AutofillProfile?>(null) }

    val categories = listOf("Semua", "Absensi", "Pendaftaran", "Survey", "Tugas", "Lain-lain")

    // Fullscreen WebView Layer
    if (activeForm != null) {
        FormWebView(
            form = activeForm!!,
            viewModel = viewModel,
            profiles = profiles,
            activeProfile = activeProfile,
            onClose = { viewModel.closeActiveForm() }
        )
    } else {
        Scaffold(
            topBar = {
                LargeTopAppBar(
                    title = {
                        Column {
                            Text(
                                "FormAuto", 
                                fontWeight = FontWeight.Black,
                                style = MaterialTheme.typography.headlineMedium
                            )
                            Text(
                                "Otomasi Pengisian & Pengakses Google Form",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = {
                            Toast.makeText(context, "Aplikasi FormAuto Aktif & Siap digunakan!", Toast.LENGTH_SHORT).show()
                        }) {
                            Icon(Icons.Default.CheckCircle, "Status")
                        }
                    },
                    colors = TopAppBarDefaults.largeTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                        titleContentColor = MaterialTheme.colorScheme.primary
                    )
                )
            },
            floatingActionButton = {
                if (activeTab == 0) {
                    ExtendedFloatingActionButton(
                        onClick = { 
                            selectedFormToEdit = null
                            showAddFormDialog = true 
                        },
                        icon = { Icon(Icons.Default.Add, "Tambah Form") },
                        text = { Text("Tambah Form") },
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                } else {
                    ExtendedFloatingActionButton(
                        onClick = { 
                            selectedProfileToEdit = null
                            showAddProfileDialog = true 
                        },
                        icon = { Icon(Icons.Default.Person, "Tambah Profil") },
                        text = { Text("Buat Profil") },
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        ) { innerPadding ->
            LazyColumn(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 1. Hero visual card banner
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(170.dp)
                            .clip(RoundedCornerShape(20.dp))
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.img_form_hero),
                            contentDescription = "Visual Hero Banner",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f))
                                    )
                                )
                        )
                        Column(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(16.dp)
                        ) {
                            Text(
                                "Mulai Pengisian Instan",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Mendukung auto-mapping teks, absensi pilihan ganda, dan checkout form harian lokal.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.LightGray
                            )
                        }
                    }
                }

                // Bento Quick Stats Section
                item {
                    val totalLinks = allFormsWithProfile.size
                    val todayFormatted = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())
                    val todayAccessCount = allFormsWithProfile.count { formWithProfile ->
                        if (formWithProfile.form.lastAccessedAt > 0L) {
                            SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date(formWithProfile.form.lastAccessedAt)) == todayFormatted
                        } else {
                            false
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Bento Card 1: Stored Links
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .height(115.dp),
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(containerColor = com.example.ui.theme.PolishBentoPurple)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Icon(
                                    imageVector = Icons.Default.List,
                                    contentDescription = null,
                                    tint = com.example.ui.theme.Purple40,
                                    modifier = Modifier.size(24.dp)
                                )
                                Column {
                                    Text(
                                        text = "$totalLinks",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = com.example.ui.theme.Purple40
                                    )
                                    Text(
                                        text = "Stored Links",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = com.example.ui.theme.PolishTextDark.copy(alpha = 0.7f),
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }

                        // Bento Card 2: Today's Access
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .height(115.dp),
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(containerColor = com.example.ui.theme.PolishBentoLavender)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = null,
                                    tint = com.example.ui.theme.PolishTextDark,
                                    modifier = Modifier.size(24.dp)
                                )
                                Column {
                                    Text(
                                        text = "$todayAccessCount",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = com.example.ui.theme.PolishTextDark
                                    )
                                    Text(
                                        text = "Today's Access",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = com.example.ui.theme.PolishTextDark.copy(alpha = 0.7f),
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }

                // 2. Tab Navigation
                item {
                    PrimaryTabRow(
                        selectedTabIndex = activeTab,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                    ) {
                        Tab(
                            selected = activeTab == 0,
                            onClick = { activeTab = 0 },
                            text = { 
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.List, null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Daftar Form (${filteredForms.size})", fontWeight = FontWeight.Bold)
                                }
                            }
                        )
                        Tab(
                            selected = activeTab == 1,
                            onClick = { activeTab = 1 },
                            text = { 
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.AccountCircle, null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Profil Autofill (${profiles.size})", fontWeight = FontWeight.Bold)
                                }
                            }
                        )
                    }
                }

                // Tab Contents
                if (activeTab == 0) {
                    // TAB 0: GOOGLE FORM MANAGER
                    item {
                        // Search OutlinedTextField
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { viewModel.setSearchQuery(it) },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("Cari formulir atau link...") },
                            leadingIcon = { Icon(Icons.Default.Search, "Search") },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                        Icon(Icons.Default.Clear, "Clear")
                                    }
                                }
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )
                    }

                    // Category filters
                    item {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(categories) { categoryName ->
                                FilterChip(
                                    selected = selectedCategory == categoryName,
                                    onClick = { viewModel.setSelectedCategory(categoryName) },
                                    label = { Text(categoryName) }
                                )
                            }
                        }
                    }

                    if (filteredForms.isEmpty()) {
                        item {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 32.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        Icons.Default.Info,
                                        contentDescription = "Empty",
                                        tint = MaterialTheme.colorScheme.secondary,
                                        modifier = Modifier.size(48.dp)
                                    )
                                    Spacer(Modifier.height(12.dp))
                                    Text(
                                        "Tidak ada Google Form ditemukan",
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center
                                    )
                                    Text(
                                        "Tambahkan link Google Form Anda menggunakan tombol tambah dibawah.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    } else {
                        items(filteredForms, key = { it.form.id }) { item ->
                            FormItemCard(
                                item = item,
                                onOpen = { viewModel.openFormInApp(item.form) },
                                onEdit = {
                                    selectedFormToEdit = item.form
                                    showAddFormDialog = true
                                },
                                onDelete = { viewModel.deleteForm(item.form) }
                            )
                        }
                    }
                } else {
                    // TAB 1: AUTOFILL PROFILES MANAGER
                    if (profiles.isEmpty()) {
                        item {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 32.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        Icons.Default.AccountBox,
                                        contentDescription = "Empty Profiles",
                                        tint = MaterialTheme.colorScheme.secondary,
                                        modifier = Modifier.size(48.dp)
                                    )
                                    Spacer(Modifier.height(12.dp))
                                    Text(
                                        "Belum ada profil autofill",
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center
                                    )
                                    Text(
                                        "Profil autofill menyimpan data pribadi Anda untuk diisi otomatis ke dalam kolom Form secara cerdas.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    } else {
                        items(profiles, key = { it.id }) { profile ->
                            ProfileItemCard(
                                profile = profile,
                                onEdit = {
                                    selectedProfileToEdit = profile
                                    showAddProfileDialog = true
                                },
                                onDelete = { 
                                    viewModel.deleteProfile(profile)
                                    Toast.makeText(context, "Profil '${profile.profileName}' terhapus.", Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // Modal Dialog: Add/Edit Form Links
    if (showAddFormDialog) {
        AddFormDialog(
            form = selectedFormToEdit,
            profiles = profiles,
            onDismiss = { showAddFormDialog = false },
            onSave = { form ->
                viewModel.saveForm(form)
                showAddFormDialog = false
                Toast.makeText(context, "Form berhasil disimpan!", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // Modal Dialog: Add/Edit Autofill Profile
    if (showAddProfileDialog) {
        AddProfileDialog(
            profile = selectedProfileToEdit,
            onDismiss = { showAddProfileDialog = false },
            onSave = { profile ->
                viewModel.saveProfile(profile)
                showAddProfileDialog = false
                Toast.makeText(context, "Profil berhasil disimpan!", Toast.LENGTH_SHORT).show()
            }
        )
    }
}

@Composable
fun FormItemCard(
    item: GoogleFormWithProfile,
    onOpen: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    var expandedMenu by remember { mutableStateOf(false) }
    
    val formattedDate = if (item.form.lastAccessedAt > 0L) {
        val sdf = SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault())
        sdf.format(Date(item.form.lastAccessedAt))
    } else {
        "Belum diakses"
    }

    // Category styling mapping
    val avatarBg = when (item.form.category) {
        "Absensi" -> com.example.ui.theme.AccentRedBg
        "Pendaftaran" -> com.example.ui.theme.AccentBlueBg
        "Survey" -> com.example.ui.theme.AccentGreenBg
        else -> com.example.ui.theme.PolishBentoPurple
    }
    val avatarTextColor = when (item.form.category) {
        "Absensi" -> com.example.ui.theme.AccentRedText
        "Pendaftaran" -> com.example.ui.theme.AccentBlueText
        "Survey" -> com.example.ui.theme.AccentGreenText
        else -> com.example.ui.theme.Purple40
    }
    val categoryIcon = when (item.form.category) {
        "Absensi" -> Icons.Default.DateRange
        "Pendaftaran" -> Icons.Default.AccountBox
        "Survey" -> Icons.Default.Star
        else -> Icons.Default.List
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = com.example.ui.theme.PolishSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, com.example.ui.theme.PolishBorder)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Category Avatar
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(avatarBg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = categoryIcon,
                        contentDescription = item.form.category,
                        tint = avatarTextColor,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(Modifier.width(12.dp))

                // Metadata Info Column
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = item.form.category,
                            style = MaterialTheme.typography.labelSmall,
                            color = avatarTextColor,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "•",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.Gray
                        )
                        Text(
                            text = "Last: $formattedDate",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.Gray
                        )
                    }

                    Spacer(Modifier.height(2.dp))

                    Text(
                        text = item.form.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = com.example.ui.theme.PolishTextDark,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Inline Dropdown Settings Menu
                Box {
                    IconButton(onClick = { expandedMenu = true }) {
                        Icon(Icons.Default.MoreVert, "Menu", tint = Color.Gray)
                    }
                    DropdownMenu(
                        expanded = expandedMenu,
                        onDismissRequest = { expandedMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Edit Detail") },
                            leadingIcon = { Icon(Icons.Default.Edit, null) },
                            onClick = {
                                expandedMenu = false
                                onEdit()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Hapus Form") },
                            leadingIcon = { Icon(Icons.Default.Delete, null) },
                            colors = MenuDefaults.itemColors(
                                textColor = MaterialTheme.colorScheme.error,
                                leadingIconColor = MaterialTheme.colorScheme.error
                            ),
                            onClick = {
                                expandedMenu = false
                                onDelete()
                            }
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = com.example.ui.theme.PolishBorder.copy(alpha = 0.5f))
            Spacer(Modifier.height(12.dp))

            // Action elements
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Active profile binding status pill
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(30.dp))
                        .background(
                            if (item.profile != null) com.example.ui.theme.PolishBentoPurple
                            else com.example.ui.theme.PolishBackground
                        )
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = if (item.profile != null) Icons.Default.CheckCircle else Icons.Default.Info,
                            contentDescription = null,
                            tint = if (item.profile != null) com.example.ui.theme.Purple40 else Color.Gray,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = item.profile?.profileName ?: "Manual harian",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = if (item.profile != null) com.example.ui.theme.Purple40 else Color.Gray
                        )
                    }
                }

                Button(
                    onClick = onOpen,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = com.example.ui.theme.Purple40,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                    modifier = Modifier.height(38.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Akses Form", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun ProfileItemCard(
    profile: AutofillProfile,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }
    var expandedMenu by remember { mutableStateOf(false) }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AccountCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            text = profile.profileName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (profile.fullName.isNotBlank()) profile.fullName else "Nama Kosong",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row {
                    IconButton(onClick = { isExpanded = !isExpanded }) {
                        Icon(
                            if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = "Detail"
                        )
                    }
                    Box {
                        IconButton(onClick = { expandedMenu = true }) {
                            Icon(Icons.Default.MoreVert, "Menu")
                        }
                        DropdownMenu(
                            expanded = expandedMenu,
                            onDismissRequest = { expandedMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Edit Profil") },
                                leadingIcon = { Icon(Icons.Default.Edit, null) },
                                onClick = {
                                    expandedMenu = false
                                    onEdit()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Hapus Profil") },
                                leadingIcon = { Icon(Icons.Default.Delete, null) },
                                colors = MenuDefaults.itemColors(
                                    textColor = MaterialTheme.colorScheme.error,
                                    leadingIconColor = MaterialTheme.colorScheme.error
                                ),
                                onClick = {
                                    expandedMenu = false
                                    onDelete()
                                }
                            )
                        }
                    }
                }
            }

            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(Modifier.height(4.dp))

                    ProfileFieldRow(label = "Email Utama", value = profile.email)
                    ProfileFieldRow(label = "No. Telepon / WhatsApp", value = profile.phone)
                    ProfileFieldRow(label = "Nomor Identitas (NIM/NIP)", value = profile.identityNumber)

                    if (profile.customFieldsJson.isNotBlank() && profile.customFieldsJson != "{}") {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "Kunci Kustom Tambahan:",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                        ) {
                            Text(
                                text = profile.customFieldsJson,
                                modifier = Modifier.padding(12.dp),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ProfileFieldRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )
        Text(
            text = value.ifBlank { "-" },
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun AddFormDialog(
    form: GoogleForm?,
    profiles: List<AutofillProfile>,
    onDismiss: () -> Unit,
    onSave: (GoogleForm) -> Unit
) {
    var title by remember { mutableStateOf(form?.title ?: "") }
    var url by remember { mutableStateOf(form?.url ?: "") }
    var category by remember { mutableStateOf(form?.category ?: "Lain-lain") }
    var selectedProfileId by remember { mutableStateOf(form?.autoFillProfileId) }
    
    var showCategoryDropdown by remember { mutableStateOf(false) }
    var showProfileDropdown by remember { mutableStateOf(false) }

    val categories = listOf("Absensi", "Pendaftaran", "Survey", "Tugas", "Lain-lain")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (form == null) "Tambah Google Form Baru" else "Edit Detail Form") },
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Title
                item {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Nama/Judul Formulir") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }

                // Link URL Google Form
                item {
                    OutlinedTextField(
                        value = url,
                        onValueChange = { url = it },
                        label = { Text("Link Google Form") },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("https://docs.google.com/forms/d/...") },
                        supportingText = {
                            Text("Harus diawali dengan docs.google.com/forms")
                        },
                        singleLine = true
                    )
                }

                // Category Selector Dropdown
                item {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = category,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Kategori") },
                            modifier = Modifier.fillMaxWidth(),
                            trailingIcon = {
                                IconButton(onClick = { showCategoryDropdown = true }) {
                                    Icon(Icons.Default.ArrowDropDown, null)
                                }
                            }
                        )
                        DropdownMenu(
                            expanded = showCategoryDropdown,
                            onDismissRequest = { showCategoryDropdown = false },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            categories.forEach { cat ->
                                DropdownMenuItem(
                                    text = { Text(cat) },
                                    onClick = {
                                        category = cat
                                        showCategoryDropdown = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Selected Profile Binding
                item {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        val currentProfileName = profiles.find { it.id == selectedProfileId }?.profileName 
                            ?: "Pengisian Manual (Belum Terikat)"
                        OutlinedTextField(
                            value = currentProfileName,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Profil Pengisi Default") },
                            modifier = Modifier.fillMaxWidth(),
                            trailingIcon = {
                                IconButton(onClick = { showProfileDropdown = true }) {
                                    Icon(Icons.Default.ArrowDropDown, null)
                                }
                            }
                        )
                        DropdownMenu(
                            expanded = showProfileDropdown,
                            onDismissRequest = { showProfileDropdown = false },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            DropdownMenuItem(
                                text = { Text("Pengisian Manual (Tanpa Autolink)") },
                                onClick = {
                                    selectedProfileId = null
                                    showProfileDropdown = false
                                }
                            )
                            profiles.forEach { profile ->
                                DropdownMenuItem(
                                    text = { Text(profile.profileName) },
                                    onClick = {
                                        selectedProfileId = profile.id
                                        showProfileDropdown = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isBlank()) {
                        return@Button
                    }
                    if (url.isBlank() || !url.contains("docs.google.com")) {
                        return@Button
                    }

                    onSave(
                        GoogleForm(
                            id = form?.id ?: 0,
                            title = title,
                            url = url.trim(),
                            category = category,
                            autoFillProfileId = selectedProfileId,
                            createdAt = form?.createdAt ?: System.currentTimeMillis(),
                            lastAccessedAt = form?.lastAccessedAt ?: 0L
                        )
                    )
                },
                enabled = title.isNotBlank() && url.isNotBlank() && url.contains("docs.google.com")
            ) {
                Text("Simpan")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal")
            }
        }
    )
}

@Composable
fun AddProfileDialog(
    profile: AutofillProfile?,
    onDismiss: () -> Unit,
    onSave: (AutofillProfile) -> Unit
) {
    var profileName by remember { mutableStateOf(profile?.profileName ?: "") }
    var fullName by remember { mutableStateOf(profile?.fullName ?: "") }
    var email by remember { mutableStateOf(profile?.email ?: "") }
    var phone by remember { mutableStateOf(profile?.phone ?: "") }
    var identityNumber by remember { mutableStateOf(profile?.identityNumber ?: "") }
    var customFieldsJson by remember { mutableStateOf(profile?.customFieldsJson ?: "{\n  \"kelas\": \"\",\n  \"instansi\": \"\"\n}") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (profile == null) "Buat Profil Autofill Baru" else "Edit Profil") },
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    OutlinedTextField(
                        value = profileName,
                        onValueChange = { profileName = it },
                        label = { Text("Nama Label Profil") },
                        placeholder = { Text("Nama atau Keperluan") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }

                item {
                    OutlinedTextField(
                        value = fullName,
                        onValueChange = { fullName = it },
                        label = { Text("Nama Lengkap") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }

                item {
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Alamat Email Utama") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }

                item {
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("No. HP / WhatsApp") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }

                item {
                    OutlinedTextField(
                        value = identityNumber,
                        onValueChange = { identityNumber = it },
                        label = { Text("No Identitas (NIM, NIP, ID)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }

                item {
                    Column {
                        Text(
                            text = "Kunci Kustom Tambahan (Format JSON):",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Kunci kustom ini akan dicocokkan otomatis apabila pertanyaan Google Form mengandung kunci tersebut (contoh: \"alamat\", \"kelas\", \"jurusan\").",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                        )
                        Spacer(Modifier.height(4.dp))
                        OutlinedTextField(
                            value = customFieldsJson,
                            onValueChange = { customFieldsJson = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(130.dp),
                            maxLines = 10,
                            textStyle = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (profileName.isBlank()) return@Button
                    
                    // Simple validation / cleaning of JSON format
                    val cleanedJson = try {
                        val trimmed = customFieldsJson.trim()
                        if (trimmed.isEmpty()) "{}" else trimmed
                    } catch (e: Exception) {
                        "{}"
                    }

                    onSave(
                        AutofillProfile(
                            id = profile?.id ?: 0,
                            profileName = profileName,
                            fullName = fullName,
                            email = email,
                            phone = phone,
                            identityNumber = identityNumber,
                            customFieldsJson = cleanedJson
                        )
                    )
                },
                enabled = profileName.isNotBlank()
            ) {
                Text("Simpan")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal")
            }
        }
    )
}

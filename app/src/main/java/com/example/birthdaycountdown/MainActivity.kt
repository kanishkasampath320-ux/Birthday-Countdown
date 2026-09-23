package com.example.birthdaycountdown

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import coil.compose.AsyncImage
import com.example.birthdaycountdown.data.model.Birthday
import com.example.birthdaycountdown.notification.BirthdayNotificationWorker
import com.example.birthdaycountdown.ui.BirthdayViewModel
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.*
import kotlin.random.Random

// Global Dark Mode Palette
val DarkCharcoal = Color(0xFF121212)
val DeepPurple = Color(0xFF1A0933)
val BrightPurple = Color(0xFFBB86FC)
val CardDark = Color(0xFF1E1E1E)
val OffWhite = Color(0xFFF5F5F5)
val MutedLavender = Color(0xFFB39DDB)
val SoftGrey = Color(0xFF9E9E9E)

// Light Mode Palette
val LightCream = Color(0xFFFFF9F0)
val LightPurple = Color(0xFFEDE7F6)
val PrimaryPurple = Color(0xFF6200EE)

sealed class Screen(val route: String, val label: String, val icon: ImageVector) {
    object Dashboard : Screen("dashboard", "Home", Icons.Default.Dashboard)
    object Calendar : Screen("calendar", "Calendar", Icons.Default.CalendarMonth)
    object Templates : Screen("templates", "Card Maker", Icons.Default.AutoAwesome)
    object Settings : Screen("settings", "Settings", Icons.Default.Settings)
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        BirthdayNotificationWorker.scheduleDailyWorker(applicationContext)

        setContent {
            val viewModel: BirthdayViewModel = viewModel()
            val isDarkMode by viewModel.isDarkMode.collectAsState()

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val launcher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestPermission()
                ) { _ -> }
                
                LaunchedEffect(Unit) {
                    if (ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                        launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
            }
            
            CelebrationsAppTheme(isDarkMode) {
                BirthdayApp(viewModel)
            }
        }
    }
}

@Composable
fun CelebrationsAppTheme(isDarkMode: Boolean, content: @Composable () -> Unit) {
    val colorScheme = if (isDarkMode) {
        darkColorScheme(
            primary = BrightPurple,
            background = DarkCharcoal,
            surface = CardDark,
            onBackground = OffWhite,
            onSurface = OffWhite
        )
    } else {
        lightColorScheme(
            primary = PrimaryPurple,
            background = LightCream,
            surface = Color.White,
            onBackground = Color.Black,
            onSurface = Color.Black
        )
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}

@Composable
fun BirthdayApp(viewModel: BirthdayViewModel) {
    val navController = rememberNavController()
    val isDarkMode by viewModel.isDarkMode.collectAsState()
    
    val bgBrush = if (isDarkMode) {
        Brush.verticalGradient(colors = listOf(DeepPurple, DarkCharcoal))
    } else {
        Brush.verticalGradient(colors = listOf(LightPurple, LightCream))
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgBrush)
    ) {
        Scaffold(
            bottomBar = { ModernBottomBar(navController, isDarkMode) },
            containerColor = Color.Transparent
        ) { padding ->
            Box(modifier = Modifier.padding(padding)) {
                NavigationGraph(navController, viewModel)
            }
        }
    }
}

@Composable
fun NavigationGraph(navController: NavHostController, viewModel: BirthdayViewModel) {
    NavHost(
        navController = navController,
        startDestination = Screen.Dashboard.route,
        enterTransition = { fadeIn(tween(400)) + slideInHorizontally { it / 10 } },
        exitTransition = { fadeOut(tween(400)) + slideOutHorizontally { -it / 10 } }
    ) {
        composable(Screen.Dashboard.route) { DashboardScreen(viewModel) }
        composable(Screen.Calendar.route) { CalendarScreen(viewModel) }
        composable(Screen.Templates.route) { TemplatesScreen(viewModel) }
        composable(Screen.Settings.route) { SettingsScreen(viewModel) }
    }
}

@Composable
fun DashboardScreen(viewModel: BirthdayViewModel) {
    val birthdays by viewModel.allBirthdays.collectAsState()
    val isDarkMode by viewModel.isDarkMode.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var selectedBirthday by remember { mutableStateOf<Birthday?>(null) }
    val context = LocalContext.current

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
            Spacer(modifier = Modifier.height(32.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Celebrations", fontSize = 34.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onBackground)
                    Text("Countdown to beautiful moments.", fontSize = 16.sp, color = if(isDarkMode) MutedLavender else SoftGrey)
                }
                // Fixed: Profile picture icon removed from header as requested
            }
            
            Spacer(modifier = Modifier.height(24.dp))

            if (birthdays.isEmpty()) {
                EmptyState(isDarkMode)
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(bottom = 100.dp)
                ) {
                    items(birthdays, key = { it.id }) { birthday ->
                        AdvancedBirthdayCard(
                            birthday = birthday,
                            isDarkMode = isDarkMode,
                            onClick = { selectedBirthday = birthday }
                        )
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = { showAddDialog = true },
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = if(isDarkMode) DarkCharcoal else Color.White,
            modifier = Modifier.align(Alignment.BottomEnd).padding(24.dp),
            shape = RoundedCornerShape(24.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add")
        }
    }

    if (showAddDialog) {
        AdvancedAddDialog(
            isDarkMode = isDarkMode,
            onDismiss = { showAddDialog = false },
            onConfirm = { name, date, uri, time ->
                val savedUri = uri?.let { saveImageToInternalStorage(context, Uri.parse(it)) }
                viewModel.insert(name, date, savedUri?.toString(), time)
                showAddDialog = false
            }
        )
    }

    if (selectedBirthday != null) {
        ProfileDetailModal(
            birthday = selectedBirthday!!,
            isDarkMode = isDarkMode,
            onDismiss = { selectedBirthday = null },
            onDelete = { viewModel.delete(it); selectedBirthday = null }
        )
    }
}

@Composable
fun AdvancedBirthdayCard(birthday: Birthday, isDarkMode: Boolean, onClick: () -> Unit) {
    val countdown = calculateDetailedCountdown(birthday.dateOfBirth)
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (birthday.imageUri != null) {
                AsyncImage(
                    model = birthday.imageUri,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp).clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier.size(64.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(birthday.name.take(1), fontWeight = FontWeight.Bold, fontSize = 24.sp, color = MaterialTheme.colorScheme.primary)
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(birthday.name, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(Icons.Default.Favorite, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                }
                Text(birthday.dateOfBirth, fontSize = 14.sp, color = if(isDarkMode) MutedLavender else SoftGrey)
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${countdown.months}m ${countdown.days}d",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "${countdown.hours}h ${countdown.minutes}m",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if(isDarkMode) SoftGrey else Color.Gray
                )
            }
        }
    }
}

@Composable
fun ProfileDetailModal(birthday: Birthday, isDarkMode: Boolean, onDismiss: () -> Unit, onDelete: (Birthday) -> Unit) {
    val context = LocalContext.current
    val countdown = calculateDetailedCountdown(birthday.dateOfBirth)

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {},
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                if (birthday.imageUri != null) {
                    AsyncImage(
                        model = birthday.imageUri,
                        contentDescription = null,
                        modifier = Modifier.size(120.dp).clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(birthday.name, fontSize = 26.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Text("Birthday: ${birthday.dateOfBirth}", color = if(isDarkMode) MutedLavender else SoftGrey)
                Text("Reminder: ${convertTo12HourFormat(birthday.reminderTime)}", color = if(isDarkMode) MutedLavender else SoftGrey)
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                        .padding(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("${countdown.months} Months, ${countdown.days} Days", fontSize = 24.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                        Text("remaining until celebration", fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    Button(
                        onClick = { shareBirthdayWish(context, birthday) },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Default.Share, "", tint = if(isDarkMode) DarkCharcoal else Color.White)
                        Spacer(Modifier.width(8.dp))
                        Text("Wish", color = if(isDarkMode) DarkCharcoal else Color.White)
                    }
                    IconButton(onClick = { onDelete(birthday) }) {
                        Icon(Icons.Default.Delete, "", tint = Color.Red)
                    }
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(32.dp)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(viewModel: BirthdayViewModel) {
    val birthdays by viewModel.allBirthdays.collectAsState()
    val selectedDate by viewModel.selectedCalendarDate.collectAsState()
    val isDarkMode by viewModel.isDarkMode.collectAsState()
    
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = selectedDate.atStartOfDay(java.time.ZoneOffset.UTC).toInstant().toEpochMilli()
    )

    LaunchedEffect(datePickerState.selectedDateMillis) {
        datePickerState.selectedDateMillis?.let {
            val date = java.time.Instant.ofEpochMilli(it).atZone(java.time.ZoneOffset.UTC).toLocalDate()
            viewModel.setSelectedCalendarDate(date)
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "Calendar",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)
        )
        
        // Fixed: Use a solid Card container with defined elevation and opaque background
        // to prevent the internal year-selection view from overlapping visually with the grid.
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(vertical = 8.dp),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
        ) {
            DatePicker(
                state = datePickerState,
                showModeToggle = false,
                title = null,
                headline = null,
                colors = DatePickerDefaults.colors(
                    // Crucial fix: Ensure the container color is opaque to hide underlying grid
                    containerColor = MaterialTheme.colorScheme.surface,
                    selectedDayContainerColor = MaterialTheme.colorScheme.primary,
                    selectedDayContentColor = if(isDarkMode) DarkCharcoal else Color.White,
                    todayContentColor = MaterialTheme.colorScheme.primary,
                    todayDateBorderColor = MaterialTheme.colorScheme.primary,
                    dayContentColor = MaterialTheme.colorScheme.onSurface,
                    weekdayContentColor = if(isDarkMode) MutedLavender else SoftGrey,
                    yearContentColor = MaterialTheme.colorScheme.onSurface,
                    currentYearContentColor = MaterialTheme.colorScheme.primary,
                    selectedYearContainerColor = MaterialTheme.colorScheme.primary,
                    selectedYearContentColor = if(isDarkMode) DarkCharcoal else Color.White
                ),
                modifier = Modifier.padding(8.dp)
            )
        }
        
        // Birthdays for selected date
        val selectedBirthdays = birthdays.filter { 
            try {
                val dob = LocalDate.parse(it.dateOfBirth)
                dob.month == selectedDate.month && dob.dayOfMonth == selectedDate.dayOfMonth
            } catch(e: Exception) { false }
        }

        if (selectedBirthdays.isNotEmpty()) {
            Text(
                text = "Events on this day:",
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(8.dp)
            )
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.heightIn(max = 200.dp).padding(bottom = 16.dp)
            ) {
                items(selectedBirthdays) { birthday ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
                        )
                    ) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Favorite, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(birthday.name, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        } else {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun BirthdayGreetingCard(birthday: Birthday, wish: String, description: String, isDarkMode: Boolean, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth().aspectRatio(0.75f),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Happy Birthday", fontSize = 32.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary, textAlign = TextAlign.Center)

            Box(
                modifier = Modifier.size(180.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)).padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                if (birthday.imageUri != null) {
                    AsyncImage(
                        model = coil.request.ImageRequest.Builder(LocalContext.current)
                            .data(birthday.imageUri)
                            .allowHardware(false) // Required for capturing bitmap from view
                            .crossfade(true)
                            .build(),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize().clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(Icons.Default.Person, null, modifier = Modifier.size(80.dp), tint = MaterialTheme.colorScheme.primary)
                }
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(50),
                modifier = Modifier.padding(vertical = 8.dp)
            ) {
                Text(birthday.name.uppercase(), color = if(isDarkMode) DarkCharcoal else Color.White, modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp), fontWeight = FontWeight.Bold, fontSize = 20.sp)
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(wish, fontSize = 18.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface, textAlign = TextAlign.Center)
                Spacer(modifier = Modifier.height(8.dp))
                Text(description, fontSize = 14.sp, color = if(isDarkMode) MutedLavender else SoftGrey, textAlign = TextAlign.Center)
            }

            Icon(Icons.Default.Celebration, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
        }
    }
}

@Composable
fun TemplatesScreen(viewModel: BirthdayViewModel) {
    val context = LocalContext.current
    val birthdays by viewModel.allBirthdays.collectAsState()
    val isDarkMode by viewModel.isDarkMode.collectAsState()
    var selectedBirthday by remember { mutableStateOf<Birthday?>(null) }
    var cardView: View? by remember { mutableStateOf(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text("Greeting Card Maker", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                Spacer(modifier = Modifier.height(16.dp))

                if (birthdays.isEmpty()) {
                    EmptyState(isDarkMode)
                } else {
                    Text("Select Person:", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = if(isDarkMode) MutedLavender else SoftGrey)
                    Spacer(Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        birthdays.forEach { birthday ->
                            val isSelected = selectedBirthday?.id == birthday.id
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.clickable { selectedBirthday = birthday }
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(60.dp)
                                        .clip(CircleShape)
                                        .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                                        .padding(if (isSelected) 3.dp else 0.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (birthday.imageUri != null) {
                                        AsyncImage(model = birthday.imageUri, contentDescription = null, modifier = Modifier.fillMaxSize().clip(CircleShape), contentScale = ContentScale.Crop)
                                    } else {
                                        Text(birthday.name.take(1).uppercase(), color = if(isSelected) (if(isDarkMode) DarkCharcoal else Color.White) else MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                    }
                                }
                                Text(birthday.name.split(" ").first(), fontSize = 10.sp, color = if(isSelected) MaterialTheme.colorScheme.primary else SoftGrey)
                            }
                        }
                    }
                }
            }

            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                if (selectedBirthday != null) {
                    AndroidView(
                        factory = { ctx ->
                            ComposeView(ctx).apply {
                                setContent {
                                    BirthdayGreetingCard(
                                        selectedBirthday!!,
                                        "🎉 Wishing you a day filled with love and joy! 🎂✨",
                                        "May your year ahead be as wonderful as you are!",
                                        isDarkMode
                                    )
                                }
                                cardView = this
                            }
                        },
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                        update = {
                            it.setContent {
                                BirthdayGreetingCard(
                                    selectedBirthday!!,
                                    "🎉 Wishing you a day filled with love and joy! 🎂✨",
                                    "May your year ahead be as wonderful as you are!",
                                    isDarkMode
                                )
                            }
                        }
                    )
                } else if (birthdays.isNotEmpty()) {
                    Text(
                        "Select a birthday to create a card",
                        color = SoftGrey,
                        fontSize = 18.sp,
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 40.dp)
                    )
                }
            }

            if (selectedBirthday != null) {
                Button(
                    onClick = { cardView?.let { shareCardAsImage(context, it) } },
                    modifier = Modifier.fillMaxWidth().padding(24.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Default.Share, null, tint = if(isDarkMode) DarkCharcoal else Color.White)
                    Spacer(Modifier.width(8.dp))
                    Text("Share Greeting Card", color = if(isDarkMode) DarkCharcoal else Color.White)
                }
            }
        }
    }
}

@Composable
fun SettingsScreen(viewModel: BirthdayViewModel) {
    val notificationsEnabled by viewModel.notificationsEnabled.collectAsState()
    val isDarkMode by viewModel.isDarkMode.collectAsState()
    val context = LocalContext.current
    var showResetDialog by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        Text("Settings", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
        Spacer(modifier = Modifier.height(24.dp))
        
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SettingToggle("Birthday Notifications", notificationsEnabled) { viewModel.setNotificationsEnabled(it) }
            
            Card(
                modifier = Modifier.fillMaxWidth().clickable { triggerTestNotification(context) },
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.NotificationsActive, "", tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(12.dp))
                    Text("Test Notification", color = MaterialTheme.colorScheme.onSurface)
                }
            }

            SettingToggle("Dark Theme", isDarkMode) { viewModel.toggleDarkMode(it) }
            
            SettingAction("Backup Data", Icons.Default.CloudUpload) { }
            SettingAction("Reset Application", Icons.Default.Restore, Color.Red) { showResetDialog = true }
        }
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Reset Application?") },
            text = { Text("This will permanently delete all saved birthdays and settings.") },
            confirmButton = {
                TextButton(onClick = { viewModel.resetAll(); showResetDialog = false }) { Text("RESET", color = Color.Red) }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) { Text("CANCEL") }
            },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }
}

fun triggerTestNotification(context: Context) {
    val channelId = "test_channel"
    val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val channel = NotificationChannel(channelId, "Test", NotificationManager.IMPORTANCE_HIGH)
        notificationManager.createNotificationChannel(channel)
    }
    val notification = NotificationCompat.Builder(context, channelId)
        .setContentTitle("Test Celebration! 🎉")
        .setContentText("Your birthday reminders are working perfectly.")
        .setSmallIcon(android.R.drawable.ic_dialog_info)
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .build()
    notificationManager.notify(99, notification)
}

fun saveImageToInternalStorage(context: Context, uri: Uri): Uri? {
    return try {
        val inputStream = context.contentResolver.openInputStream(uri)
        val file = File(context.filesDir, "profile_${System.currentTimeMillis()}.png")
        val outputStream = FileOutputStream(file)
        inputStream?.copyTo(outputStream)
        inputStream?.close()
        outputStream.close()
        Uri.fromFile(file)
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

fun shareCardAsImage(context: Context, view: View) {
    try {
        // Fixed: Ensure the view has dimensions before creating bitmap to prevent crash
        if (view.width == 0 || view.height == 0) return

        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        view.draw(canvas)

        val cachePath = File(context.cacheDir, "images")
        cachePath.mkdirs()
        val file = File(cachePath, "birthday_card.png")
        val stream = FileOutputStream(file)
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        stream.close()

        val contentUri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            putExtra(Intent.EXTRA_STREAM, contentUri)
            type = "image/png"
        }
        context.startActivity(Intent.createChooser(shareIntent, "Share Card"))
    } catch (e: Exception) { e.printStackTrace() }
}

fun shareBirthdayWish(context: Context, birthday: Birthday) {
    val wishes = listOf(
        "🎉 Happy Birthday, ${birthday.name}! Wishing you a day filled with love and joy! 🎂✨",
        "🎂 Have a blast, ${birthday.name}! 🌟🎁",
        "🎈 Sending lots of love on your special day, ${birthday.name}! 🥳💖"
    )
    val randomWish = wishes[Random.nextInt(wishes.size)]
    val sendIntent: Intent = Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(Intent.EXTRA_TEXT, randomWish)
        type = "text/plain"
    }
    context.startActivity(Intent.createChooser(sendIntent, null))
}

data class DetailedCountdown(val months: Long, val days: Long, val hours: Long, val minutes: Long)

fun calculateDetailedCountdown(dobString: String): DetailedCountdown {
    return try {
        val dob = LocalDate.parse(dobString)
        val now = LocalDateTime.now()
        var nextBirthday = dob.withYear(now.year).atTime(9, 0)
        if (nextBirthday.isBefore(now)) nextBirthday = nextBirthday.plusYears(1)
        
        val totalMinutes = ChronoUnit.MINUTES.between(now, nextBirthday)
        val months = totalMinutes / (30 * 24 * 60)
        val days = (totalMinutes % (30 * 24 * 60)) / (24 * 60)
        val hours = (totalMinutes % (24 * 60)) / 60
        val minutes = totalMinutes % 60
        DetailedCountdown(months, days, hours, minutes)
    } catch (e: Exception) { DetailedCountdown(0,0,0,0) }
}

fun convertTo12HourFormat(time24: String): String {
    return try {
        val parts = time24.split(":")
        var h = parts[0].toInt()
        val m = parts[1]
        val suffix = if (h >= 12) "PM" else "AM"
        h = if (h == 0) 12 else if (h > 12) h - 12 else h
        "$h:$m $suffix"
    } catch (e: Exception) { time24 }
}

@Composable
fun SettingToggle(title: String, checked: Boolean, enabled: Boolean = true, onCheckedChange: (Boolean) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(title, fontWeight = FontWeight.Medium, color = if(enabled) MaterialTheme.colorScheme.onSurface else SoftGrey)
            Switch(checked = checked, onCheckedChange = onCheckedChange, enabled = enabled)
        }
    }
}

@Composable
fun SettingAction(title: String, icon: ImageVector, color: Color = OffWhite, onClick: () -> Unit = {}) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, "", tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(12.dp))
                Text(title, fontWeight = FontWeight.Medium, color = if(color == OffWhite) MaterialTheme.colorScheme.onSurface else color)
            }
            Icon(Icons.Default.ChevronRight, "", tint = SoftGrey)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdvancedAddDialog(isDarkMode: Boolean, onDismiss: () -> Unit, onConfirm: (String, String, String?, String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var date by remember { mutableStateOf("") }
    var hour by remember { mutableStateOf("09") }
    var minute by remember { mutableStateOf("00") }
    var isAm by remember { mutableStateOf(true) }
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { imageUri = it }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Birthday", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface) },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(modifier = Modifier.size(80.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)).clickable { launcher.launch("image/*") }, contentAlignment = Alignment.Center) {
                    if (imageUri != null) AsyncImage(model = imageUri, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                    else Icon(Icons.Default.AddAPhoto, "", tint = MaterialTheme.colorScheme.primary)
                }
                Spacer(modifier = Modifier.height(16.dp))
                TextField(value = name, onValueChange = { name = it }, label = { Text("Name") }, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(8.dp))
                TextField(value = date, onValueChange = { date = it }, label = { Text("Date (YYYY-MM-DD)") }, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(16.dp))
                Text("Reminder Time", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextField(value = hour, onValueChange = { if(it.length <= 2) hour = it }, modifier = Modifier.width(60.dp), textStyle = TextStyle(textAlign = TextAlign.Center))
                    Text(" : ", color = MaterialTheme.colorScheme.onSurface)
                    TextField(value = minute, onValueChange = { if(it.length <= 2) minute = it }, modifier = Modifier.width(60.dp), textStyle = TextStyle(textAlign = TextAlign.Center))
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = { isAm = !isAm }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))) {
                        Text(if (isAm) "AM" else "PM", color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { if (name.isNotBlank() && date.isNotBlank()) onConfirm(name, date, imageUri?.toString(), formatTo24Hour(hour, minute, isAm)) }) {
                Text("Save", color = if(isDarkMode) DarkCharcoal else Color.White)
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(32.dp)
    )
}

@Composable
fun ModernBottomBar(navController: NavHostController, isDarkMode: Boolean) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val items = listOf(Screen.Dashboard, Screen.Calendar, Screen.Templates, Screen.Settings)

    NavigationBar(containerColor = MaterialTheme.colorScheme.surface, tonalElevation = 0.dp) {
        items.forEach { screen ->
            val isSelected = currentDestination?.hierarchy?.any { it.route == screen.route } == true
            NavigationBarItem(
                selected = isSelected,
                onClick = { navController.navigate(screen.route) { popUpTo(navController.graph.findStartDestination().id) { saveState = true }; launchSingleTop = true; restoreState = true } },
                icon = { Icon(screen.icon, null, tint = if (isSelected) MaterialTheme.colorScheme.primary else SoftGrey) },
                label = { Text(screen.label, fontSize = 10.sp, color = if (isSelected) MaterialTheme.colorScheme.primary else SoftGrey) }
            )
        }
    }
}

@Composable
fun EmptyState(isDarkMode: Boolean) {
    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.Default.AutoAwesome, "", modifier = Modifier.size(80.dp), tint = if(isDarkMode) MutedLavender.copy(alpha = 0.3f) else SoftGrey.copy(alpha = 0.3f))
        Text("No celebrations yet.", fontWeight = FontWeight.Bold, color = SoftGrey)
    }
}

fun formatTo24Hour(hour: String, minute: String, isAm: Boolean): String {
    var h = hour.toIntOrNull() ?: 0
    val m = minute.toIntOrNull() ?: 0
    if (isAm) { if (h == 12) h = 0 } else { if (h != 12) h += 12 }
    return String.format("%02d:%02d", h, m)
}

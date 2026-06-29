package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.HowToReg
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.local.AppDatabase
import com.example.data.remote.AttendanceApiService
import com.example.data.repository.AttendanceRepository
import com.example.ui.MainViewModel
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.RegisterScreen
import com.example.ui.screens.ScanScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.MyApplicationTheme

class MainActivity : androidx.fragment.app.FragmentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      MyApplicationTheme {
        MainAppScreen()
      }
    }
  }
}

enum class ScreenTab {
  SCAN, REGISTER, HISTORY, SETTINGS
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScreen() {
  val context = LocalContext.current

  // Initialize Room Database, Retrofit, and Repository
  val database = remember { AppDatabase.getDatabase(context) }
  val apiService = remember { AttendanceApiService.create() }
  val repository = remember {
    AttendanceRepository(
      fingerprintDao = database.fingerprintDao(),
      attendanceLogDao = database.attendanceLogDao(),
      apiService = apiService
    )
  }

  // ViewModel initialization via Factory
  val mainViewModel: MainViewModel = viewModel(
    factory = MainViewModel.Factory(repository)
  )

  var currentTab by remember { mutableStateOf(ScreenTab.SCAN) }

  Scaffold(
    modifier = Modifier.fillMaxSize(),
    containerColor = MaterialTheme.colorScheme.background,
    topBar = {
      TopAppBar(
        title = {
          Column {
            Text(
              text = "BioSecure",
              fontWeight = FontWeight.Bold,
              style = MaterialTheme.typography.titleLarge,
              letterSpacing = (-0.5).sp
            )
            Text(
              text = "ATTENDANCE SYSTEM V2.4",
              style = MaterialTheme.typography.labelSmall,
              fontWeight = FontWeight.ExtraBold,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              letterSpacing = 1.5.sp
            )
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(
          containerColor = Color(0x1F0F172A), // Sleek translucent dark top bar
          titleContentColor = Color.White
        )
      )
    },
    bottomBar = {
      // Floating frosted-glass action navigation tab bar
      Box(
        modifier = Modifier
          .padding(start = 16.dp, end = 16.dp, bottom = 24.dp)
          .background(Color(0x12FFFFFF), shape = RoundedCornerShape(24.dp))
          .border(1.dp, Color(0x1AFFFFFF), shape = RoundedCornerShape(24.dp))
          .padding(horizontal = 4.dp, vertical = 2.dp)
      ) {
        NavigationBar(
          modifier = Modifier.testTag("bottom_nav_bar"),
          containerColor = Color.Transparent,
          tonalElevation = 0.dp
        ) {
          NavigationBarItem(
            selected = currentTab == ScreenTab.SCAN,
            onClick = { currentTab = ScreenTab.SCAN },
            colors = NavigationBarItemDefaults.colors(
              selectedIconColor = MaterialTheme.colorScheme.primary,
              selectedTextColor = MaterialTheme.colorScheme.primary,
              unselectedIconColor = Color(0xFF94A3B8),
              unselectedTextColor = Color(0xFF94A3B8),
              indicatorColor = Color(0x266366F1)
            ),
            icon = {
              Icon(
                imageVector = Icons.Default.Fingerprint,
                contentDescription = "SCAN",
                modifier = Modifier.size(24.dp)
              )
            },
            label = { Text("SCAN", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold) },
            modifier = Modifier.testTag("nav_tab_scan")
          )

          NavigationBarItem(
            selected = currentTab == ScreenTab.REGISTER,
            onClick = { currentTab = ScreenTab.REGISTER },
            colors = NavigationBarItemDefaults.colors(
              selectedIconColor = MaterialTheme.colorScheme.primary,
              selectedTextColor = MaterialTheme.colorScheme.primary,
              unselectedIconColor = Color(0xFF94A3B8),
              unselectedTextColor = Color(0xFF94A3B8),
              indicatorColor = Color(0x266366F1)
            ),
            icon = {
              Icon(
                imageVector = Icons.Default.HowToReg,
                contentDescription = "REGISTRASI",
                modifier = Modifier.size(24.dp)
              )
            },
            label = { Text("USERS", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold) },
            modifier = Modifier.testTag("nav_tab_register")
          )

          NavigationBarItem(
            selected = currentTab == ScreenTab.HISTORY,
            onClick = { currentTab = ScreenTab.HISTORY },
            colors = NavigationBarItemDefaults.colors(
              selectedIconColor = MaterialTheme.colorScheme.primary,
              selectedTextColor = MaterialTheme.colorScheme.primary,
              unselectedIconColor = Color(0xFF94A3B8),
              unselectedTextColor = Color(0xFF94A3B8),
              indicatorColor = Color(0x266366F1)
            ),
            icon = {
              Icon(
                imageVector = Icons.Default.History,
                contentDescription = "LOGS",
                modifier = Modifier.size(24.dp)
              )
            },
            label = { Text("LOGS", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold) },
            modifier = Modifier.testTag("nav_tab_history")
          )

          NavigationBarItem(
            selected = currentTab == ScreenTab.SETTINGS,
            onClick = { currentTab = ScreenTab.SETTINGS },
            colors = NavigationBarItemDefaults.colors(
              selectedIconColor = MaterialTheme.colorScheme.primary,
              selectedTextColor = MaterialTheme.colorScheme.primary,
              unselectedIconColor = Color(0xFF94A3B8),
              unselectedTextColor = Color(0xFF94A3B8),
              indicatorColor = Color(0x266366F1)
            ),
            icon = {
              Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = "ADMIN",
                modifier = Modifier.size(24.dp)
              )
            },
            label = { Text("ADMIN", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold) },
            modifier = Modifier.testTag("nav_tab_settings")
          )
        }
      }
    }
  ) { innerPadding ->
    val modifier = Modifier.padding(innerPadding)
    when (currentTab) {
      ScreenTab.SCAN -> ScanScreen(viewModel = mainViewModel, modifier = modifier)
      ScreenTab.REGISTER -> RegisterScreen(viewModel = mainViewModel, modifier = modifier)
      ScreenTab.HISTORY -> HistoryScreen(viewModel = mainViewModel, modifier = modifier)
      ScreenTab.SETTINGS -> SettingsScreen(viewModel = mainViewModel, modifier = modifier)
    }
  }
}


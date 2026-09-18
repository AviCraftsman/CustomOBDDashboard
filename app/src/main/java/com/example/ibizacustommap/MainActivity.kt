package com.example.ibizacustommap

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.activity.compose.rememberLauncherForActivityResult

class MainActivity : ComponentActivity() {

    // 1. PERMISSIONS SET: Depending on the phone's OS version, request different ones.
    private val requiredPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        arrayOf(
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
    } else {
        arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            // Variable that tracks whether we have already gotten a "Yes"
            var permissionsGranted by remember { mutableStateOf(checkPermissions()) }

            // Launcher for the Android permission pop-up
            val permissionLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestMultiplePermissions()
            ) { permissionsMap ->
                // Check whether the user has accepted ALL the permissions we requested
                permissionsGranted = permissionsMap.values.all { it == true }
            }

            // Call the visual screen passing it the state and the button to request permissions
            MainAppScreen(
                permissionsGranted = permissionsGranted,
                onRequestPermissions = { permissionLauncher.launch(requiredPermissions) }
            )
        }
    }

    // Function that silently checks the status of the permissions
    private fun checkPermissions(): Boolean {
        return requiredPermissions.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
    }
}

// ====================================================================
// THE SCREEN DESIGN (Minimalist Black/White)
// ====================================================================
@Composable
fun MainAppScreen(permissionsGranted: Boolean, onRequestPermissions: () -> Unit) {

    val backgroundColor = Color(0xFF040404) // Near-absolute black
    val textColor = Color(0xFFF0F0F0)       // Off-white
    val mutedText = Color(0xFF7A7A7A)       // Subtle gray for status text
    val seatRed = Color(0xFFB40000)         // Kept only for the permissions button

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // NEW SEAT LOGO (No additional text)
        androidx.compose.foundation.Image(
            painter = androidx.compose.ui.res.painterResource(id = R.drawable.seat_logo),
            contentDescription = "Seat Logo",
            modifier = Modifier.fillMaxWidth(0.7f), // Will occupy 70% of the screen width
            // If you want the logo (silver S and red letters) to become 100% PURE WHITE
            // to keep the strict aesthetic, uncomment the following line by removing the two slashes (//):
            // colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(Color.White)
        )

        Spacer(modifier = Modifier.height(80.dp))

        // UI LOGIC
        if (permissionsGranted) {
            Text(
                text = "WAITING FOR OBD2 CONNECTION...",
                color = textColor,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "System permissions: OK",
                color = mutedText,
                fontSize = 12.sp
            )
        } else {
            Text(
                text = "ACCESS REQUIRED",
                color = textColor, // Changed to white to keep it understated
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "To interface with the ECU and connect to the OBD2 adapter, enabling Bluetooth access is essential.",
                color = mutedText,
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = onRequestPermissions,
                colors = ButtonDefaults.buttonColors(containerColor = seatRed),
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .height(50.dp)
            ) {
                Text(
                    text = "LINK SYSTEMS",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }
        }
    }
}
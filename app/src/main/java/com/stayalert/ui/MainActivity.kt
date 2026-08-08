package com.stayalert.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.stayalert.data.DataStoreSettingsRepository
import com.stayalert.ui.components.ResponsibleUseNotice
import com.stayalert.ui.theme.Accent
import com.stayalert.ui.theme.AccentOn
import com.stayalert.ui.theme.InkDisabled
import com.stayalert.ui.theme.SurfaceBase
import com.stayalert.ui.theme.StayAlertTheme
import com.stayalert.ui.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {

    private val settingsRepository by lazy {
        DataStoreSettingsRepository(applicationContext)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            StayAlertTheme {
                val viewModel: MainViewModel = viewModel(
                    factory = MainViewModel.Factory(settingsRepository)
                )
                MainScreen(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun MainScreen(viewModel: MainViewModel) {
    val noticeAccepted by viewModel.noticeAccepted.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "stayAlert",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(48.dp))

        Button(
            onClick = { /* no-op: sesión no implementada aún */ },
            enabled = noticeAccepted,
            shape = RoundedCornerShape(999.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Accent,
                contentColor = AccentOn,
                disabledContainerColor = InkDisabled,
                disabledContentColor = SurfaceBase
            )
        ) {
            Text(
                text = "Iniciar Jornada",
                style = MaterialTheme.typography.labelLarge
            )
        }
    }

    if (!noticeAccepted) {
        ResponsibleUseNotice(onAccept = viewModel::acceptNotice)
    }
}

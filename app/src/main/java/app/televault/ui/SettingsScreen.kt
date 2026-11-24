package app.televault.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.biometric.BiometricManager
import app.televault.MainViewModel
import app.televault.data.Vault

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(vm: MainViewModel) {
    val ctx = LocalContext.current
    val index by Vault.index.collectAsStateWithLifecycle()
    var confirm by remember { mutableStateOf("") }
    val total = index.files.sumOf { it.size }

    Scaffold(topBar = { TopAppBar(title = { Text("Settings") }) }) { pad ->
        Column(
            Modifier.padding(pad).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Card("Connected") {
                Text("🤖 @${vm.botName}")
                Text("💬 ${vm.chatTitle}")
                Text("🗂 ${index.files.size} files • ${formatSize(ctx, total)}")
            }
            Card("Security") {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Text("Lock app (fingerprint / PIN)")
                    Switch(checked = vm.lockEnabled, onCheckedChange = { on ->
                        if (!on) vm.setLock(false) else {
                            val ok = BiometricManager.from(ctx).canAuthenticate(
                                BiometricManager.Authenticators.BIOMETRIC_WEAK or BiometricManager.Authenticators.DEVICE_CREDENTIAL
                            ) == BiometricManager.BIOMETRIC_SUCCESS
                            if (ok) vm.setLock(true) else vm.toast("Set up a screen lock on your phone first")
                        }
                    })
                }
            }
            Card("Index backup") {
                Text("The list of your files is stored on this phone. A backup copy is pinned in your Telegram chat so you can restore it after reinstalling.", style = MaterialTheme.typography.bodySmall)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Text("Auto-backup after uploads")
                    Switch(checked = vm.autoBackup, onCheckedChange = { vm.updateAutoBackup(it) })
                }
                OutlinedButton(onClick = { vm.backupNow() }, modifier = Modifier.fillMaxWidth()) { Text("Back up now") }
                OutlinedButton(onClick = { confirm = "restore" }, modifier = Modifier.fillMaxWidth()) { Text("Restore from Telegram") }
            }
            Card("Files already in your chat") {
                Text("TeleVault picks up files posted to your chat and puts them in the Imported folder, where you can move, rename or delete them. Quick scan sees recent posts; Deep scan walks the whole history.", style = MaterialTheme.typography.bodySmall)
                OutlinedButton(onClick = { vm.scanNewPosts() }, enabled = !vm.scanning, modifier = Modifier.fillMaxWidth()) { Text("Scan for new files") }
                OutlinedButton(onClick = { vm.deepScan() }, enabled = !vm.scanning, modifier = Modifier.fillMaxWidth()) { Text("Deep scan older files") }
            }
            Card("Good to know") {
                Text("• Files over 18 MB are split into parts automatically and re-joined on download.", style = MaterialTheme.typography.bodySmall)
                Text("• Telegram limits bots to deleting messages younger than 48 h (unless the bot is an admin with \"Delete messages\" permission). Otherwise the file is replaced by a tiny placeholder message.", style = MaterialTheme.typography.bodySmall)
                Text("• Files are not end-to-end encrypted – keep the chat private.", style = MaterialTheme.typography.bodySmall)
            }
            OutlinedButton(onClick = { confirm = "disconnect" }, modifier = Modifier.fillMaxWidth()) { Text("Disconnect") }
        }
    }

    if (confirm == "restore") ConfirmDialog("Restore index?", "This replaces the file list on this phone with the pinned backup.", { confirm = "" }) {
        vm.restoreNow(); confirm = ""
    }
    if (confirm == "disconnect") ConfirmDialog("Disconnect?", "Local settings and the file list are cleared. Your files stay in Telegram.", { confirm = "" }) {
        vm.disconnect(); confirm = ""
    }
}

@Composable
private fun Card(title: String, content: @Composable () -> Unit) {
    Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            content()
        }
    }
}

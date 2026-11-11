import re

# 1. Update PlacementRepository.kt
repo_path = r"d:\6th Sem\Project\AuraFace_App\app\src\main\java\com\auraface\auraface_app\data\repository\PlacementRepository.kt"
with open(repo_path, 'r', encoding='utf-8') as f: repo = f.read()

repo = repo.replace(
    'fun addInternship(company: String, role: String, startDate: String, documentUrl: String? = null) =\n        api.addInternship(mapOf("company_name" to company, "role" to role, "start_date" to startDate, "document_url" to (documentUrl ?: "")))',
    'fun addInternship(company: String, role: String, startDate: String, endDate: String, documentUrl: String? = null) =\n        api.addInternship(mapOf("company_name" to company, "role" to role, "start_date" to startDate, "end_date" to endDate, "certificate_url" to (documentUrl ?: "")))'
)

repo = repo.replace(
    'fun editInternship(id: Int, company: String, role: String, startDate: String, documentUrl: String? = null) =\n        api.editInternship(id, mapOf("company_name" to company, "role" to role, "start_date" to startDate, "document_url" to (documentUrl ?: "")))',
    'fun editInternship(id: Int, company: String, role: String, startDate: String, endDate: String, documentUrl: String? = null) =\n        api.editInternship(id, mapOf("company_name" to company, "role" to role, "start_date" to startDate, "end_date" to endDate, "certificate_url" to (documentUrl ?: "")))'
)

with open(repo_path, 'w', encoding='utf-8') as f: f.write(repo)

# 2. Update PlacementViewModel.kt
vm_path = r"d:\6th Sem\Project\AuraFace_App\app\src\main\java\com\auraface\auraface_app\presentation\placement\PlacementViewModel.kt"
with open(vm_path, 'r', encoding='utf-8') as f: vm = f.read()

vm = vm.replace(
    'fun addInternship(company: String, role: String, startDate: String, file: java.io.File? = null) {',
    'fun addInternship(company: String, role: String, startDate: String, endDate: String, file: java.io.File? = null) {'
)
vm = vm.replace(
    'repository.addInternship(company, role, startDate, url)',
    'repository.addInternship(company, role, startDate, endDate, url)'
)

vm = vm.replace(
    'fun editInternship(id: Int, company: String, role: String, startDate: String, file: java.io.File? = null) {',
    'fun editInternship(id: Int, company: String, role: String, startDate: String, endDate: String, file: java.io.File? = null) {'
)
vm = vm.replace(
    'repository.editInternship(id, company, role, startDate, url)',
    'repository.editInternship(id, company, role, startDate, endDate, url)'
)

with open(vm_path, 'w', encoding='utf-8') as f: f.write(vm)

# 3. Update PlacementScreen.kt
screen_path = r"d:\6th Sem\Project\AuraFace_App\app\src\main\java\com\auraface\auraface_app\presentation\placement\PlacementScreen.kt"
with open(screen_path, 'r', encoding='utf-8') as f: screen = f.read()

# Fix the call in ReadinessContent
screen = screen.replace(
    '{ company, role, date, file ->\n                if (editInternData != null) {\n                    viewModel.editInternship(editInternData!!.id, company, role, date, file)\n                } else {\n                    viewModel.addInternship(company, role, date, file)\n                }',
    '{ company, role, startDate, endDate, file ->\n                if (editInternData != null) {\n                    viewModel.editInternship(editInternData!!.id, company, role, startDate, endDate, file)\n                } else {\n                    viewModel.addInternship(company, role, startDate, endDate, file)\n                }'
)

# Fix AddInternDialog definition
target_dialog = """@Composable
fun AddInternDialog(existing: com.auraface.auraface_app.data.remote.dto.PlacementInternshipDto? = null, onDismiss: () -> Unit, onConfirm: (String, String, String, java.io.File?) -> Unit) {
    var company by remember { mutableStateOf(existing?.company_name ?: "") }
    var role by remember { mutableStateOf(existing?.role ?: "") }
    var date by remember { mutableStateOf(if (existing?.start_date != null) existing.start_date.take(10) else "2024-01-01") } // Placeholder, need DatePicker ideally
    var selectedFile by remember { mutableStateOf<android.net.Uri?>(null) }"""

new_dialog = """@Composable
fun AddInternDialog(existing: com.auraface.auraface_app.data.remote.dto.PlacementInternshipDto? = null, onDismiss: () -> Unit, onConfirm: (String, String, String, String, java.io.File?) -> Unit) {
    var company by remember { mutableStateOf(existing?.company_name ?: "") }
    var role by remember { mutableStateOf(existing?.role ?: "") }
    var startDate by remember { mutableStateOf(if (existing?.start_date != null) existing.start_date.take(10) else "2024-01-01") } 
    var endDate by remember { mutableStateOf(if (existing?.end_date != null) existing.end_date.take(10) else "") } 
    var selectedFile by remember { mutableStateOf<android.net.Uri?>(null) }"""

screen = screen.replace(target_dialog, new_dialog)

target_dialog_ui = """                OutlinedTextField(value = date, onValueChange = { date = it }, label = { Text("Start Date (YYYY-MM-DD)") }, placeholder = { Text("2024-01-01") })
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = { launcher.launch("*/*") }) {"""

new_dialog_ui = """                OutlinedTextField(value = startDate, onValueChange = { startDate = it }, label = { Text("Start Date (YYYY-MM-DD)") }, placeholder = { Text("2024-01-01") })
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = endDate, onValueChange = { endDate = it }, label = { Text("End Date (Optional)") }, placeholder = { Text("2024-06-01") })
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = { launcher.launch("*/*") }) {"""

screen = screen.replace(target_dialog_ui, new_dialog_ui)

target_dialog_confirm = """            TextButton(onClick = { 
                if(name.isNotBlank()) {
                    val file = generateTempUploadFile(context, selectedFile)
                    onConfirm(name, issuer, file)
                }
            })"""

# Wait, wait, intern dialog uses company! Let's be precise.
target_intern_confirm = """            TextButton(onClick = { 
                if(company.isNotBlank()) {
                    val file = generateTempUploadFile(context, selectedFile)
                    onConfirm(company, role, date, file)
                }
            })"""

new_intern_confirm = """            TextButton(onClick = { 
                if(company.isNotBlank()) {
                    val file = generateTempUploadFile(context, selectedFile)
                    onConfirm(company, role, startDate, endDate, file)
                }
            })"""

screen = screen.replace(target_intern_confirm, new_intern_confirm)

# Fix project pending completed UI
# PremiumListItem isPending logic
target_pending = 'val isPending = trailingText.contains("Pending", ignoreCase = true)'
new_pending = 'val isPending = trailingText.contains("Pending", ignoreCase = true) || trailingText.contains("Ongoing", ignoreCase = true)'

target_approved = 'val isApproved = trailingText.contains("Approved", ignoreCase = true) || trailingText.contains("Verified", ignoreCase = true)'
new_approved = 'val isApproved = trailingText.contains("Approved", ignoreCase = true) || trailingText.contains("Verified", ignoreCase = true) || trailingText.contains("Completed", ignoreCase = true)'

screen = screen.replace(target_pending, new_pending)
screen = screen.replace(target_approved, new_approved)

with open(screen_path, 'w', encoding='utf-8') as f: f.write(screen)

print("Done updating intern and project features.")

import re
import sys

filepath = r"d:\6th Sem\Project\AuraFace_App\app\src\main\java\com\auraface\auraface_app\presentation\placement\PlacementScreen.kt"
with open(filepath, 'r', encoding='utf-8') as f:
    content = f.read()

# 1. Add Edit states to PlacementReadinessScreen
state_additions = """    // Dialog States
    var showSkillDialog by remember { mutableStateOf(false) }
    var editSkillData by remember { mutableStateOf<com.auraface.auraface_app.data.remote.dto.PlacementSkillDto?>(null) }
    
    var showProjectDialog by remember { mutableStateOf(false) }
    var editProjectData by remember { mutableStateOf<com.auraface.auraface_app.data.remote.dto.PlacementProjectDto?>(null) }
    
    var showCertDialog by remember { mutableStateOf(false) }
    var editCertData by remember { mutableStateOf<com.auraface.auraface_app.data.remote.dto.PlacementCertificationDto?>(null) }
    
    var showInternDialog by remember { mutableStateOf(false) }
    var editInternData by remember { mutableStateOf<com.auraface.auraface_app.data.remote.dto.PlacementInternshipDto?>(null) }
    
    var showEventDialog by remember { mutableStateOf(false) }
    var editEventData by remember { mutableStateOf<com.auraface.auraface_app.data.remote.dto.PlacementEventDto?>(null) }"""

content = re.sub(
    r"    // Dialog States\s+var showSkillDialog.*?\n    var showEventDialog by remember { mutableStateOf\(false\) }",
    state_additions,
    content,
    flags=re.DOTALL
)

# 2. Update ReadinessContent calls
readiness_content_old = """                    ReadinessContent(
                        data = s.data,
                        onAddSkill = { showSkillDialog = true },
                        onAddProject = { showProjectDialog = true },
                        onAddCert = { showCertDialog = true },
                        onAddIntern = { showInternDialog = true },
                        onAddEvent = { showEventDialog = true },
                        onDeleteSkill = { viewModel.deleteSkill(it) },
                        onDeleteProject = { viewModel.deleteProject(it) },
                        onDeleteCert = { viewModel.deleteCertification(it) },
                        onDeleteIntern = { viewModel.deleteInternship(it) },
                        onDeleteEvent = { viewModel.deleteEvent(it) }
                    )"""

readiness_content_new = """                    ReadinessContent(
                        data = s.data,
                        onAddSkill = { showSkillDialog = true },
                        onAddProject = { showProjectDialog = true },
                        onAddCert = { showCertDialog = true },
                        onAddIntern = { showInternDialog = true },
                        onAddEvent = { showEventDialog = true },
                        onDeleteSkill = { viewModel.deleteSkill(it) },
                        onDeleteProject = { viewModel.deleteProject(it) },
                        onDeleteCert = { viewModel.deleteCertification(it) },
                        onDeleteIntern = { viewModel.deleteInternship(it) },
                        onDeleteEvent = { viewModel.deleteEvent(it) },
                        onEditSkill = { editSkillData = it },
                        onEditProject = { editProjectData = it },
                        onEditCert = { editCertData = it },
                        onEditIntern = { editInternData = it },
                        onEditEvent = { editEventData = it }
                    )"""
content = content.replace(readiness_content_old, readiness_content_new)

# 3. Update ReadinessContent definition
content = content.replace("onDeleteEvent: (Int) -> Unit", "onDeleteEvent: (Int) -> Unit,\n    onEditSkill: (com.auraface.auraface_app.data.remote.dto.PlacementSkillDto) -> Unit,\n    onEditProject: (com.auraface.auraface_app.data.remote.dto.PlacementProjectDto) -> Unit,\n    onEditCert: (com.auraface.auraface_app.data.remote.dto.PlacementCertificationDto) -> Unit,\n    onEditIntern: (com.auraface.auraface_app.data.remote.dto.PlacementInternshipDto) -> Unit,\n    onEditEvent: (com.auraface.auraface_app.data.remote.dto.PlacementEventDto) -> Unit")

# 4. Pass onEdit from LazyColumn items to PremiumListItem
content = content.replace(
    "onDelete = { onDeleteSkill(skill.id) }",
    "onDelete = { onDeleteSkill(skill.id) },\n                onEdit = { onEditSkill(skill) }"
)
content = content.replace(
    "onDelete = { onDeleteProject(proj.id) }",
    "onDelete = { onDeleteProject(proj.id) },\n                onEdit = { onEditProject(proj) }"
)
content = content.replace(
    "onDelete = { onDeleteCert(cert.id) }",
    "onDelete = { onDeleteCert(cert.id) },\n                onEdit = { onEditCert(cert) }"
)
content = content.replace(
    "onDelete = { onDeleteIntern(intern.id) }",
    "onDelete = { onDeleteIntern(intern.id) },\n                onEdit = { onEditIntern(intern) }"
)
content = content.replace(
    "onDelete = { onDeleteEvent(event.id) }",
    "onDelete = { onDeleteEvent(event.id) },\n                onEdit = { onEditEvent(event) }"
)

def fix_dialog(content, func_name, obj_type, fields, view_model_add, view_model_edit, ui_state_show, ui_state_edit):
    # This edits the call inside PlacementReadinessScreen
    
    old_call = f"""        if ({ui_state_show}) {{
            {func_name}(onDismiss = {{ {ui_state_show} = false }}) {{ {', '.join(fields)}, file ->
                viewModel.{view_model_add}({', '.join(fields)}, file)
                {ui_state_show} = false
            }}
        }}"""
    
    new_call = f"""        if ({ui_state_show} || {ui_state_edit} != null) {{
            {func_name}(
                existing = {ui_state_edit},
                onDismiss = {{ {ui_state_show} = false; {ui_state_edit} = null }}
            ) {{ {', '.join(fields)}, file ->
                if ({ui_state_edit} != null) {{
                    viewModel.{view_model_edit}({ui_state_edit}!!.id, {', '.join(fields)}, file)
                }} else {{
                    viewModel.{view_model_add}({', '.join(fields)}, file)
                }}
                {ui_state_show} = false
                {ui_state_edit} = null
            }}
        }}"""
    return content.replace(old_call, new_call)

content = fix_dialog(content, "AddSkillDialog", "PlacementSkillDto", ["name", "level"], "addSkill", "editSkill", "showSkillDialog", "editSkillData")
content = fix_dialog(content, "AddProjectDialog", "PlacementProjectDto", ["title", "desc", "stack"], "addProject", "editProject", "showProjectDialog", "editProjectData")
content = fix_dialog(content, "AddCertDialog", "PlacementCertificationDto", ["name", "issuer"], "addCertification", "editCertification", "showCertDialog", "editCertData")
content = fix_dialog(content, "AddInternDialog", "PlacementInternshipDto", ["company", "role", "date"], "addInternship", "editInternship", "showInternDialog", "editInternData")
content = fix_dialog(content, "AddEventDialog", "PlacementEventDto", ["name", "type", "date"], "addEvent", "editEvent", "showEventDialog", "editEventData")

# 5. Fix Dialog Definitions and Temporary file generation
import textwrap

helper = """
fun generateTempUploadFile(context: android.content.Context, uri: android.net.Uri?): java.io.File? {
    if (uri == null) return null
    return try {
        val contentResolver = context.contentResolver
        val inputStream = contentResolver.openInputStream(uri)
        val mimeType = contentResolver.getType(uri)
        val extension = android.webkit.MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType)
        val suffix = if (extension != null) ".$extension" else ".tmp"
        val tempFile = java.io.File.createTempFile("upload_", suffix, context.cacheDir)
        tempFile.outputStream().use { output ->
            inputStream?.copyTo(output)
        }
        tempFile
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}
"""

content += helper

content = re.sub(r'var file: java\.io\.File\? = null\s+if \(selectedFile != null\) {[^}]+try {.*?catch \(e: Exception\) {.*?}\s+}', 'val file = generateTempUploadFile(context, selectedFile)', content, flags=re.DOTALL)

# Now fix the signatures of dialogs
content = re.sub(
    r'@Composable\nfun AddSkillDialog\(onDismiss: \(\) -> Unit, onConfirm: \(String, String, java\.io\.File\?\) -> Unit\) {.*?var name by remember { mutableStateOf\(""\) }.*?var level by remember { mutableStateOf\("Beginner"\) }',
    '@Composable\nfun AddSkillDialog(existing: com.auraface.auraface_app.data.remote.dto.PlacementSkillDto? = null, onDismiss: () -> Unit, onConfirm: (String, String, java.io.File?) -> Unit) {\n    var name by remember { mutableStateOf(existing?.skill_name ?: "") }\n    var level by remember { mutableStateOf(existing?.proficiency ?: "Beginner") }',
    content, flags=re.DOTALL
)

content = re.sub(
    r'@Composable\nfun AddProjectDialog\(onDismiss: \(\) -> Unit, onConfirm: \(String, String, String, java\.io\.File\?\) -> Unit\) {.*?var title by remember { mutableStateOf\(""\) }.*?var desc by remember { mutableStateOf\(""\) }.*?var stack by remember { mutableStateOf\(""\) }',
    '@Composable\nfun AddProjectDialog(existing: com.auraface.auraface_app.data.remote.dto.PlacementProjectDto? = null, onDismiss: () -> Unit, onConfirm: (String, String, String, java.io.File?) -> Unit) {\n    var title by remember { mutableStateOf(existing?.title ?: "") }\n    var desc by remember { mutableStateOf(existing?.description ?: "") }\n    var stack by remember { mutableStateOf(existing?.tech_stack ?: "") }',
    content, flags=re.DOTALL
)

content = re.sub(
    r'@Composable\nfun AddCertDialog\(onDismiss: \(\) -> Unit, onConfirm: \(String, String, java\.io\.File\?\) -> Unit\) {.*?var name by remember { mutableStateOf\(""\) }.*?var issuer by remember { mutableStateOf\(""\) }',
    '@Composable\nfun AddCertDialog(existing: com.auraface.auraface_app.data.remote.dto.PlacementCertificationDto? = null, onDismiss: () -> Unit, onConfirm: (String, String, java.io.File?) -> Unit) {\n    var name by remember { mutableStateOf(existing?.name ?: "") }\n    var issuer by remember { mutableStateOf(existing?.issuing_org ?: "") }',
    content, flags=re.DOTALL
)

content = re.sub(
    r'@Composable\nfun AddInternDialog\(onDismiss: \(\) -> Unit, onConfirm: \(String, String, String, java\.io\.File\?\) -> Unit\) {.*?var company by remember { mutableStateOf\(""\) }.*?var role by remember { mutableStateOf\(""\) }.*?var date by remember { mutableStateOf\("2024-01-01"\) }',
    '@Composable\nfun AddInternDialog(existing: com.auraface.auraface_app.data.remote.dto.PlacementInternshipDto? = null, onDismiss: () -> Unit, onConfirm: (String, String, String, java.io.File?) -> Unit) {\n    var company by remember { mutableStateOf(existing?.company_name ?: "") }\n    var role by remember { mutableStateOf(existing?.role ?: "") }\n    var date by remember { mutableStateOf(if (existing?.start_date != null) existing.start_date.take(10) else "2024-01-01") }',
    content, flags=re.DOTALL
)

content = re.sub(
    r'@Composable\nfun AddEventDialog\(onDismiss: \(\) -> Unit, onConfirm: \(String, String, String, java\.io\.File\?\) -> Unit\) {.*?var name by remember { mutableStateOf\(""\) }.*?var type by remember { mutableStateOf\("Hackathon"\) }.*?var date by remember { mutableStateOf\("2024-01-01"\) }',
    '@Composable\nfun AddEventDialog(existing: com.auraface.auraface_app.data.remote.dto.PlacementEventDto? = null, onDismiss: () -> Unit, onConfirm: (String, String, String, java.io.File?) -> Unit) {\n    var name by remember { mutableStateOf(existing?.event_name ?: "") }\n    var type by remember { mutableStateOf(existing?.event_type ?: "Hackathon") }\n    var date by remember { mutableStateOf(if(existing?.date != null) existing.date.take(10) else "2024-01-01") }',
    content, flags=re.DOTALL
)

content = content.replace('Text("Add")', 'Text(if (existing != null) "Update" else "Add")')
content = content.replace('title = { Text("Add Skill") }', 'title = { Text(if (existing != null) "Edit Skill" else "Add Skill") }')
content = content.replace('title = { Text("Add Project") }', 'title = { Text(if (existing != null) "Edit Project" else "Add Project") }')
content = content.replace('title = { Text("Add Certification") }', 'title = { Text(if (existing != null) "Edit Certification" else "Add Certification") }')
content = content.replace('title = { Text("Add Internship") }', 'title = { Text(if (existing != null) "Edit Internship" else "Add Internship") }')
content = content.replace('title = { Text("Add Event") }', 'title = { Text(if (existing != null) "Edit Event" else "Add Event") }')

with open(filepath, 'w', encoding='utf-8') as f:
    f.write(content)

print("Updated script.")

@file:OptIn(ExperimentalMaterial3Api::class)

package com.auraface.auraface_app.presentation.student

import android.widget.Toast
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.auraface.auraface_app.data.network.api.StudentApi
import com.auraface.auraface_app.data.remote.dto.FeeDuesDto
import com.auraface.auraface_app.data.remote.dto.FeeTransactionDto
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FeePaymentViewModel @Inject constructor(
    private val studentApi: StudentApi
) : ViewModel() {
    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading

    private val _dues = MutableStateFlow<FeeDuesDto?>(null)
    val dues: StateFlow<FeeDuesDto?> = _dues

    private val _transactions = MutableStateFlow<List<FeeTransactionDto>>(emptyList())
    val transactions: StateFlow<List<FeeTransactionDto>> = _transactions

    fun loadData(onError: (String) -> Unit) {
        _loading.value = true
        viewModelScope.launch {
            try {
                _dues.value = studentApi.getFeeDues()
                _transactions.value = studentApi.getFeeTransactions()
            } catch (e: Exception) {
                onError(e.localizedMessage ?: "Failed to load fee details")
            } finally {
                _loading.value = false
            }
        }
    }

    fun makePayment(
        category: String,
        amount: Float,
        paymentMethod: String,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        _loading.value = true
        viewModelScope.launch {
            try {
                val res = studentApi.payFee(
                    com.auraface.auraface_app.data.remote.dto.FeePayRequestDto(
                        category = category,
                        amount = amount,
                        payment_method = paymentMethod
                    )
                )
                // Reload dues and transactions safely
                try {
                    _dues.value = studentApi.getFeeDues()
                    _transactions.value = studentApi.getFeeTransactions()
                } catch (_: Exception) {}
                onSuccess(res.message)
            } catch (e: Exception) {
                onError(e.localizedMessage ?: "Payment transaction failed")
            } finally {
                _loading.value = false
            }
        }
    }

    fun requestParentPayment(
        category: String,
        amount: Float,
        parentName: String,
        parentPhone: String,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        _loading.value = true
        viewModelScope.launch {
            try {
                val res = studentApi.requestParentPayment(
                    com.auraface.auraface_app.data.remote.dto.ParentPaymentRequestDto(
                        category = category,
                        amount = amount,
                        parent_name = parentName,
                        parent_phone = parentPhone
                    )
                )
                onSuccess(res.message)
            } catch (e: Exception) {
                onError(e.localizedMessage ?: "Failed to send request to parent")
            } finally {
                _loading.value = false
            }
        }
    }
}

@Composable
fun FeePaymentScreen(
    navController: NavController,
    viewModel: FeePaymentViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val loading by viewModel.loading.collectAsState()
    val dues by viewModel.dues.collectAsState()
    val transactions by viewModel.transactions.collectAsState()

    var showPaymentDialog by remember { mutableStateOf(false) }
    var showParentRequestDialog by remember { mutableStateOf(false) }
    var selectedPayCategory by remember { mutableStateOf("") }
    var selectedPayAmount by remember { mutableStateOf(0f) }

    LaunchedEffect(Unit) {
        viewModel.loadData { err ->
            Toast.makeText(context, err, Toast.LENGTH_LONG).show()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("GIET Smart Fee Portal", fontWeight = FontWeight.Bold, color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF4F46E5))
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF8FAFC))
                .padding(padding)
        ) {
            if (loading && dues == null) {
                CircularProgressIndicator(color = Color(0xFF4F46E5), modifier = Modifier.align(Alignment.Center))
            } else if (dues != null) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp)
                ) {
                    // Dues Summary Card
                    item {
                        Card(
                            shape = RoundedCornerShape(24.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        brush = Brush.linearGradient(
                                            colors = listOf(Color(0xFF4F46E5), Color(0xFF3B82F6))
                                        )
                                    )
                            ) {
                                Column(
                                    modifier = Modifier.padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text("Outstanding College Dues", fontWeight = FontWeight.SemiBold, color = Color.White.copy(alpha = 0.8f))
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = "Rs. ${dues!!.total_due}",
                                        style = MaterialTheme.typography.displayMedium,
                                        fontWeight = FontWeight.Black,
                                        color = Color.White
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Due Date: 30th August 2026",
                                        fontSize = 12.sp,
                                        color = Color.White.copy(alpha = 0.8f)
                                    )
                                }
                            }
                        }
                    }

                    // Detailed breakdown
                    item {
                        Text("Fee Category Breakdown", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, color = Color(0xFF1E293B))
                    }

                    val duesList = listOf(
                        Triple("Tuition Fee", dues!!.tuition_fee_due, Color(0xFF4F46E5)),
                        Triple("Hostel Fee", dues!!.hostel_fee_due, Color(0xFF10B981)),
                        Triple("Exam Fee", dues!!.exam_fee_due, Color(0xFFF59E0B)),
                        Triple("Library Fine", dues!!.library_fine_due, Color(0xFFEF4444))
                    )

                    items(duesList) { (title, amt, color) ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth(),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(18.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(color)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(title, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFF1E293B))
                                    Text("Rs. $amt due", color = Color(0xFF64748B), fontSize = 13.sp)
                                    if (amt > 0) {
                                        Text(
                                            text = "Ask Parent to Pay",
                                            color = Color(0xFF8B5CF6),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier
                                                .clickable {
                                                    selectedPayCategory = title
                                                    selectedPayAmount = amt
                                                    showParentRequestDialog = true
                                                }
                                                .padding(top = 4.dp)
                                        )
                                    }
                                }
                                if (amt > 0) {
                                    Button(
                                        onClick = {
                                            selectedPayCategory = title
                                            selectedPayAmount = amt
                                            showPaymentDialog = true
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEEF2FF)),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Text("Pay", color = Color(0xFF4F46E5), fontWeight = FontWeight.Bold)
                                    }
                                } else {
                                    Surface(
                                        color = Color(0xFFD1FAE5),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text("PAID", color = Color(0xFF065F46), fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                                    }
                                }
                            }
                        }
                    }

                    // Transaction history
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Recent Receipts", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, color = Color(0xFF1E293B))
                    }

                    if (transactions.isEmpty()) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = Color.White)
                            ) {
                                Text("No transactions recorded yet.", color = Color.Gray, modifier = Modifier.padding(20.dp), textAlign = TextAlign.Center)
                            }
                        }
                    } else {
                        items(transactions) { tx ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Receipt,
                                        contentDescription = null,
                                        tint = Color(0xFF818CF8)
                                    )
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(tx.category, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        Text(tx.transaction_id, fontSize = 11.sp, color = Color.Gray)
                                        Text(tx.paid_at.take(10), fontSize = 11.sp, color = Color.Gray)
                                    }
                                    Text("Rs. ${tx.amount}", fontWeight = FontWeight.Black, color = Color(0xFF10B981))
                                }
                            }
                        }
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CloudOff,
                        contentDescription = null,
                        tint = Color(0xFFEF4444),
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Unable to load fee details",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = Color(0xFF1E293B)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Please check server connectivity at ${com.auraface.auraface_app.core.Constants.BASE_URL}",
                        textAlign = TextAlign.Center,
                        color = Color(0xFF64748B),
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Button(
                        onClick = {
                            viewModel.loadData { err ->
                                Toast.makeText(context, err, Toast.LENGTH_LONG).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Retry", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        if (showPaymentDialog) {
            CheckoutDialog(
                category = selectedPayCategory,
                amount = selectedPayAmount,
                onDismiss = { showPaymentDialog = false },
                onConfirmPayment = { method, onComplete ->
                    viewModel.makePayment(
                        category = selectedPayCategory,
                        amount = selectedPayAmount,
                        paymentMethod = method,
                        onSuccess = { msg ->
                            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                            onComplete(true, msg)
                        },
                        onError = { err ->
                            Toast.makeText(context, err, Toast.LENGTH_LONG).show()
                            onComplete(false, err)
                        }
                    )
                }
            )
        }

        if (showParentRequestDialog) {
            val defaultParentName = dues?.parent_name ?: "Ramesh Kumar"
            val defaultParentPhone = dues?.parent_phone ?: "+91 9988776655"
            ParentRequestDialog(
                category = selectedPayCategory,
                amount = selectedPayAmount,
                defaultName = defaultParentName,
                defaultPhone = defaultParentPhone,
                onDismiss = { showParentRequestDialog = false },
                onSendRequest = { name, phone, onComplete ->
                    viewModel.requestParentPayment(
                        selectedPayCategory,
                        selectedPayAmount,
                        name,
                        phone,
                        { msg -> 
                            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                            onComplete(true, msg)
                            // Launch native SMS Intent to send real workable request message
                            val smsMsg = "Hi $name, please pay my college $selectedPayCategory dues of Rs. $selectedPayAmount. Link: http://giet.edu/pay/108"
                            val intent = Intent(Intent.ACTION_SENDTO).apply {
                                data = Uri.parse("smsto:$phone")
                                putExtra("sms_body", smsMsg)
                            }
                            try {
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, smsMsg)
                                }
                                context.startActivity(Intent.createChooser(shareIntent, "Send Request via"))
                            }
                        },
                        { err -> 
                            Toast.makeText(context, err, Toast.LENGTH_LONG).show()
                            onComplete(false, err)
                        }
                    )
                }
            )
        }
    }
}

@Composable
fun ParentRequestDialog(
    category: String,
    amount: Float,
    defaultName: String,
    defaultPhone: String,
    onDismiss: () -> Unit,
    onSendRequest: (String, String, (Boolean, String) -> Unit) -> Unit
) {
    var parentName by remember { mutableStateOf(defaultName) }
    var parentPhone by remember { mutableStateOf(defaultPhone) }
    var isSending by remember { mutableStateOf(false) }
    var showSuccess by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMsgText by remember { mutableStateOf("Sent to parent via SMS and WhatsApp.") }

    Dialog(
        onDismissRequest = { if (!isSending) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color.White,
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .wrapContentHeight()
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (showSuccess) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Color(0xFF10B981),
                        modifier = Modifier.size(72.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Request Dispatched!", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Color(0xFF1E293B))
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(successMsgText, color = Color(0xFF64748B), fontSize = 13.sp, textAlign = TextAlign.Center)
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Done", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                } else if (isSending) {
                    CircularProgressIndicator(color = Color(0xFF4F46E5), modifier = Modifier.size(56.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Sending notification request...", fontWeight = FontWeight.Bold)
                } else {
                    Text("Ask Parent to Pay", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Request funding for your $category", color = Color.Gray, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Rs. $amount", fontWeight = FontWeight.Black, fontSize = 28.sp, color = Color(0xFF8B5CF6))
                    
                    if (errorMessage != null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Surface(
                            color = Color(0xFFFEE2E2),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = errorMessage!!,
                                color = Color(0xFFDC2626),
                                fontSize = 12.sp,
                                modifier = Modifier.padding(10.dp),
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = parentName,
                        onValueChange = { parentName = it },
                        label = { Text("Parent / Guardian Name") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = parentPhone,
                        onValueChange = { parentPhone = it },
                        label = { Text("Parent Mobile Number") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Preview message
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("Message Preview:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Hi $parentName, please pay my college $category dues of Rs. $amount. Link: http://giet.edu/pay/108",
                                fontSize = 11.sp,
                                color = Color.DarkGray
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = onDismiss,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE2E8F0)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Cancel", color = Color(0xFF475569))
                        }
                        Button(
                            onClick = {
                                isSending = true
                                errorMessage = null
                                onSendRequest(parentName, parentPhone) { success, msg ->
                                    isSending = false
                                    if (success) {
                                        successMsgText = msg
                                        showSuccess = true
                                    } else {
                                        errorMessage = msg
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B5CF6)),
                            modifier = Modifier.weight(1f),
                            enabled = parentName.isNotBlank() && parentPhone.isNotBlank()
                        ) {
                            Text("Send Request", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CheckoutDialog(
    category: String,
    amount: Float,
    onDismiss: () -> Unit,
    onConfirmPayment: (String, (Boolean, String) -> Unit) -> Unit
) {
    var payMethod by remember { mutableStateOf("UPI") } // UPI or CARD
    var upiId by remember { mutableStateOf("") }
    var cardNumber by remember { mutableStateOf("") }
    var cardExpiry by remember { mutableStateOf("") }
    var cardCvv by remember { mutableStateOf("") }
    var isProcessing by remember { mutableStateOf(false) }
    var showSuccessAnimation by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMsgText by remember { mutableStateOf("Dues cleared and digital receipt cataloged.") }

    Dialog(
        onDismissRequest = { if (!isProcessing) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color.White,
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .wrapContentHeight()
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (showSuccessAnimation) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Color(0xFF10B981),
                        modifier = Modifier.size(72.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Payment Successful!", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Color(0xFF1E293B))
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(successMsgText, color = Color(0xFF64748B), fontSize = 13.sp, textAlign = TextAlign.Center)
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Done", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                } else if (isProcessing) {
                    CircularProgressIndicator(color = Color(0xFF4F46E5), modifier = Modifier.size(56.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Processing payment securely...", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Please do not exit or click Back", color = Color.Gray, fontSize = 12.sp)
                } else {
                    Text("Complete Payment", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Paying dues for $category", color = Color.Gray, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Rs. $amount", fontWeight = FontWeight.Black, fontSize = 28.sp, color = Color(0xFF4F46E5))
                    
                    if (errorMessage != null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Surface(
                            color = Color(0xFFFEE2E2),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = errorMessage!!,
                                color = Color(0xFFDC2626),
                                fontSize = 12.sp,
                                modifier = Modifier.padding(10.dp),
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { payMethod = "UPI" },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (payMethod == "UPI") Color(0xFF4F46E5) else Color(0xFFF1F5F9)
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("UPI ID", color = if (payMethod == "UPI") Color.White else Color(0xFF475569))
                        }
                        Button(
                            onClick = { payMethod = "CARD" },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (payMethod == "CARD") Color(0xFF4F46E5) else Color(0xFFF1F5F9)
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Credit Card", color = if (payMethod == "CARD") Color.White else Color(0xFF475569))
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    if (payMethod == "UPI") {
                        OutlinedTextField(
                            value = upiId,
                            onValueChange = { upiId = it },
                            placeholder = { Text("student@paytm or upi_id@ybl") },
                            label = { Text("Enter UPI ID") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )
                    } else {
                        OutlinedTextField(
                            value = cardNumber,
                            onValueChange = { cardNumber = it },
                            placeholder = { Text("4111 2222 3333 4444") },
                            label = { Text("Card Number") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = cardExpiry,
                                onValueChange = { cardExpiry = it },
                                placeholder = { Text("MM/YY") },
                                label = { Text("Expiry Date") },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            )
                            OutlinedTextField(
                                value = cardCvv,
                                onValueChange = { cardCvv = it },
                                placeholder = { Text("123") },
                                label = { Text("CVV") },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                visualTransformation = PasswordVisualTransformation()
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = onDismiss,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE2E8F0)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Cancel", color = Color(0xFF475569))
                        }
                        Button(
                            onClick = {
                                isProcessing = true
                                errorMessage = null
                                onConfirmPayment(payMethod) { success, msg ->
                                    isProcessing = false
                                    if (success) {
                                        successMsgText = msg
                                        showSuccessAnimation = true
                                    } else {
                                        errorMessage = msg
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5)),
                            modifier = Modifier.weight(1f),
                            enabled = (payMethod == "UPI" && upiId.isNotBlank()) || (payMethod == "CARD" && cardNumber.isNotBlank() && cardCvv.isNotBlank())
                        ) {
                            Text("Pay Now", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

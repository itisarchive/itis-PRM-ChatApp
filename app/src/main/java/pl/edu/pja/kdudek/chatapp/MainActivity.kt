package pl.edu.pja.kdudek.chatapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.database
import pl.edu.pja.kdudek.chatapp.model.Message
import pl.edu.pja.kdudek.chatapp.ui.theme.ChatAppTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ChatAppTheme {
                var isLoggedIn by remember { mutableStateOf(false) }
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Box(
                        modifier = Modifier.padding(innerPadding)
                    ) {}
                    if (!isLoggedIn) {
                        LoginScreen { email, password ->
                            Firebase.auth.signInWithEmailAndPassword(email, password)
                                .addOnSuccessListener {
                                    isLoggedIn = it.user != null
                                }
                        }
                    } else {
                        Column(
                            modifier = Modifier.fillMaxSize()
                        ) {
                            var message by remember { mutableStateOf("") }
                            val messages = remember { mutableStateListOf<Message>() }

                            DisposableEffect(Unit) {
                                val listener = object : ValueEventListener {
                                    override fun onDataChange(snapshot: DataSnapshot) {
                                        snapshot.children.mapNotNull {
                                            it.getValue(Message::class.java)
                                        }.let {
                                            messages.clear()
                                            messages.addAll(it)
                                        }
                                    }

                                    override fun onCancelled(error: DatabaseError) {
                                        // Handle error
                                    }
                                }

                                Firebase.database.getReference("messages")
                                    .addValueEventListener(listener)
                                onDispose {
                                    Firebase.database.getReference("messages")
                                        .removeEventListener(listener)
                                }
                            }

                            MessageView(messages)
                            Text(
                                Firebase.auth.currentUser?.displayName
                                    ?: Firebase.auth.currentUser?.email ?: "...",
                            )
                            Row {
                                TextField(message, onValueChange = { message = it })
                                Button(onClick = {
                                    Firebase.database.getReference("messages")
                                        .child(System.currentTimeMillis().toString())
                                        .setValue(Message(
                                            Firebase.auth.currentUser?.email ?: "...",
                                            message
                                        ))
                                        .addOnSuccessListener { println("ok") }
                                        .addOnFailureListener { println(it)
                                        }
                                }) {
                                    Text("Send")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LoginScreen(
    onLogin: (String, String) -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        TextField(
            email,
            onValueChange = { email = it }
        )
        TextField(
            password,
            onValueChange = { password = it },
            visualTransformation = PasswordVisualTransformation()
        )
        Button(
            onClick = { onLogin(email, password) }
        ) {
            Text("Login")
        }
    }
}

@Composable
fun MessageView(messageList: List<Message>) {
    LazyColumn {
        items(messageList, { it.content }) { msg ->
            Card(
                modifier = Modifier.padding(8.dp),
            ) {
                Column(
                    modifier = Modifier.padding(8.dp)
                ) {
                    Text(msg.email, style = MaterialTheme.typography.labelSmall)
                    Text(msg.content)
                }
            }
        }
    }
}

@Preview
@Composable
fun MessagesPreview() {
    ChatAppTheme {
        MessageView(
            listOf(
                Message("test@pja.edu.pl", "Hello World!"),
                Message("test@pja.edu.pl", "Hello Kotlin!"),
                Message("test@pja.edu.pl", "Another message"),
                Message("test@pja.edu.pl", "Yet another message")
            )
        )
    }
}

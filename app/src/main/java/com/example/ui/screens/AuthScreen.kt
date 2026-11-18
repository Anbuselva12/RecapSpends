package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.CurrencyRupee
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import com.example.R
import com.example.data.firebase.EmailAlreadyInUseException
import com.example.data.firebase.createAccountWithEmailPassword
import com.example.data.firebase.launchInteractiveGoogleSignIn
import com.example.data.firebase.sendPasswordResetEmail
import com.example.data.firebase.signInWithEmailPassword
import com.example.ui.components.SelavuSegmentedControl
import com.example.ui.components.SelavuTextField
import com.example.ui.theme.SelavuTheme
import com.example.util.selavuStrings
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AuthScreen(
    onAuthSuccess: () -> Unit
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }
    val colorScheme = MaterialTheme.colorScheme
    val extendedColors = SelavuTheme.extendedColors
    val strings = selavuStrings()

    val hostingBaseUrl = stringResource(R.string.hosting_base_url)
    val termsUrl = stringResource(R.string.terms_url)
    val policiesUrl = stringResource(R.string.policies_url)
    val privacyUrl = stringResource(R.string.privacy_url)

    fun openExternalUrl(url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            context.startActivity(intent)
        } catch (_: Exception) {
            Toast.makeText(context, "Visit $url in your browser", Toast.LENGTH_LONG).show()
        }
    }

    var authMode by rememberSaveable { mutableStateOf("signin") }
    var username by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var passwordVisible by rememberSaveable { mutableStateOf(false) }

    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isEmailCollisionError by remember { mutableStateOf(false) }
    var infoMessage by remember { mutableStateOf<String?>(null) }

    var showForgotDialog by remember { mutableStateOf(false) }
    var resetEmailInput by rememberSaveable { mutableStateOf("") }
    var isSendingReset by remember { mutableStateOf(false) }

    if (showForgotDialog) {
        AlertDialog(
            onDismissRequest = { if (!isSendingReset) showForgotDialog = false },
            containerColor = colorScheme.surface,
            titleContentColor = colorScheme.onSurface,
            textContentColor = colorScheme.onSurfaceVariant,
            shape = MaterialTheme.shapes.extraLarge,
            title = {
                Text(
                    text = "Reset password",
                    style = MaterialTheme.typography.headlineMedium,
                    color = colorScheme.onSurface
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Enter your registered email address and we will send a password reset link.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colorScheme.onSurfaceVariant
                    )
                    SelavuTextField(
                        value = resetEmailInput,
                        onValueChange = { resetEmailInput = it },
                        label = "Email address",
                        placeholder = "name@example.com",
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Done
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("forgot_password_email_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    enabled = !isSendingReset,
                    shape = MaterialTheme.shapes.small,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colorScheme.primary,
                        contentColor = colorScheme.onPrimary
                    ),
                    onClick = {
                        isSendingReset = true
                        errorMessage = null
                        isEmailCollisionError = false
                        infoMessage = null
                        scope.launch {
                            val res = sendPasswordResetEmail(resetEmailInput)
                            isSendingReset = false
                            if (res.isSuccess) {
                                email = resetEmailInput.trim()
                                infoMessage = "Password reset link sent to ${resetEmailInput.trim()}. Check your inbox or spam folder."
                                showForgotDialog = false
                            } else {
                                errorMessage = res.exceptionOrNull()?.localizedMessage
                                    ?: "Could not send password reset email."
                                showForgotDialog = false
                            }
                        }
                    },
                    modifier = Modifier.testTag("send_reset_email_button")
                ) {
                    Text(
                        text = if (isSendingReset) "Sending…" else "Send reset link",
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            },
            dismissButton = {
                TextButton(
                    enabled = !isSendingReset,
                    onClick = { showForgotDialog = false }
                ) {
                    Text(
                        text = "Cancel",
                        style = MaterialTheme.typography.labelLarge,
                        color = colorScheme.onSurfaceVariant
                    )
                }
            }
        )
    }

    fun submitEmailAuth() {
        if (isLoading) return
        focusManager.clearFocus()
        errorMessage = null
        isEmailCollisionError = false
        infoMessage = null
        isLoading = true
        scope.launch {
            val result = if (authMode == "signin") {
                signInWithEmailPassword(email, password)
            } else {
                createAccountWithEmailPassword(username, email, password)
            }
            isLoading = false
            if (result.isSuccess) {
                onAuthSuccess()
            } else {
                val error = result.exceptionOrNull()
                isEmailCollisionError = error is EmailAlreadyInUseException
                errorMessage = error?.localizedMessage
                    ?: "Authentication failed. Please check your credentials."
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colorScheme.background)
            .safeDrawingPadding()
            .imePadding(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 460.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(MaterialTheme.shapes.large)
                        .background(
                            Brush.linearGradient(
                                listOf(colorScheme.secondary, colorScheme.primary)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.CurrencyRupee,
                        contentDescription = "RecapSpends",
                        tint = colorScheme.onSecondary,
                        modifier = Modifier.size(30.dp)
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = strings.appName,
                        style = MaterialTheme.typography.headlineLarge,
                        color = colorScheme.onBackground
                    )
                    Text(
                        text = "Track Expense Smartly",
                        style = MaterialTheme.typography.titleMedium,
                        color = colorScheme.onPrimaryContainer
                    )
                }
            }

            Text(
                text = strings.signInSubtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = colorScheme.onSurfaceVariant
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.extraLarge)
                    .background(colorScheme.surface)
                    .border(1.dp, colorScheme.outlineVariant, MaterialTheme.shapes.extraLarge)
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SelavuSegmentedControl(
                    options = listOf("signin" to "Sign in", "signup" to "Create account"),
                    selectedKey = authMode,
                    onSelect = { mode ->
                        authMode = mode
                        errorMessage = null
                        isEmailCollisionError = false
                        infoMessage = null
                    },
                    modifier = Modifier.testTag("auth_mode_toggle")
                )

                if (authMode == "signup") {
                    SelavuTextField(
                        value = username,
                        onValueChange = { username = it },
                        label = "Username / Display name",
                        placeholder = "e.g. Karthik Raja",
                        leadingIcon = {
                            Icon(
                                Icons.Filled.Person,
                                contentDescription = null,
                                tint = colorScheme.onSurfaceVariant
                            )
                        },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("auth_username_input")
                    )
                }

                SelavuTextField(
                    value = email,
                    onValueChange = {
                        email = it
                        isEmailCollisionError = false
                    },
                    label = "Email address",
                    placeholder = "name@example.com",
                    leadingIcon = {
                        Icon(
                            Icons.Filled.Email,
                            contentDescription = null,
                            tint = colorScheme.onSurfaceVariant
                        )
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Next
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("auth_email_input")
                )

                SelavuTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = if (authMode == "signup") "Password (min 6 chars)" else "Password",
                    leadingIcon = {
                        Icon(
                            Icons.Filled.Lock,
                            contentDescription = null,
                            tint = colorScheme.onSurfaceVariant
                        )
                    },
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = if (passwordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                contentDescription = if (passwordVisible) "Hide password" else "Show password",
                                tint = colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(onDone = { submitEmailAuth() }),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("auth_password_input")
                )

                if (authMode == "signin") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Text(
                            text = "Forgot password?",
                            style = MaterialTheme.typography.labelLarge,
                            color = colorScheme.onPrimaryContainer,
                            modifier = Modifier
                                .clip(MaterialTheme.shapes.extraSmall)
                                .clickable {
                                    resetEmailInput = email.trim()
                                    showForgotDialog = true
                                }
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                                .testTag("auth_forgot_password")
                        )
                    }
                }

                if (infoMessage != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(MaterialTheme.shapes.small)
                            .background(extendedColors.okSoft)
                            .padding(12.dp)
                    ) {
                        Text(
                            text = infoMessage!!,
                            style = MaterialTheme.typography.bodySmall,
                            color = extendedColors.ok
                        )
                    }
                }

                if (errorMessage != null) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(MaterialTheme.shapes.small)
                            .background(colorScheme.errorContainer)
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = errorMessage!!,
                            style = MaterialTheme.typography.bodySmall,
                            color = colorScheme.error
                        )
                        if (isEmailCollisionError) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        authMode = "signin"
                                        errorMessage = null
                                        isEmailCollisionError = false
                                    },
                                    shape = MaterialTheme.shapes.small,
                                    border = BorderStroke(1.dp, colorScheme.error.copy(alpha = 0.5f)),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = colorScheme.error
                                    ),
                                    modifier = Modifier.testTag("auth_switch_to_signin_from_error")
                                ) {
                                    Text(
                                        text = "Switch to Sign in",
                                        style = MaterialTheme.typography.labelLarge
                                    )
                                }
                                TextButton(
                                    onClick = {
                                        resetEmailInput = email.trim()
                                        errorMessage = null
                                        isEmailCollisionError = false
                                        showForgotDialog = true
                                    },
                                    colors = ButtonDefaults.textButtonColors(
                                        contentColor = colorScheme.onErrorContainer
                                    ),
                                    modifier = Modifier.testTag("auth_reset_from_collision")
                                ) {
                                    Text(
                                        text = "Reset password",
                                        style = MaterialTheme.typography.labelLarge
                                    )
                                }
                            }
                        }
                    }
                }

                Button(
                    onClick = { submitEmailAuth() },
                    enabled = !isLoading,
                    shape = MaterialTheme.shapes.medium,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colorScheme.primary,
                        contentColor = colorScheme.onPrimary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag(if (authMode == "signin") "auth_email_sign_in_button" else "auth_sign_up_button")
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text = if (authMode == "signin") "Sign in" else "Create account",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    HorizontalDivider(modifier = Modifier.weight(1f), color = colorScheme.outlineVariant)
                    Text("OR", style = MaterialTheme.typography.labelSmall, color = colorScheme.outline)
                    HorizontalDivider(modifier = Modifier.weight(1f), color = colorScheme.outlineVariant)
                }

                OutlinedButton(
                    onClick = {
                        if (isLoading) return@OutlinedButton
                        errorMessage = null
                        isEmailCollisionError = false
                        infoMessage = null
                        isLoading = true
                        launchInteractiveGoogleSignIn(
                            context = context,
                            credentialManager = credentialManager,
                            scope = scope,
                            onAuthSuccess = {
                                isLoading = false
                                onAuthSuccess()
                            },
                            onAuthError = { msg ->
                                isLoading = false
                                errorMessage = msg
                            },
                            onAuthCancelled = {
                                isLoading = false
                            }
                        )
                    },
                    enabled = !isLoading,
                    shape = MaterialTheme.shapes.medium,
                    border = BorderStroke(1.dp, colorScheme.outlineVariant),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = colorScheme.background,
                        contentColor = colorScheme.onSurface
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("google_sign_in_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.AccountCircle,
                        contentDescription = null,
                        tint = colorScheme.onSurface,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Continue with Google",
                        style = MaterialTheme.typography.titleSmall,
                        color = colorScheme.onSurface
                    )
                }
            }

            val linkStyles = TextLinkStyles(
                style = SpanStyle(
                    color = colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                    textDecoration = TextDecoration.Underline
                )
            )
            val agreementText = buildAnnotatedString {
                append("By signing in you agree to ")
                withLink(
                    LinkAnnotation.Url(
                        url = termsUrl,
                        styles = linkStyles,
                        linkInteractionListener = { openExternalUrl(termsUrl) }
                    )
                ) {
                    append("Terms")
                }
                append(" and ")
                withLink(
                    LinkAnnotation.Url(
                        url = policiesUrl,
                        styles = linkStyles,
                        linkInteractionListener = { openExternalUrl(policiesUrl) }
                    )
                ) {
                    append("Policies")
                }
                append(" & ")
                withLink(
                    LinkAnnotation.Url(
                        url = privacyUrl,
                        styles = linkStyles,
                        linkInteractionListener = { openExternalUrl(privacyUrl) }
                    )
                ) {
                    append("Privacy Notice")
                }
                append(".")
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = agreementText,
                    style = MaterialTheme.typography.bodySmall.copy(textAlign = TextAlign.Center),
                    color = colorScheme.onSurfaceVariant,
                    modifier = Modifier.testTag("auth_legal_agreement_text")
                )
            }
        }
    }
}

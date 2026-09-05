package io.github.kaulith.helpdeskanalytics.ui.screens.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.kaulith.helpdeskanalytics.R
import io.github.kaulith.helpdeskanalytics.ui.theme.FrappeRadius
import io.github.kaulith.helpdeskanalytics.ui.theme.Spacing
import org.koin.androidx.compose.koinViewModel

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    viewModel: LoginViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val focusManager = LocalFocusManager.current
    val context = LocalContext.current
    val cs = MaterialTheme.colorScheme

    LaunchedEffect(uiState.isLoginSuccess) {
        if (uiState.isLoginSuccess) onLoginSuccess()
    }

    LaunchedEffect(uiState.authorizationUrl) {
        val url = uiState.authorizationUrl ?: return@LaunchedEffect
        if (!launchAuthorization(context, url)) viewModel.onBrowserUnavailable()
        viewModel.onAuthorizationLaunched()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(cs.surface)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .imePadding(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.xl, vertical = Spacing.xl2),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Hero icon: primaryContainer tile, scheme-aware
            Surface(
                shape = FrappeRadius.xl2,
                color = cs.primaryContainer,
                modifier = Modifier.size(96.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        painter = painterResource(R.drawable.ic_launcher_foreground),
                        contentDescription = null,
                        tint = androidx.compose.ui.graphics.Color.Unspecified,
                        modifier = Modifier.size(80.dp)
                    )
                }
            }

            Spacer(Modifier.height(Spacing.lg))

            Text(
                text = "Helpdesk",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = cs.onSurface,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(Spacing.xs))
            Text(
                text = "Sign in to your Frappe Helpdesk site",
                style = MaterialTheme.typography.bodyMedium,
                color = cs.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(Spacing.xl2))

            OutlinedTextField(
                value = uiState.siteUrl,
                onValueChange = viewModel::onSiteUrlChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Site URL") },
                placeholder = { Text("https://yoursite.frappe.cloud") },
                isError = uiState.siteUrlError != null,
                supportingText = uiState.siteUrlError?.let { { Text(it) } },
                singleLine = true,
                shape = FrappeRadius.lg,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Uri,
                    imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Down) }
                )
            )

            if (uiState.useApiKey) {
                Spacer(Modifier.height(Spacing.md))

                OutlinedTextField(
                    value = uiState.apiKey,
                    onValueChange = viewModel::onApiKeyChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("API Key") },
                    placeholder = { Text("Enter API key") },
                    isError = uiState.apiKeyError != null,
                    supportingText = uiState.apiKeyError?.let { { Text(it) } },
                    singleLine = true,
                    shape = FrappeRadius.lg,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(
                        onNext = { focusManager.moveFocus(FocusDirection.Down) }
                    )
                )

                Spacer(Modifier.height(Spacing.md))

                OutlinedTextField(
                    value = uiState.apiSecret,
                    onValueChange = viewModel::onApiSecretChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("API Secret") },
                    placeholder = { Text("Enter API secret") },
                    isError = uiState.apiSecretError != null,
                    supportingText = uiState.apiSecretError?.let { { Text(it) } },
                    singleLine = true,
                    shape = FrappeRadius.lg,
                    visualTransformation = if (uiState.isPasswordVisible) VisualTransformation.None
                    else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = viewModel::togglePasswordVisibility) {
                            Icon(
                                imageVector = if (uiState.isPasswordVisible) Icons.Outlined.Visibility
                                else Icons.Outlined.VisibilityOff,
                                contentDescription = if (uiState.isPasswordVisible) "Hide secret" else "Show secret",
                                tint = cs.onSurfaceVariant
                            )
                        }
                    },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            focusManager.clearFocus()
                            viewModel.signInWithApiKey()
                        }
                    )
                )
            } else if (uiState.needsClientId) {
                Spacer(Modifier.height(Spacing.md))

                OutlinedTextField(
                    value = uiState.clientId,
                    onValueChange = viewModel::onClientIdChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Client ID") },
                    placeholder = { Text("From the site's OAuth Client") },
                    isError = uiState.clientIdError != null,
                    supportingText = uiState.clientIdError?.let { { Text(it) } },
                    singleLine = true,
                    shape = FrappeRadius.lg,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            focusManager.clearFocus()
                            viewModel.signIn()
                        }
                    )
                )
            }

            uiState.generalError?.let { error ->
                Spacer(Modifier.height(Spacing.md))
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = FrappeRadius.md,
                    color = cs.errorContainer,
                    contentColor = cs.onErrorContainer
                ) {
                    Text(
                        text = error,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(Spacing.md)
                    )
                }
            }

            Spacer(Modifier.height(Spacing.xl))

            Button(
                onClick = {
                    focusManager.clearFocus()
                    if (uiState.useApiKey) viewModel.signInWithApiKey() else viewModel.signIn()
                },
                enabled = !uiState.isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = FrappeRadius.full,
                colors = ButtonDefaults.buttonColors(
                    containerColor = cs.primary,
                    contentColor = cs.onPrimary
                )
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = cs.onPrimary
                    )
                    Spacer(Modifier.size(Spacing.sm))
                }
                Text(
                    "Sign in",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(Modifier.height(Spacing.sm))

            TextButton(
                onClick = viewModel::toggleApiKeyEntry,
                enabled = !uiState.isLoading
            ) {
                Text(
                    text = if (uiState.useApiKey) "Sign in with your account" else "Use an API key instead",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Spacer(Modifier.height(Spacing.lg))

            Text(
                text = if (uiState.useApiKey) {
                    "Need help generating API credentials?"
                } else {
                    "Signing in opens your site in the browser"
                },
                style = MaterialTheme.typography.bodySmall,
                color = cs.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

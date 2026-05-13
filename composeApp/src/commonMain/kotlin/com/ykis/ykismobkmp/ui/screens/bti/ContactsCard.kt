package com.ykis.ykismobkmp.ui.screens.bti

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AlternateEmail
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.ykis.mob.R
import com.ykis.mob.core.composable.EmailField
import com.ykis.mob.core.composable.PhoneField
import com.ykis.ykismobkmp.ui.BaseUIState
import com.ykis.ykismobkmp.ui.components.BaseCard
import com.ykis.mob.ui.components.LabelTextWithTextAndIcon

@Composable
fun ContactsCard(
    modifier : Modifier = Modifier,
    baseUIState: BaseUIState = BaseUIState(),
    onUpdateBti : () -> Unit,
    phone : String,
    email : String,
    onEmailChange: (String) -> Unit,
    onPhoneChange: (String) -> Unit,
) {
    val openDialog = remember { mutableStateOf(false) }
    BaseCard(
        label = stringResource(id = R.string.contacts),
        actionButton = {
            IconButton(onClick = {
                openDialog.value = true
            }) {
                Icon(
                    imageVector = Icons.Default.Edit ,
                    contentDescription = "edit"
                )
            }
        }
    ) {
        LabelTextWithTextAndIcon(
            imageVector = Icons.Default.Phone,
            labelText = stringResource(id = R.string.phone_colon),
            valueText = phone
        )
        LabelTextWithTextAndIcon(
            imageVector = Icons.Default.AlternateEmail,
            labelText = stringResource(id = R.string.email_colon),
            valueText = email
        )
    }
    if(openDialog.value){
        ChangeContactsDialog(
            baseUIState = baseUIState,
            onDismissRequest = { openDialog.value = false},
            phone = phone,
            email = email ,
            previousPhone = baseUIState.apartment.phone,
            previousEmail = baseUIState.apartment.email,
            onEmailChange = onEmailChange,
            onPhoneChange = onPhoneChange,
            onUpdateClick = onUpdateBti
        )
    }
}

@Composable
fun ChangeContactsDialog(
    modifier: Modifier = Modifier,
    baseUIState: BaseUIState,
    onDismissRequest: () -> Unit,
    onUpdateClick : () -> Unit,
    phone : String,
    email : String,
    previousPhone:String,
    previousEmail:String,
    onEmailChange: (String) -> Unit,
    onPhoneChange: (String) -> Unit,
) {
    Dialog(
        onDismissRequest = { onDismissRequest()},
    ) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(48.dp)),
        ) {
            Column(
                modifier = modifier.padding(all = 8.dp)
            ){
//                Icon(
//                    tint = MaterialTheme.colorScheme.secondary,
//                    modifier = modifier.fillMaxWidth(),
//                    imageVector = Icons.Default.Edit ,
//                    contentDescription = null
//                )
                Text(
                    text = stringResource(R.string.update_bti),
                    fontSize = 22.sp,
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = modifier.padding(all = 8.dp),
                    color = MaterialTheme.colorScheme.onSurface
                )
                PhoneField(modifier = modifier.padding(bottom = 5.dp),value = phone , onNewValue =onPhoneChange )
                EmailField(value = email, onNewValue =onEmailChange )
                Row(
                    modifier = modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = {
                        onDismissRequest()
                        onEmailChange(previousEmail)
                        onPhoneChange(previousPhone)
                    }) {
                        Text(
                            text = stringResource(id = R.string.cancel)
                        )
                    }
                    TextButton(
                        onClick = {
                            onDismissRequest()
                            onUpdateClick()
                        },
                        enabled = (previousEmail != email ||previousPhone != phone )

                    ) {
                        Text(
                            stringResource(R.string.change)
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewContactsCard() {
    ContactsCard(phone = "+3806337442",
        email = "rshulik74@gmail.com",
        onEmailChange = {} ,
        onPhoneChange = {},
        onUpdateBti = {}
    )
}

@Preview
@Composable
private fun PreviewDialog() {
    ChangeContactsDialog(
        baseUIState = BaseUIState(),
        onDismissRequest = { /*TODO*/ },
        onUpdateClick = { /*TODO*/ },
        phone = BaseUIState().apartment.phone,
        email = BaseUIState().apartment.email,
        previousPhone = "+380000000",
        previousEmail = "example@gmail.com",
        onEmailChange = {}
    ) {

    }
}

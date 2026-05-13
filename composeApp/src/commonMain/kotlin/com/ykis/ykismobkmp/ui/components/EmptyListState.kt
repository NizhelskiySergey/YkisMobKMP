package com.ykis.mob.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ykis.mob.R
import com.ykis.ykismobkmp.ui.theme.YkisPAMTheme

@Composable
fun EmptyListState(modifier: Modifier = Modifier,
                   useDarkTheme: Boolean = isSystemInDarkTheme(),
                   title : String,
                   subtitle : String = ""
) {
    val paintRes = if(useDarkTheme){
        R.drawable.ic_empty_box_dark
    }else{
        R.drawable.ic_empty_box_light
    }
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        Alignment.Center
    ) {
        val height = maxHeight
        val modifierBox1: Modifier = if (height > 5000.dp)
            Modifier.fillMaxHeight()
        else
            Modifier.height(50.dp)
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(bottom = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Image(
                modifier = modifier,
                painter = painterResource(id = paintRes),
                contentDescription = null)
            Text(
                text = title, style = TextStyle(
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    lineHeight = 24.sp,
                    letterSpacing = 0.15.sp
                )
            )
            Text(text = subtitle)
        }
    }
}


@Preview(showBackground = true, device = "spec:width=1081px,height=1920px")
@Composable
private fun PreviewEmptyState() {
    YkisPAMTheme {
        EmptyListState(
            title = "Нічого не знайдено",
            subtitle = "Показань не знайдено"
        )
    }
}

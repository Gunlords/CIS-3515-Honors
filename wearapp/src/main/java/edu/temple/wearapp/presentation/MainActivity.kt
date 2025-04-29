package edu.temple.wearapp.presentation

import android.graphics.BitmapFactory
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.Wearable
import edu.temple.wearapp.presentation.theme.WearappTheme

class MainActivity : ComponentActivity(), DataClient.OnDataChangedListener {

    private var bookTitle by mutableStateOf("Waiting for Book...")
    private var coverImageBytes by mutableStateOf<ByteArray?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        setTheme(android.R.style.Theme_DeviceDefault)

        setContent {
            WearApp(bookTitle, coverImageBytes)
        }
    }

    override fun onResume() {
        super.onResume()
        Wearable.getDataClient(this).addListener(this)
    }

    override fun onPause() {
        super.onPause()
        Wearable.getDataClient(this).removeListener(this)
    }

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        for (event in dataEvents) {
            if (event.type == DataEvent.TYPE_CHANGED) {
                val item = DataMapItem.fromDataItem(event.dataItem)
                if (event.dataItem.uri.path == "/book_info") {
                    bookTitle = item.dataMap.getString("title") ?: "Unknown Title"
                    coverImageBytes = item.dataMap.getByteArray("cover")
                }
            }
        }
    }
}

@Composable
fun WearApp(title: String, coverImageBytes: ByteArray?) {
    WearappTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colors.background),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    text = title
                )
                Spacer(modifier = Modifier.height(10.dp))
                coverImageBytes?.let {
                    val bitmap = BitmapFactory.decodeByteArray(it, 0, it.size)
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "Book Cover",
                        modifier = Modifier.size(100.dp)
                    )
                }
            }
        }
    }
}

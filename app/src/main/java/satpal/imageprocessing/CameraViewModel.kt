package satpal.imageprocessing

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class CameraViewModel : ViewModel() {
    private val _params = MutableStateFlow(Params.CannyParams(255.0, 255.0))
    val params: StateFlow<Params.CannyParams> = _params

    fun onThreshold1Change(data: Double) {
        _params.value = _params.value.copy(threshold1 = data)
    }

    fun onThreshold2Change(data: Double) {
        _params.value = _params.value.copy(threshold2 = data)
    }
}

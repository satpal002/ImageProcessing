package satpal.imageprocessing

import android.graphics.Bitmap
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.viewModels
import satpal.imageprocessing.databinding.ActivityCameraBinding
import satpal.imageprocessing.util.CameraUtil
import satpal.imageprocessing.util.ProcessImageAnalyzer

class CameraActivity : AppCompatActivity() {

    private val cameraViewModel: CameraViewModel by viewModels()

    private var _binding: ActivityCameraBinding? = null
    private val binding get() = _binding!!
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = ActivityCameraBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)
        binding.btnRescan.visibility = View.INVISIBLE

        if (!CameraUtil.checkPermissions(this)) {
            CameraUtil.userRequestPermissions(this)
        }

        binding.btnRescan.setOnClickListener() {
            imageFound = false
            binding.btnRescan.visibility = View.INVISIBLE
        }
        initCamera()
    }

    var imageFound = false
    private fun initCamera() {
        CameraUtil.startCamera(
            this,
            ProcessImageAnalyzer(
                { it1, it ->
                    runOnUiThread {
                        if (!imageFound) {
                            _binding?.imageView?.setImageBitmap(
                                it
                            )
                        }
                        if (!imageFound && it1) {
                            val aspectRatio = getAspectRatio(it)
                            Log.wtf("satpal", "aspect ratio: $aspectRatio")
                            if ((aspectRatio > ProcessImageAnalyzer.aspectRatioThresholdMin && aspectRatio < ProcessImageAnalyzer.aspectRatioThresholdMax) ||
                                (aspectRatio > ProcessImageAnalyzer.aspectRatioThresholdMinLandscape && aspectRatio < ProcessImageAnalyzer.aspectRatioThresholdMaxLandscape)
                            ) {
                                imageFound = it1
                                binding.btnRescan.visibility = View.VISIBLE
                            }
                        }
                    }

                }, cameraViewModel.params
            ),
        )

    }

    fun getAspectRatio(bitmap: Bitmap): Float {
        if (bitmap.height != 0) {
            return bitmap.width.toFloat() / bitmap.height.toFloat()
        } else {
            return 0f
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }
}
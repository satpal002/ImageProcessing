package satpal.imageprocessing.util

import android.graphics.Bitmap
import android.graphics.Rect
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy

import kotlinx.coroutines.flow.StateFlow
import org.opencv.android.Utils
import org.opencv.core.Mat
import org.opencv.core.MatOfPoint
import org.opencv.core.MatOfPoint2f
import org.opencv.imgproc.Imgproc
import satpal.imageprocessing.Params

class ProcessImageAnalyzer(
    val runOnUiThread: (Boolean, Bitmap) -> Unit,
    val params: StateFlow<Params>
) : ImageAnalysis.Analyzer {

    companion object {
        val aspectRatioThresholdMin = 0.5f
        val aspectRatioThresholdMax = 0.51f
        val aspectRatioThresholdMinLandscape = 2.05f
        val aspectRatioThresholdMaxLandscape = 2.1f
    }

    override fun analyze(image: ImageProxy) {
        val mat: Mat = CameraUtil.getMatFromImage(image)
        val matMask = Mat(mat.rows(), mat.cols(), 0)
        val matTemp = Mat(mat.rows(), mat.cols(), mat.type())
        val matOutput = Mat(mat.rows(), mat.cols(), mat.type())
        val params = params.value
        if(params is Params.CannyParams) {
            val originalMat = mat.clone()
            Imgproc.cvtColor(mat, mat, Imgproc.COLOR_RGB2BGR)
            Imgproc.Canny(
                mat,
                matOutput,
                params.threshold1,
                params.threshold2
            )

            val contours = ArrayList<MatOfPoint>()
            Imgproc.findContours(
                matOutput,
                contours,
                Mat(),
                Imgproc.RETR_EXTERNAL,
                Imgproc.CHAIN_APPROX_SIMPLE
            )

            val largestContour = contours.maxByOrNull { Imgproc.contourArea(it) }
                if (largestContour != null) {
                    // Simplify the contour
//                val epsilon = 0.04 * Imgproc.arcLength(largestContour, true)
                    val epsilon =
                        0.04 * Imgproc.arcLength(MatOfPoint2f(*largestContour.toArray()), true)
                    val approx = MatOfPoint2f()
                    Imgproc.approxPolyDP(MatOfPoint2f(*largestContour.toArray()), approx, epsilon, true)

                    if (approx.toArray().size == 4) {
                        val inputBitmap: Bitmap =
                            Bitmap.createBitmap(originalMat.cols(), originalMat.rows(), Bitmap.Config.ARGB_8888)
                        Utils.matToBitmap(originalMat, inputBitmap)

                        val rect = Imgproc.boundingRect(largestContour)
                        println("Square coordinates: (${rect.x}, ${rect.y}) to (${rect.x + rect.width}, ${rect.y + rect.height})")
                        val bitmap = cropBitmap(inputBitmap, rect.x, rect.y, rect.width, rect.height)

                        val aspectRatio = getAspectRatio(bitmap)
                        if ((aspectRatio > aspectRatioThresholdMin && aspectRatio < aspectRatioThresholdMax) || (aspectRatio > aspectRatioThresholdMinLandscape && aspectRatio < aspectRatioThresholdMaxLandscape)) {
                            runOnUiThread(true, bitmap)
                            image.close()
                            return
                        }
                    }

                }

            val bitmap: Bitmap =
                Bitmap.createBitmap(originalMat.cols(), originalMat.rows(), Bitmap.Config.ARGB_8888)
            Utils.matToBitmap(originalMat, bitmap)
            runOnUiThread(false, bitmap)
            image.close()
        }


    }

    fun cropBitmap(inputBitmap: Bitmap, x: Int, y: Int, width: Int, height: Int): Bitmap {
        // Create a Rect to define the cropping region
        val rect = Rect(x, y, x + width, y + height)

        // Create a new Bitmap by cropping the inputBitmap
        val croppedBitmap =
            Bitmap.createBitmap(inputBitmap, rect.left, rect.top, rect.width(), rect.height())

        return croppedBitmap
    }

    fun getAspectRatio(bitmap: Bitmap): Float {
        if (bitmap.height != 0) {
            return bitmap.width.toFloat() / bitmap.height.toFloat()
        } else {
            return 0f
        }
    }
}

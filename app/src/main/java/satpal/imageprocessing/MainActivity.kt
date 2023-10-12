package satpal.imageprocessing

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Rect
import android.os.Bundle
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import org.opencv.android.Utils
import org.opencv.core.Mat
import org.opencv.core.MatOfPoint
import org.opencv.core.MatOfPoint2f
import org.opencv.core.Scalar
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc


class MainActivity : AppCompatActivity() {

    private val minThreshold = 10.0
    private val maxThreshold = 30.0
    private val thickness = 10
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val inputImage = R.drawable.image6
        val bitmapHighlighted = highlightAllContours(inputImage)

        val imgInput = findViewById<ImageView>(R.id.imgInput)
        imgInput.setImageBitmap(bitmapHighlighted)

        val imgOutput = findViewById<ImageView>(R.id.imgOutput)
        val bitmapProcessed = cropSolidBorderUsingOpenCV(inputImage)
        bitmapProcessed?.let {
            imgOutput.setImageBitmap(bitmapProcessed)
        }
    }

    // Function to detect and crop solid border from an image using OpenCV
    private fun cropSolidBorderUsingOpenCV(inputImage: Int): Bitmap? {

        //array to store all contours for testing
        val arrBitmap = ArrayList<Bitmap>()
        val inputBitmap = BitmapFactory.decodeResource(resources, inputImage)
        val inputMat = Mat()
        Utils.bitmapToMat(inputBitmap, inputMat)
        try {
            // Convert the input bitmap to a Mat (OpenCV format)
            val gray = Mat()
            Imgproc.cvtColor(inputMat, gray, Imgproc.COLOR_BGR2GRAY)

            // Apply Gaussian blur
            val blurred = Mat()
            Imgproc.GaussianBlur(gray, blurred, Size(5.0, 5.0), 0.0)

            // Apply Canny edge detection
            val edges = Mat()
            Imgproc.Canny(blurred, edges, minThreshold, maxThreshold)

            // Find contours
            val contours = ArrayList<MatOfPoint>()
            Imgproc.findContours(
                edges,
                contours,
                Mat(),
                Imgproc.RETR_EXTERNAL,
                Imgproc.CHAIN_APPROX_SIMPLE
            )

            for (contour in contours) {
                val epsilon = 0.04 * Imgproc.arcLength(MatOfPoint2f(*contour.toArray()), true)
                val approx = MatOfPoint2f()
                Imgproc.approxPolyDP(MatOfPoint2f(*contour.toArray()), approx, epsilon, true)

                if (approx.toArray().size == 4) {


                    val rect = Imgproc.boundingRect(contour)
                    println("Square coordinates: (${rect.x}, ${rect.y}) to (${rect.x + rect.width}, ${rect.y + rect.height})")

                    //I was trying to detect area by calculating aspect ratio of contour
//                        if(rect.width/rect.height.toDouble() > 1.2 || rect.height/rect.width.toDouble() == 1.2) {
//                                return cropBitmap(
//                                    inputBitmap,
//                                    rect.x,
//                                    rect.y,
//                                    rect.width,
//                                    rect.height
//                                )
//                        }

                    arrBitmap.add(
                        cropBitmap(
                            inputBitmap,
                            rect.x,
                            rect.y,
                            rect.width,
                            rect.height
                        )
                    )
                }

            }

            // Find the largest contour
            val largestContour = contours.maxByOrNull { Imgproc.contourArea(it) }
            if (largestContour != null) {
                // Simplify the contour
//                val epsilon = 0.04 * Imgproc.arcLength(largestContour, true)
                val epsilon =
                    0.04 * Imgproc.arcLength(MatOfPoint2f(*largestContour.toArray()), true)
                val approx = MatOfPoint2f()
                Imgproc.approxPolyDP(MatOfPoint2f(*largestContour.toArray()), approx, epsilon, true)

                if (approx.toArray().size == 4) {
                    val rect = Imgproc.boundingRect(largestContour)
                    println("Square coordinates: (${rect.x}, ${rect.y}) to (${rect.x + rect.width}, ${rect.y + rect.height})")
                    return cropBitmap(inputBitmap, rect.x, rect.y, rect.width, rect.height)
                }

                // Draw the contour
                val drawnImage = Mat()
                inputMat.copyTo(drawnImage)
                Imgproc.drawContours(
                    drawnImage,
                    listOf(largestContour),
                    -1,
                    Scalar(0.0, 255.0, 0.0),
                    thickness
                )

                // Convert Mat to Bitmap
                val resultBitmap = Bitmap.createBitmap(
                    drawnImage.cols(),
                    drawnImage.rows(),
                    Bitmap.Config.ARGB_8888
                )
                Utils.matToBitmap(drawnImage, resultBitmap)
                return resultBitmap
            }


        } catch (e: Exception) {
            println("exception: ${e.message}")
        }

        return null
    }

    fun cropBitmap(inputBitmap: Bitmap, x: Int, y: Int, width: Int, height: Int): Bitmap {
        // Create a Rect to define the cropping region
        val rect = Rect(x, y, x + width, y + height)

        // Create a new Bitmap by cropping the inputBitmap
        val croppedBitmap =
            Bitmap.createBitmap(inputBitmap, rect.left, rect.top, rect.width(), rect.height())

        return croppedBitmap
    }

    private fun highlightAllContours(inputImage: Int): Bitmap? {
        val inputBitmap = BitmapFactory.decodeResource(resources, inputImage)
        val inputMat = Mat()
        Utils.bitmapToMat(inputBitmap, inputMat)

        try {
            // Convert the input bitmap to a Mat (OpenCV format)
            val gray = Mat()
            Imgproc.cvtColor(inputMat, gray, Imgproc.COLOR_BGR2GRAY)

            // Apply Gaussian blur
            val blurred = Mat()
            Imgproc.GaussianBlur(gray, blurred, Size(5.0, 5.0), 0.0)

            // Apply Canny edge detection
            val edges = Mat()
            Imgproc.Canny(blurred, edges, minThreshold, maxThreshold)

            // Find contours
            val contours = ArrayList<MatOfPoint>()
            Imgproc.findContours(
                edges,
                contours,
                Mat(),
                Imgproc.RETR_EXTERNAL,
                Imgproc.CHAIN_APPROX_SIMPLE
            )

            // Create a copy of the inputMat to draw contours on
            val drawnImage = Mat()
            inputMat.copyTo(drawnImage)

            // Define the purple color (in RGB format)
            val purpleColor = Scalar(128.0, 128.0, 128.0)

            // Iterate through the contours and draw each one in purple with a thickness of 20 pixels
            for (contour in contours) {
                Imgproc.drawContours(
                    drawnImage,
                    listOf(contour),
                    -1,
                    purpleColor, // Set the highlight color to purple in RGB
                    thickness // Set the line thickness to 20 pixels
                )
            }



            // Convert the drawnImage Mat to a Bitmap
            val resultBitmap = Bitmap.createBitmap(
                drawnImage.cols(),
                drawnImage.rows(),
                Bitmap.Config.ARGB_8888
            )
            Utils.matToBitmap(drawnImage, resultBitmap)
            return resultBitmap
        } catch (e: Exception) {
            println("exception: ${e.message}")
        }

        return null
    }


}
package satpal.imageprocessing;

import android.app.Application;
import android.util.Log;

import org.opencv.android.OpenCVLoader;

public class ImageProcessingApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        if (!OpenCVLoader.initDebug())
            Log.e("OpenCV", "Unable to load OpenCV!");
        else
            Log.d("OpenCV", "OpenCV loaded Successfully!");
    }
}

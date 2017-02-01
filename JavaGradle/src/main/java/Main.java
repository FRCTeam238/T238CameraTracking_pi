package edu.Team238;

import java.util.ArrayList;

import edu.wpi.first.wpilibj.networktables.*;
import edu.wpi.first.wpilibj.tables.*;
import edu.wpi.cscore.*;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.imgproc.Imgproc; 
import org.opencv.core.Rect;

public class Main {
  public static void main(String[] args) {
    // Loads our OpenCV library. This MUST be included
    System.loadLibrary("opencv_java310");
    
    GripPipeline pipeline = new GripPipeline();
    long lastFrameTime = 0;
    long frameTime = 0;
    ArrayList<MatOfPoint> hulledContours = new ArrayList<MatOfPoint>();

    // Connect NetworkTables, and get access to the publishing table
    NetworkTable.setClientMode();
    // Set your team number here
    NetworkTable.setTeam(238);

    NetworkTable.initialize();
    NetworkTable.getTable("SmartDashboard").putBoolean("Process", false);
    NetworkTable.getTable("SmartDashboard").putNumber("Luminance Min", 19);
    NetworkTable.getTable("SmartDashboard").putNumber("Luminance Max", 255);
    NetworkTable.getTable("SmartDashboard").putNumber("Saturation Min", 110);
    NetworkTable.getTable("SmartDashboard").putNumber("Saturation Max", 255);  
    NetworkTable.getTable("SmartDashboard").putNumber("Hue Min", 76);        
    NetworkTable.getTable("SmartDashboard").putNumber("Hue Max", 93);
    


    // This is the network port you want to stream the raw received image to
    // By rules, this has to be between 1180 and 1190, so 1185 is a good choice
    int streamPort = 1185;

    // This stores our reference to our mjpeg server for streaming the input image
    MjpegServer inputStream = new MjpegServer("MJPEG Server", streamPort);

    /***********************************************/

    // USB Camera
    // This gets the image from a USB camera 
    // Usually this will be on device 0, but there are other overloads
    // that can be used
    UsbCamera camera = setUsbCamera(0, inputStream);
    // Set the resolution for our camera, since this is over USB
    camera.setResolution(320,240);

    // This creates a CvSink for us to use. This grabs images from our selected camera, 
    // and will allow us to use those images in opencv
    CvSink imageSink = new CvSink("CV Image Grabber");
    imageSink.setSource(camera);

    // This creates a CvSource to use. This will take in a Mat image that has had OpenCV operations
    // operations 
    CvSource imageSource = new CvSource("CV Image Source", VideoMode.PixelFormat.kMJPEG, 640, 480, 30);
    MjpegServer cvStream = new MjpegServer("CV Image Stream", 1186);
    cvStream.setSource(imageSource);

    // All Mats and Lists should be stored outside the loop to avoid allocations
    // as they are expensive to create
    Mat inputImage = new Mat();
    Mat hsv = new Mat();

    // Infinitely process image
    while (true) {
      // Grab a frame. If it has a frame time of 0, there was an error.
      // Just skip and continue
      long frameT = imageSink.grabFrame(inputImage);
      if (frameT == 0) continue;
      lastFrameTime = frameTime;
      frameTime = System.nanoTime();
      NetworkTable.getTable("SmartDashboard").putNumber("Frame time", (frameTime - lastFrameTime)/1000000);
      // Below is where you would do your OpenCV operations on the provided image
      // The sample below just changes color source to HSV
      if(NetworkTable.getTable("SmartDashboard").getBoolean("Process", false))
      {
        pipeline.hslThresholdLuminance[0] = NetworkTable.getTable("SmartDashboard").getNumber("Luminance Min", 19);
        pipeline.hslThresholdLuminance[1] = NetworkTable.getTable("SmartDashboard").getNumber("Luminance Max", 255);
        pipeline.hslThresholdSaturation[0] = NetworkTable.getTable("SmartDashboard").getNumber("Saturation Min", 110);
        pipeline.hslThresholdSaturation[1] = NetworkTable.getTable("SmartDashboard").getNumber("Saturation Max", 255);  
        pipeline.hslThresholdHue[0] = NetworkTable.getTable("SmartDashboard").getNumber("Hue Min", 76);        
        pipeline.hslThresholdHue[1] = NetworkTable.getTable("SmartDashboard").getNumber("Hue Max", 93);
        pipeline.process(inputImage);
        
        hulledContours = pipeline.filterContoursOutput();
        hulledContours.sort((c1, c2) -> (int)(Imgproc.contourArea(c1) - Imgproc.contourArea (c2)));
        if(hulledContours.size() > 0)
        {
          Rect rect = Imgproc.boundingRect(hulledContours.get(0));
          int cX = rect.x + rect.width/2;
          int cY = rect.y + rect.height/2;
          double dd = cX / inputImage.size().width;
          double angle = 60*dd - 30;
          NetworkTable.getTable("SmartDashboard").putNumber("DD", dd);
          NetworkTable.getTable("SmartDashboard").putNumber("Angle", angle);
        }

        // Here is where you would write a processed image that you want to restreams
        // This will most likely be a marked up image of what the camera sees
        // For now, we are just going to stream the HSV image
        imageSource.putFrame(pipeline.hslThresholdOutput());
      }
    }
  }
  
    private static UsbCamera setUsbCamera(int cameraId, MjpegServer server) {
    // This gets the image from a USB camera 
    // Usually this will be on device 0, but there are other overloads
    // that can be used
    UsbCamera camera = new UsbCamera("CoprocessorCamera", cameraId);
    server.setSource(camera);
    return camera;
  }
}
import java.util.ArrayList;

import edu.wpi.first.wpilibj.networktables.*;
import edu.wpi.first.wpilibj.tables.*;
import edu.wpi.cscore.*;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

public class Main {
  public static void main(String[] args) {
    // Loads our OpenCV library. This MUST be included
    System.loadLibrary("opencv_java310");

    // Connect NetworkTables, and get access to the publishing table
    NetworkTable.setClientMode();
    // Set your team number here
    NetworkTable.setTeam(238);

    NetworkTable.initialize();

    // This stores our reference to our mjpeg server for streaming the input image
    MjpegServer inputStream = new MjpegServer("MJPEG Server",
        TargetTracking.CAMERA_STREAM_PORT);

    // USB Camera
    // This gets the image from a USB camera 
    // Usually this will be on device 0, but there are other overloads
    // that can be used
    UsbCamera camera = setUsbCamera(0, inputStream);
    // Set the resolution for our camera, since this is over USB

    camera.setResolution(
        TargetTracking.CAMERA_RESOLUTION_WIDTH,
        TargetTracking.CAMERA_RESOLUTION_HEIGHT);

    /***********************************************/
    /*C TargetTracking tracking = new TargetTracking(); */
    //TargetTracking2017v2_Gear tracking = new TargetTracking2017v2_Gear();
    TargetTracking2017v2_Shooter tracking = new TargetTracking2017v2_Shooter();
    tracking.Initialize(camera,
            (double)TargetTracking.CAMERA_RESOLUTION_WIDTH,
            (double)TargetTracking.CAMERA_RESOLUTION_HEIGHT,
            TargetTracking.CAMERA_WIDTH_DEGREES,
            TargetTracking.CAMERA_HEIGHT_DEGREES);

    while (true) {
        tracking.Process();
    }
  }

  private static HttpCamera setHttpCamera(String cameraName, MjpegServer server) {
    // Start by grabbing the camera from NetworkTables
    NetworkTable publishingTable = NetworkTable.getTable("CameraPublisher");
    // Wait for robot to connect. Allow this to be attempted indefinitely
    while (true) {
      try {
        if (publishingTable.getSubTables().size() > 0) {
          break;
        }
        Thread.sleep(500);
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }


    HttpCamera camera = null;
    if (!publishingTable.containsSubTable(cameraName)) {
      return null;
    }
    ITable cameraTable = publishingTable.getSubTable(cameraName);
    String[] urls = cameraTable.getStringArray("streams", null);
    if (urls == null) {
      return null;
    }
    ArrayList<String> fixedUrls = new ArrayList<String>();
    for (String url : urls) {
      if (url.startsWith("mjpg")) {
        fixedUrls.add(url.split(":", 2)[1]);
      }
    }
    camera = new HttpCamera("CoprocessorCamera", fixedUrls.toArray(new String[0]));
    server.setSource(camera);
    return camera;
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

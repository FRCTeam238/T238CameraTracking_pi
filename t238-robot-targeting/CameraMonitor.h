#ifndef CameraMonitor_h
#define CameraMonitor_h

#include "TargetProcessor.h"

#include <opencv2/opencv.hpp>
#include <vector>

struct CameraSettings
{
    int LowerHue;
    int UpperHue;
    int LowerSaturation;
    int UpperSaturation;
    int LowerValue;
    int UpperValue;
    int BlurIndex;
};

class CameraMonitor
{
    public:
        CameraMonitor();
        ~CameraMonitor();

        void InitializeSettings();
        void InitializeCamera();

        bool IsReady() const
        {
            return mCamera.isOpened();
        }

        cv::Mat NextFrame();

    private:
        struct FrameCalculations
        {
            int max_contour;
            int max_width;
            int max_height;

            // the calculated rectangle
            int s_range_x1;
            int s_range_y1;
            int s_range_x2;
            int s_range_y2;

            // the center point of the calculated rectangle
            int center_x;
            int center_y;

            double angle;
            double distance;
        };

        bool ReadFrame(cv::Mat &frame);

        static void DrawHull(cv::Mat frame, ContourList &contours,
                FrameCalculations &calcs);
        static void GetRangeOfContour(std::vector<cv::Point> &contour,
                int &range_x1, int &range_y1, int &range_x2, int &range_y2);
        static void CalculateHull(cv::Mat frame, ContourList &contours,
                FrameCalculations &calcs);

        CameraSettings mSettings;
        int mColorMode;
        cv::VideoCapture mCamera;
        TargetProcessor mTarget;
        ContourList mHull;

        // disable the copy constructor and assignment operator
        CameraMonitor(const CameraMonitor &);
        CameraMonitor &operator=(const CameraMonitor &);
};

#endif

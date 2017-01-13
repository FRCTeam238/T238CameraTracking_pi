#include "TargetProcessor.h"
//#include "ReportingThread.h"
#include "Configuration.h"

using namespace cv;

#if 1
TargetProcessor::TargetProcessor(int colorMode, int blurIndex,
    cv::Scalar threshLow, cv::Scalar threshHigh)
    : mBlurIndex(blurIndex),
      convertColor(colorMode),
      blur(blurIndex),
      threshold(threshLow, threshHigh),
      contourBuilder(),
      hullBuilder()
{
    // do nothing
}
#else

TargetProcessor::TargetProcessor()
{
    // do nothing
}
#endif

TargetProcessor::~TargetProcessor()
{
    // do nothing
}

void TargetProcessor::Process(cv::Mat frame)
{
    frame = convertColor.Process(frame);

    if (mBlurIndex > 0)
    {
        frame = blur.Process(frame);
    }

    frame = threshold.Process(frame);

    cv::Mat ccframe = frame.clone();
    mContours = contourBuilder.Process(ccframe);
    mHull = hullBuilder.Process(mContours);

#if 0
    static int frameCount = 0;
    //TODO the frame count is used to determine the 
    //  fps - at some point convert this to be a rolling avg
    //  instead of a true average over the life of the program
    frameCount++;
    if (frameCount == 0)
    {
        frameCount = 1;
    }

    if (Config.DebugMode == DM_Normal)
    {
    }
    else if (Config.DebugMode == DM_RotatingNumbers)
    {
        // emit results to the transmit buffer here
        static int angle = 0;
        static int direction = 0;

        UpdateCameraData(angle, direction, frameCount);

        angle++;
        direction--;
    }
#endif
}

void TargetProcessor::Initialize(int colorMode,
        int lowerHue, int upperHue,
        int lowerSaturation, int upperSaturation,
        int lowerValue, int upperValue,
        int blurIndex)
{
    convertColor.SetColorSpace(colorMode);
    threshold.SetRange(
        Scalar(lowerHue, lowerSaturation, lowerValue),
        Scalar(upperHue, upperSaturation, upperValue));
    blur.SetBlur(blurIndex);
}



#include "TargetProcessor.h"
//#include "ReportingThread.h"
#include "Configuration.h"

using namespace cv;

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

TargetProcessor::~TargetProcessor()
{
    // do nothing
}

#if 1
Mat TargetProcessor::Process_filter(cv::Mat frame)
{
    frame = convertColor.Process(frame);

    if (mBlurIndex > 0)
    {
        frame = blur.Process(frame);
    }

    frame = threshold.Process(frame);

    return frame;
}

ContourList TargetProcessor::Process_hull(cv::Mat frame)
{
    cv::Mat ccframe = frame.clone();
    mContours = contourBuilder.Process(ccframe);
    mHull = hullBuilder.Process(mContours);

    return mHull;
}

void TargetProcessor::Process(cv::Mat frame)
{
    Mat ccframe = Process_filter(frame);
    Process_hull(ccframe);
}

#else
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
}
#endif

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



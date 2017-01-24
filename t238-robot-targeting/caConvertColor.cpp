#include "caConvertColor.h"

ConvertColor::ConvertColor(int targetColorSpace)
    : mTargetColorSpace(targetColorSpace)
{
    // do nothing
}

ConvertColor::~ConvertColor()
{
    // do nothing
}

cv::Mat ConvertColor::Process(cv::Mat frame)
{
    cv::Mat retval;

    cv::cvtColor(frame, retval, mTargetColorSpace);

    return retval;
}


#include "Blur.h"

Blur::Blur(int blurIndex)
    : mBlurIndex(blurIndex)
{
    // do nothing
}

Blur::~Blur()
{
    // do nothing
}

cv::Mat Blur::Process(cv::Mat frame)
{
    cv::Mat retval;

    //cv::GaussianBlur(mat, mat, cv::Size(), mSigma);
    cv::blur(frame, retval, cv::Size(20,20));
    return retval;
}


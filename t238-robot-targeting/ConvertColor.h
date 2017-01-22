#ifndef ConvertColor_h
#define ConvertColor_h

#include <opencv2/opencv.hpp>

class ConvertColor
{
    public:
        ConvertColor(int targetColorSpace);
        ~ConvertColor();

        cv::Mat Process(cv::Mat frame);

        void SetColorSpace(int colorSpace)
        {
            mTargetColorSpace = colorSpace;
        }

    private:
        int mTargetColorSpace;

        // disabled
        ConvertColor(ConvertColor &);
        ConvertColor &operator=(ConvertColor &);
};

#endif


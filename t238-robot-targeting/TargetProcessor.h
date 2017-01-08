#ifndef TargetProcessor_h
#define TargetProcessor_h

#include <opencv2/opencv.hpp>

#include <vector>
typedef std::vector<std::vector<cv::Point> > ContourList;


class ConvertColor
{
    public:
        ConvertColor(int targetColorSpace)
            : mTargetColorSpace(targetColorSpace)
        {
            // do nothing
        }

        ~ConvertColor()
        {
            // do nothing
        }

        cv::Mat Process(cv::Mat frame)
        {
            cv::Mat retval;

            cv::cvtColor(frame, retval, mTargetColorSpace);

            return retval;
        }

    private:
        int mTargetColorSpace;

        // disabled
        ConvertColor &operator=(ConvertColor &);
};

class Blur 
{
    public:
        Blur(int blurIndex)
            : mBlurIndex(blurIndex)
        {
            // do nothing
        }

        cv::Mat Process(cv::Mat frame)
        {
            cv::Mat retval;

            //cv::GaussianBlur(mat, mat, cv::Size(), mSigma);
            cv::blur(frame, retval, cv::Size(20,20));
            return retval;
        }

    private:
        int mBlurIndex;
};

class FilterColorThreshold
{
    public:
        FilterColorThreshold(cv::Scalar low, cv::Scalar high) :
            mRangeLower(low), mRangeUpper(high)
        {
            // do nothing
        }

        ~FilterColorThreshold()
        {
            // do nothing
        }

        cv::Mat Process(cv::Mat frame)
        {
            cv::Mat retval;

            cv::inRange(frame, mRangeLower, mRangeUpper, retval);
            return retval;
        }

    private:
        cv::Scalar mRangeLower;
        cv::Scalar mRangeUpper;
};

class ContourBuilder
{
    public:
        ContourBuilder()
            : mMode(CV_RETR_TREE), mMethod(CV_CHAIN_APPROX_SIMPLE)
        {
            // do nothing
        }

        ContourList Process(cv::Mat mat)
        {
            ContourList contours;
            std::vector<cv::Vec4i> notused; // contours;

            //std::cout << mat.size() << std::endl;
            //std::cout << mat.type() << std::endl;

            cv::findContours(mat, contours, notused, mMode, mMethod,
                    cv::Point(0,0));

            return contours;
        }

    private:
        int mMode;
        int mMethod;
};

class HullBuilder
{
    public:
        ContourList Process(ContourList &contours)
        {
            ContourList hull(contours.size());

            for (size_t index = 0; index < contours.size(); index++)
            {
                convexHull(cv::Mat(contours[index]), hull[index], false);
            }

            return hull;
        }

    private:

};

class TargetProcessor
{
    public:
#if 1
        TargetProcessor(int colorMode, int blurIndex,
            cv::Scalar threshLow, cv::Scalar threshHigh);
#else
        TargetProcessor();
#endif
        ~TargetProcessor();

        void Initialize(int colorMode,
                int lowerHue, int upperHue,
                int lowerSaturation, int upperSaturation,
                int lowerValue, int upperValue,
                int blurIndex);

        void Process(cv::Mat frame);

        const ContourList &GetHull() const
        {
            return mHull;
        }

    private:
        ContourList mContours;
        ContourList mHull;

        int mBlurIndex;

        ConvertColor convertColor;
        Blur blur;
        FilterColorThreshold threshold;
        ContourBuilder contourBuilder;
        HullBuilder hullBuilder;

        // disable the copy constructor and assignment operator
        TargetProcessor(const TargetProcessor &);
        TargetProcessor &operator=(const TargetProcessor &);
};

#endif


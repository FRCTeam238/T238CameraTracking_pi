#ifndef Blur_h
#define Blur_h

#include <opencv2/opencv.hpp>

class Blur 
{
    public:
        Blur(int blurIndex);
        ~Blur();

        cv::Mat Process(cv::Mat frame);

        void SetBlur(int blurIndex)
        {
            mBlurIndex = blurIndex;
        }

    private:
        int mBlurIndex;

        // disabled 
        Blur(const Blur &);
        Blur &operator =(Blur &);
};

#endif

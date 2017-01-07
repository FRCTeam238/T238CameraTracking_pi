#include <unistd.h>
#include <iostream>
#include <opencv2/opencv.hpp>
#include <string>

#include "ReportingThread.h"
#include "CameraMonitor.h"
#include "Configuration.h"

using std::cout;
using std::endl;

static CameraMonitor sMonitor;

void start_reporting_thread()
{
    StartReportingThread();
}

void camera_monitor_initialize()
{
    // TODO load settings from an external file
    sMonitor.InitializeSettings();

    sMonitor.InitializeCamera();
}

void camera_monitor_iteration()
{
    cv::Mat frame = sMonitor.NextFrame();
    //cv::imshow("test", frame);
}

static void print_help()
{
    cout << "-d0       Normal operating mode" << endl;
    cout << "-d1       (DBG) Send rolling numbers to robot" << endl;
    cout << "-s        (DBG) Show camera image window" << endl;
    cout << "-r IP     Set robot IP address" << endl;
    cout << "-p PORT   Set robot port number" << endl;
}

void parse_options(int argc, char *argv[])
{
    int argi;

    for (argi = 1; argi < argc; )
    {
        if (strcmp(argv[argi], "-d0") == 0)
        {
            Config.DebugMode = DM_Normal;
            argi++;
        }
        else if (strcmp(argv[argi], "-d1") == 0)
        {
            Config.DebugMode = DM_RotatingNumbers;
            argi++;
        }
        else if (strcmp(argv[argi], "-s") == 0)
        {
            Config.ShowDebugFrameWindow = !Config.ShowDebugFrameWindow;
            argi++;
        }
        else if (strcmp(argv[argi], "-r") == 0)
        {
            argi++;
            Config.RobotIPAddress = argv[argi];
            argi++;
        }
        else if (strcmp(argv[argi], "-p") == 0)
        {
            argi++;
            Config.RobotPort = argv[argi];
            argi++;
        }
        else if (strcmp(argv[argi], "-h") == 0)
        {
            print_help();
            argi++;
        }
        else
        {
            argi++;
        }
    }
}

int main(int argc, char *argv[])
{
    ConfigurationInitialize();
    /* begin:options Test the command line options, and report the results */
    parse_options(argc, argv);

    if (Config.DebugMode == DM_Normal)
    {
        cout << "Mode: Normal" << endl;
    }
    else if (Config.DebugMode == DM_RotatingNumbers)
    {
        cout << "Mode: Rotating Numbers" << endl;
    }
    else
    {
    }
    /* end:options */

    try {
        bool done = false;

        /* begin:init start the various threads */
        camera_monitor_initialize();

        start_reporting_thread(); // initialize & start sending camera data
        //start_command_thread();   // TODO receive command from robot

        /* end:init */

        /* begin:main camera loop */
        for (done = false; !done; )
        {
            usleep(300);
            camera_monitor_iteration();
        }
        /* end:init */
    }
    //catch (exception ex)
    catch (...)
    {
        // any subsystem (but not threads) can escape the program
        // by throwing an exception 
        cout << "exception" << endl;
    }

    return 0;
}

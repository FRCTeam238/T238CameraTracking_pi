#ifndef Configuration_h
#define COnfiguration_h

#include <string>

enum DebugModeSetting
{
    DM_Normal,
    DM_RotatingNumbers
};

struct Configuration
{
    DebugModeSetting DebugMode;
    bool ShowDebugFrameWindow;
    double AngleWidth;

    char RobotPort[256];
    char RobotIPAddress[256];
    //std::string RobotPort;
    //std::string RobotIPAddress;
};

extern Configuration Config;

void ConfigurationInitialize();


#endif

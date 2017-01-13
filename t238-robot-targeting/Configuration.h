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
    double AngleWidth;

    bool RP_Enable;

    // robot port
    char RB_Port[256];
    char RB_IPAddress[256];

    // debug channel
    bool DC_Enable;
    char DC_Port[256];
    char DC_IPAddress[256];

    bool SI_Enable;
    char SI_Filename[256];

    // show the debug information
    DebugModeSetting DebugMode;
    bool ShowDebugFrameWindow;

    bool DB_DrawAllHull;

};

extern Configuration Config;

void ConfigurationInitialize();


#endif

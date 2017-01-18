#include "Configuration.h"

#include <cstring>

#define DEFAULT_RB_PORT "5800"
#define DEFAULT_RB_IP   "10.2.38.2"

#define DEFAULT_DC_PORT "2017"
#define DEFAULT_DC_IP   "localhost"

#define DEFAULT_SI_FILENAME ""

Configuration Config;

void ConfigurationInitialize()
{
    memset(&Config, 0, sizeof(Config));

    Config.DebugMode = DM_Normal;
    Config.ShowDebugFrameWindow = false;
    Config.DB_DrawAllHull = true;

    Config.AngleWidth = 60.0;

    Config.RP_Enable = true;

    strcpy(Config.RB_Port, DEFAULT_RB_PORT);
    strcpy(Config.RB_IPAddress, DEFAULT_RB_IP);

    Config.DC_Enable = false;
    strcpy(Config.DC_Port, DEFAULT_DC_PORT);
    strcpy(Config.DC_IPAddress, DEFAULT_DC_IP);

    Config.SI_Enable = false;
    strcpy(Config.SI_Filename, DEFAULT_SI_FILENAME);

}


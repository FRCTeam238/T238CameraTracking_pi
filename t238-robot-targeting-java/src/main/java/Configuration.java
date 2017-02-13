import java.util.*;
import java.io.*;

class Configuration
{
    private static Configuration mConfig = null;

    public static Configuration GetInstance()
    {
        if (mConfig == null)
        {
            mConfig = new Configuration();
        }

        return mConfig;
    }

    public static void Initialize()
    {
        try {
            Configuration config = Configuration.GetInstance();

            Properties defaults = new Properties();
            defaults.setProperty("tracking_mode", "gear");

            Properties props = new Properties(defaults);

            FileInputStream in = new FileInputStream("camera-tracking.config");
            props.load(in);
            in.close();

            Set keys = props.keySet();   // get set-view of keys
            Iterator itr = keys.iterator();

            while (itr.hasNext())
            {
                String str = (String)itr.next();
                System.out.println("PROP: [" +
                        str + "]=[" + props.getProperty(str) + "]");

                if (str.equals("tracking_mode"))
                {
                    String mode = props.getProperty(str);

                    if (mode.equals("gear"))
                    {
                        config.SetTrackingMode(
                                Configuration.TRACKING_MODE_gear);
                    }
                    else if (mode.equals("shooter"))
                    {
                        config.SetTrackingMode(
                                Configuration.TRACKING_MODE_shooter);
                    }
                    else
                    {
                        System.out.println(
                            "Invalid value for tracking_mode. " +
                            "Use 'gear' or 'shooter'");
                        config.SetTrackingMode(
                                Configuration.TRACKING_MODE_gear);
                    }
                }
                else
                {
                    System.out.println("Unknown property");
                }
            }
        }
        catch (Exception ex)
        {
            System.out.println(ex);
        }
    }

    /*
        This section contains all of the configuration options. All
        variable names match the name as it appears in the configuration
        file itself.

        The allowed values defined as constants include the variable
        name (all upper case) and the value string that would appear
        in the configuration file in a case sensitive manner.

     */

    public static final int TRACKING_MODE_gear = 0;
    public static final int TRACKING_MODE_shooter = 1;
    private int tracking_mode = Configuration.TRACKING_MODE_gear;

    public void SetTrackingMode(int mode)
    {
        tracking_mode = mode;
    }

    public int GetTrackingMode()
    {
        return tracking_mode;
    }
}



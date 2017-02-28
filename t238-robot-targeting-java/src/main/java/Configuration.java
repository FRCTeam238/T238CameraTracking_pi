import java.util.*;
import java.io.*;

class Configuration
{
    private static Configuration mConfig = null;

    public static Configuration GetInstance()
    {
        if (mConfig == null)
        {
            System.out.println("new configuration");
            mConfig = new Configuration();
        }

        return mConfig;
    }

    private void ProcessProperties(Properties props, Iterator it)
    {
        while (it.hasNext())
        {
            String propertyName = (String)it.next();
            String propertyValue = props.getProperty(propertyName);

            if (propertyName.equals("tracking_mode"))
            {
                if (propertyValue.equals("gear"))
                {
                    SetTrackingMode(Configuration.TRACKING_MODE_gear);
                }
                else if (propertyValue.equals("gear3"))
                {
                    SetTrackingMode(Configuration.TRACKING_MODE_gear3);
                }
                else if (propertyValue.equals("shooter"))
                {
                    SetTrackingMode(Configuration.TRACKING_MODE_shooter);
                }
                else if (propertyValue.equals("shooter3"))
                {
                    SetTrackingMode(Configuration.TRACKING_MODE_shooter3);
                }
                else
                {
                    System.out.println("props: tracking_mode has a bad value");
                }
            }
            else if (propertyName.equals("tracking_gear_side"))
            {
                if (propertyValue.equals("left"))
                {
                    SetTrackingGearSide(Configuration.TRACKING_GEAR_SIDE_left);
                }
                else if (propertyValue.equals("right"))
                {
                    SetTrackingGearSide(Configuration.TRACKING_GEAR_SIDE_right);
                }
                else
                {
                    System.out.println(
                            "props: tracking_gear_side has a bad value");
                }
                System.out.println("tracking_gear_side=" + GetTrackingGearSide());
            }
        }
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

            config.ProcessProperties(props, itr);

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
    public static final int TRACKING_MODE_gear3 = 2;
    public static final int TRACKING_MODE_shooter3 = 3;

    private int tracking_mode = Configuration.TRACKING_MODE_gear;

    public void SetTrackingMode(int mode)
    {
        tracking_mode = mode;
    }

    public int GetTrackingMode()
    {
        return tracking_mode;
    }

    public static final int TRACKING_GEAR_SIDE_left = 0;
    public static final int TRACKING_GEAR_SIDE_right = 1;

    private int tracking_gear_side = Configuration.TRACKING_GEAR_SIDE_left;

    public void SetTrackingGearSide(int side)
    {
        tracking_gear_side = side;
    }

    public int GetTrackingGearSide()
    {
        return tracking_gear_side;
    }
}



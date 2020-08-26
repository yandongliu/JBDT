package JBDT.util;

import JBDT.errors.ArgumentsError;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;

/**
 * Configuration class
 */
public class Config {

    HashMap<String, String> mapConfig;
    public String config_dir;
    public int train_max_rows;
    public String label;
    public int loss;
    public boolean train = true;
    public boolean test = true;

    public Config() {
        mapConfig = new HashMap<String, String>();
    }

    public boolean getBool(String key, boolean defval) {
        if(mapConfig.containsKey(key)) {
            return Boolean.parseBoolean(mapConfig.get(key));
        } else
            return defval;
    }

    public int getInt(String key, int defval) {
        if(mapConfig.containsKey(key)) {
            return Integer.parseInt(mapConfig.get(key));
        } else
            return defval;
    }

    public double getDouble(String key, double defval) {
        if(mapConfig.containsKey(key)) {
            return Double.parseDouble(mapConfig.get(key));
        } else
            return defval;
    }

    public String get(String key, String defval) {
        if(mapConfig.containsKey(key)) {
            return mapConfig.get(key);
        } else
            return defval;
    }

    public static Config parseArgs(String[] args) throws ArgumentsError {
        Config config = new Config();
        int minops = 1;
        for(int i=0;i<args.length;i++) {
            if(args[i].equals("--train")) {
                config.train = true;
                config.test = false;
                minops++;
                continue;
            }
            if(args[i].equals("--test")) {
                config.train = false;
                config.test = true;
                minops++;
                continue;
            }
        }
        try {
            if(args.length < minops) {
                throw new ArgumentsError();
            }
            File file = new File(args[args.length-1]);
            config.config_dir = file.getParent();
            BufferedReader br = new BufferedReader(new FileReader(file));
            String s;
            while((s=br.readLine())!=null) {
                s = s.trim();
                s = s.replaceFirst("#.*","").trim();

                if(s.equals("")) {
                    continue;
                }
                String[] ss = s.split("=");
                if(ss.length!=2) {
                    System.out.println(ss.length);
                    System.out.println("config file format error: "+s);
                    return null;
                }
                config.put(ss[0].trim(), ss[1].trim());
            }
            br.close();
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }
        return config;
    }

    public void put(String key, String val) {
        mapConfig.put(key, val);
    }
}

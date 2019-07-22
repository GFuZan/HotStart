package gfuzan.hotstart;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Properties;

public class Configuration {

    private Properties properties = new Properties();

    private String listDelimiter = ";";

    public static Configuration properties(InputStream inStream) throws IOException {
        Properties properties = new Properties();
        properties.load(inStream);

        Configuration configuration = new Configuration();
        configuration.properties = properties;

        return configuration;
    }

    public static Configuration properties(URL url) {
        try {
            return properties(new File(url.toURI()));
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        return new Configuration();
    }

    public static Configuration properties(File file) {
        InputStream inStream = null;
        try {
            inStream = new FileInputStream(file);
            return properties(inStream);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (inStream != null) {
                    inStream.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return new Configuration();
    }

    public void append(Configuration c) {
        for (Object key : c.properties.keySet()) {
            this.properties.setProperty((String) key, c.properties.getProperty((String) key));
        }
    }

    public int getInt(String key, int defaultValue) {
        int res = defaultValue;
        try {
            Integer.parseInt(getString(key));

        } catch (Exception e) {
        }
        return res;
    }

    public String getString(String key) {
        return properties.getProperty(key);
    }

    String[] getStringArray(String key) {
        return getString(key).split(listDelimiter);
    }
}

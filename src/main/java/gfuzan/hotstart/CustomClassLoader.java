package gfuzan.hotstart;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;

public class CustomClassLoader extends URLClassLoader {

    public CustomClassLoader() {
        super(new URL[] {});
    }

    public void addURL(File path) {
        try {
            this.addURL(path.toURI().toURL());
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void addURL(URL url) {
        super.addURL(url);
    }

    @Override
    protected Class<?> findClass(final String name) throws ClassNotFoundException {
         System.out.println("加载类: " + name);
        return super.findClass(name);
    }
}

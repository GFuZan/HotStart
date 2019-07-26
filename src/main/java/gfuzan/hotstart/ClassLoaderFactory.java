package gfuzan.hotstart;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Collection;

import org.apache.commons.io.FileUtils;

/**
 * 类加载器工厂
 * @author GFuZan
 *
 */
public class ClassLoaderFactory {
    /**
     * 获取类加载器
     * @param classesPaths  ClassPath(文件夹路径)数组
     * @param libPaths  Lib(jar包文件夹路径)数组
     * @return  CustomClassLoader
     */
    public static CustomClassLoader getClassLoader(String[] classesPaths, String[] libPaths) {
        CustomClassLoader ccl = new CustomClassLoaderImpl();
        for (String classesPath : classesPaths) {
            ccl.addFile(new File(classesPath));
        }

        for (String libPath : libPaths) {
            File jarDirectory = new File(libPath);
            if (jarDirectory.isDirectory()) {
                Collection<File> listJars = FileUtils.listFiles(new File(libPath), new String[] { "jar" }, true);
                for (File jar : listJars) {
                    ccl.addFile(jar);
                }
            }
        }
        return ccl;
    }

    /**
     * 加载器接口
     * @author GFuZan
     *
     */
    public static interface CustomClassLoader {

        public void addFile(File path);

        public Class<?> findClass(final String name) throws ClassNotFoundException;

        public URLClassLoader getURLClassLoader();

    }

    /**
     * 加载器实现
     * @author GFuZan
     *
     */
    public static class CustomClassLoaderImpl extends URLClassLoader implements CustomClassLoader {

        private CustomClassLoaderImpl() {
            super(new URL[] {});
        }

        @Override
        public void addFile(File path) {
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
        public Class<?> findClass(final String name) throws ClassNotFoundException {
            // System.out.println("加载类: " + name);
            return super.findClass(name);
        }

        @Override
        public URLClassLoader getURLClassLoader() {
            return this;
        }
    }
}

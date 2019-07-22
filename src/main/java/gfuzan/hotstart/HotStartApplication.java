package gfuzan.hotstart;

import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.FileUtils;

public class HotStartApplication {

    /**
     * properties 文件列表
     */
    private static final String[] PROPERTIES = new String[] { "application.properties" };

    /**
     * 扫描间隔(单位s)
     */
    private static final String SCAN_INTERVAL = "ScanInterval";

    /**
     * 启动类
     */
    private static final String START_CLASS = "StartClass";

    /**
     * 类路径
     */
    private static final String CLASSES_PATH = "ClassesPath";

    /**
     * jar包路径
     */
    private static final String LIB_PATH = "LibPath";

    @SuppressWarnings("deprecation")
    public static void main(String[] args) throws Exception {
        Configuration configuration = getConfigurations(PROPERTIES);

        // 获取配置文件内容
        final int scanInterval = configuration.getInt(SCAN_INTERVAL, 30);
        final String startClass = configuration.getString(START_CLASS);
        String[] classesPath = configuration.getStringArray(CLASSES_PATH);
        String[] libPath = configuration.getStringArray(LIB_PATH);

        // 配置信息校验
        if (startClass == null || startClass.isEmpty() || classesPath == null || classesPath.length == 0) {
            throw new NullPointerException("请配置参数: '" + START_CLASS + "','" + CLASSES_PATH + "'");
        }

        // 初始化文件信息
        final Map<String, Long> fileInfo = InitFileInfo(classesPath);

        MyRunnable myRunnable = new MyRunnable(getClassLoader(classesPath, libPath));
        myRunnable.startClass = startClass;

        Thread thread = new Thread(myRunnable);
        thread.setContextClassLoader(myRunnable.ccl);
        thread.start();

        // 检测文件变化并重启
        for (;;) {
            boolean newLoad = false;
            for (String key : fileInfo.keySet()) {
                File file = new File(key);
                Long time = fileInfo.get(key);
                if (FileUtils.isFileNewer(file, time)) {
                    fileInfo.put(key, file.lastModified());
                    if (!newLoad) {
                        System.out.println("-----------重新加载----------");
                        newLoad = true;
                        // 停止不了运行中的线程
                        {
                            thread.interrupt();
                            thread.stop();
                        }
                        myRunnable.ccl.close();
                        myRunnable.ccl = getClassLoader(classesPath, libPath);
                        thread = new Thread(myRunnable);
                        thread.setContextClassLoader(myRunnable.ccl);
                        thread.start();
                    }
                }
            }
            newLoad = false;
            Thread.sleep(scanInterval * 1000);
        }

    }

    private static CustomClassLoader getClassLoader(String[] classesPaths, String[] libPaths) {
        CustomClassLoader ccl = new CustomClassLoader();
        for (String classesPath : classesPaths) {
            ccl.addURL(new File(classesPath));
        }

        for (String libPath : libPaths) {
            File jarDirectory = new File(libPath);
            if (jarDirectory.isDirectory()) {
                Collection<File> listJars = FileUtils.listFiles(new File(libPath), new String[] { "jar" }, true);
                for (File jar : listJars) {
                    ccl.addURL(jar);
                }
            }
        }
        return ccl;
    }

    /**
     * 初始化文件信息
     * @param classesPath 类路径
     * @param libPath jar包路径
     */
    private static Map<String, Long> InitFileInfo(String[] classesPaths) {
        Map<String, Long> fileInfo = new HashMap<>();
        for (String classesPath : classesPaths) {
            Collection<File> listFiles = FileUtils.listFiles(new File(classesPath), new String[] { "class" }, true);
            for (File file : listFiles) {
                if (file.canRead()) {
                    fileInfo.put(file.getAbsolutePath(), file.lastModified());
                }
            }
        }

        return fileInfo;
    }

    /**
     * 获取配置文件
     * @param file 文件名
     * @return
     */
    private static Configuration getConfigurations(String[] files) {

        Configuration configurations = new Configuration();

        ClassLoader classLoader = HotStartApplication.class.getClassLoader();
        for (String file : files) {
            Configuration properties = Configuration.properties(classLoader.getResource(file));
            configurations.append(properties);
        }

        return configurations;
    }

    private static class MyRunnable implements Runnable {
        public CustomClassLoader ccl = null;
        public String startClass = null;

        public MyRunnable(CustomClassLoader ccl) {
            this.ccl = ccl;
        }

        @Override
        public void run() {
            try {
                ccl.findClass(startClass).getMethod("main", String[].class).invoke(null, (Object) new String[0]);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}

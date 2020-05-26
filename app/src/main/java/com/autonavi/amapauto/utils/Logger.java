package com.autonavi.amapauto.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.SystemClock;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;


public class Logger {

    private final static String CHARSETNAME_UTF8 = "UTF-8";

    public final static String KEY_IS_WRITE_LOG = "isWriteLog";

    /**
     * 默认日志的tag
     */
    private final static String DEFAULT_TAG = "tag_auto";

    /**
     * 日期格式
     */
    private static SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

    private static final String SEPARATOR = "{?}";
    /**
     * 是否打印日志到logcat
     */
    private static boolean isLogcat = false;

    private static FileLogger fileLogger = new FileLogger();

    public static boolean isLogCat() {
        return isLogcat;
    }

    public static void setLogCat(boolean isLogCat) {
        Logger.isLogcat = isLogCat;
    }

    public static void setLog(boolean isLog) {
        Logger.isLogcat = isLog;
    }

    /**
     * 是否需要打印日志。包括logcat，或者Log文件等方式。
     */
    public static boolean isLog() {
        return isLogcat || fileLogger.isLogFile();
    }

    public static void setMaxNum(int num){
        if(fileLogger != null){
            fileLogger.MAX_LOG_NUMBER = num;
        }
    }

    /**
     * 系统的application
     */
    private static Context applicationContext;

    /**
     * 初始化，引用的是系统的Context,所以不需要销毁
     *
     * @param context
     */
    public static void init(Context context) {
        applicationContext = context.getApplicationContext();
    }

    private static Object[] toParamsString(Object... params) {
        Object[] stringParams = null;
        if(params != null && params.length > 0) {
            final int length = params.length;
            stringParams = new Object[length];
            for (int i=0; i<length; i++){
                stringParams[i] = String.valueOf(params[i]);
            }
        }
        return stringParams;
    }

    /**
     * 打印debug级别的日志
     *
     * @param tag
     * @param msg    日期信息，参数采用{?}的形式， 例如 "这是个{?},继续打印其他参数"
     * @param params 参数信息，用来替换msg中的{?}
     */
    public static void d(String tag, String msg, Object... params) {
        Object[] stringParam = null;
        if (isLog()) {
            // 只有需要打印日志，才格式化参数
            stringParam = toParamsString(params);
        }
        //是否在控制台写日志
        if (isLogcat) {
            LoggerThread.getInstance().d(tag, msg, stringParam);
        }
        if (fileLogger.isLogFile()) {
            LoggerThread.getInstance().write(tag, msg, stringParam);
        }
    }

    /**
     * 打印error级别的日志
     *
     * @param tag
     * @param msg    日期信息，参数采用{?}的形式， 例如 "这是个{?},继续打印其他参数"
     * @param params 参数信息，用来替换msg中的{?}
     */
    public static void e(String tag, String msg, Throwable e, Object... params) {
        if(null==msg){
            msg="";
        }

        Object[] stringParam = null;
        if (isLog()) {
            // 只有需要打印日志，才格式化参数
            stringParam = toParamsString(params);
            msg += (e == null) ? "error is null" : Log.getStackTraceString(e);
        }
        if (isLogcat) {
            //防止控制台重复输出日志
            LoggerThread.getInstance().d(tag, msg, stringParam);
        }
        if (fileLogger.isLogFile()) {
            LoggerThread.getInstance().write(tag, msg, stringParam);
        }
    }

    /**
     * 打印debug级别的日志， 默认tag是@DEFAULT_TAG
     *
     * @param msg    日期信息，参数采用{?}的形式， 例如 "这是个{?},继续打印其他参数"
     * @param params 参数信息，用来替换msg中的{?}
     */
    @Deprecated
    public static void D(String msg, Object... params) {
        d(DEFAULT_TAG, msg, params);
    }

    /**
     * 打印error级别的日志， 默认tag是@DEFAULT_TAG
     *
     * @param msg    日期信息，参数采用{?}的形式， 例如 "这是个{?},继续打印其他参数"
     * @param params 参数信息，用来替换msg中的{?}
     */
    @Deprecated
    public static void E(String msg, Throwable e, Object... params) {
        e(DEFAULT_TAG, msg, e, params);
    }

    /**
     * 将消息重新组合格式化处理
     *
     * @param msg
     * @param params
     * @return
     */
    private static void msgFromParams(StringBuffer bf, String msg, Object... params) {
        if (msg == null) {
            return;
        }
        if (params == null) {
            bf.append(msg);
            return;
        }
        //String msgArray[] = msg.split("\\{\\?\\}");
        String[] msgArray = splitParams(msg);
        int minLen = Math.min(msgArray.length, params.length);
        for (int i = 0; i < minLen; i++) {
            Object param = params[i];
            bf.append(msgArray[i]).append(param);
        }
        for (int i = minLen; i < msgArray.length; i++) {
            bf.append(msgArray[i]);
        }
    }

    private static String[] splitParams(final String buffer){
        final ArrayList<String> msgList = new ArrayList<>();
        final int separatorLen = SEPARATOR.length();
        int start = 0;
        int end = buffer.indexOf(SEPARATOR);
        while (end >= 0){
            msgList.add(buffer.substring(start, end));
            start = end+separatorLen;
            end = buffer.indexOf(SEPARATOR, start);
        }
        if (start < buffer.length()-1){
            msgList.add(buffer.substring(start));
        }

        return msgList.toArray(new String[0]);
    }

    public static void d(Class c, String msg, Object... params) {
        d(c.getSimpleName(), msg, params);
    }

    /**
     * Logger的线程销毁。 其实这个线程不销毁也没关系
     */
    public static void destory() {
        if (isLog()) {
            LoggerThread.getInstance().destory();
        }
    }

    public static void checkFile(){
        if(fileLogger != null) {
            fileLogger.checkLogFile();
        }
    }


    /**
     *  创建日志
     */
    public static boolean createLogDir(){
        String filePath = getLogFileDir(applicationContext);
        File tempFile = new File(filePath);
        if(tempFile.exists()){
            if(!tempFile.isDirectory()) {
                tempFile.delete();
            }
        }
        return tempFile.mkdirs();
    }

    /**
     * 获取日志的路径
     * @param context
     * @return
     */
    public static String getLogFileDir(Context context){
        return "/sdcard/amapauto9/gdarcameraclient/";
    }

    private static class FileLogger {

        /**
         * 文件大小20M
         */
        private final static long MAX_FILE_SIZE = 20*1024*1024;
        /**
         * 日志文件个数（实际个数需加2，包括autolog.log，及autoglog.log.MAX_LOG_NUMBER+1）
         */
        public  static int MAX_LOG_NUMBER = 8;

        private static final String FILE_NAME = "autolog.log";

        private String filePath = null;

        /**
         * 校验写日志的文件是否存在
         */
        private boolean isCheckedLogDir = false;
        /**
         * 要写日志的文件是存在
         */
        private boolean isLogFileExist = false;
        /**
         * 是否要写文件
         */
        private boolean isWriteLog;
        /**
         * 当前的日志文件
         */
        private File mCurrentLogFile;
        /**
         * 缓存的日志信息
         */
        private StringBuffer bf = new StringBuffer();
        /**
         * 最后一次保存日志时间
         */
        private long lastSaveLogTime = 0;

        private final int MAX_CACHE_SIZE = 200 * 1024;


        /**
         * 校验是否存在写日志目录
         */
        private void checkLogFile() {
            if (applicationContext == null) {
                return;
            }
            filePath = getLogFileDir(applicationContext);
            File tempFile = new File(filePath);
            if(tempFile.exists()){
               if(!tempFile.isDirectory()) {
                   tempFile.delete();
               }
            }
            //TODO 正式版本，就不需要默认创建了
//            tempFile.mkdirs();

            isCheckedLogDir = true;
            /**
             * 这里对MapSharePreference 的操作，采用直接从applicationContext
             * 这样避免采用MapSharePreference，从而引入其它的插件信息
             * @author xiangdong.wu
             * @since 2015/12/17
             */
//			MapSharePreference msp = new MapSharePreference(MapSharePreference.SharePreferenceName.SharedPreferences);
//			isWriteLog = msp.getBooleanValue(MapSharePreference.SharePreferenceKeyEnum.isWriteLog, IS_WRITE_LOG);

            SharedPreferences msp = applicationContext.getSharedPreferences("SharedPreferences", 0);
            isWriteLog = msp.getBoolean(KEY_IS_WRITE_LOG, true);
            if (!isWriteLog) {
                return;
            }

            File filepath = new File(filePath);
            if (filepath.exists() && filepath.isDirectory()) {
                this.isLogFileExist = true;
            }
        }

        /**
         * 在写日志
         * @param aData
         * @param append
         * @return
         */
        private boolean writeFile(byte[] aData, boolean append) {
            if (applicationContext == null) {
                return false;
            }
            OutputStream out = null;
            boolean ok = false;
            try {
                out = new FileOutputStream(mCurrentLogFile, append);
                out.write(aData);
                ok = true;
            } catch (Exception e) {
            } finally {
                if (out != null) {
                    try {
                        out.close();
                    } catch (IOException e) {
                    }
                }
            }

            return ok;
        }

        /**
         * 创建新的目录。
         * 如果文件目录太大，则往后移动，最多保存 MAX_LOG_NUMBER 个 MAX_FILE_SIZE 的日志文件
         */
        private void creteFile(){
            boolean createNewFile = false;
            if(mCurrentLogFile == null){
                mCurrentLogFile = new File(filePath ,FILE_NAME);
            }
            try {
                if (!mCurrentLogFile.exists()) {
                    /**
                     * 如果文件的上级目录不存在，则不创建文件。
                     * 场景：有可能在写日志过程中，上级目录被删除导致
                     */
                    if(!mCurrentLogFile.getParentFile().exists()){
                        return;
                    }
                    mCurrentLogFile.createNewFile();
                    createNewFile = true;
                } else {
                    //文件太大，则重命名旧文件
                    if(mCurrentLogFile.length() > MAX_FILE_SIZE){
                        resetLogFiles();
                        createNewFile = true;
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        /**
         * log文件重新命名,并删除超过最大个数的那个文件
         */
        private void resetLogFiles() throws IOException {
            String path = mCurrentLogFile.getParent();
            String fileName = mCurrentLogFile.getName();
            File deleteFile = new File(path ,mCurrentLogFile.getName() +"."+ (MAX_LOG_NUMBER + 1));
            if(deleteFile.exists()){
                deleteFile.delete();
            }
            //所有文件往后重命名一个
            for(int i = MAX_LOG_NUMBER; i > 0; i--){
                File f = new File(path,mCurrentLogFile.getName() + "." +i);
                if(f.exists()){
                    f.renameTo(new File(path,mCurrentLogFile.getName()+"." + (i+1)));
                }
            }
            //当前名字加了后缀1
            mCurrentLogFile.renameTo(new File(path,mCurrentLogFile.getName()+"." + 1));
            mCurrentLogFile = new File(path,fileName);
            mCurrentLogFile.createNewFile();
        }




        /**
         * 开始写日志信息
         *
         * @param tag
         * @param milliseconds 打印日志时间
         * @param threadName 打印日志的线程名称
         * @param msg 日志主题内容 以{?}作为参数信息
         * @param params 参数信息
         */
        public void write(String tag, long milliseconds, String threadName, String msg, Object... params) {
            formatDate(bf, milliseconds);
            bf.append("##");
            bf.append("/").append(tag).append(":");
            bf.append("[").append(threadName).append("]");
            msgFromParams(bf, msg, params);
            bf.append("\r\n");

            /**
             * 200K或2s写一次日志,这样降低IO操作频率
             */
            boolean flush = false;
            if(bf.length() >= MAX_CACHE_SIZE) {
                flush = true;
                bf.append("-------------！！！！警告！！！！警告 【log输出太频繁，请检查2秒内的log输出情况】-------------\r\n");
            } else if(SystemClock.elapsedRealtime() - lastSaveLogTime > 2000) {
                flush = true;
            }
            if(flush){
                try {
                    writeFile(bf.toString().getBytes(CHARSETNAME_UTF8), true);
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                lastSaveLogTime = SystemClock.elapsedRealtime();
                bf.setLength(0);
            }

        }

        /**
         * 判断是否需要打印日志
         *
         * @return
         */
        public boolean isLogFile() {
            //如果没有检测过目录是否存在
            if (!isCheckedLogDir) {
                checkLogFile();
            }
            //打开日志的开关是是否存在
            if (isCheckedLogDir && isWriteLog && isLogFileExist) {
                return true;
            }
            return false;
        }

    }

    /**
     * 打印日志的线程.
     * 在这一新的线程中打印日志
     */
    private static class LoggerThread {
        private static LoggerThread instance;
        private List<Runnable> taskLogList = Collections.synchronizedList(new LinkedList<Runnable>());//Collections.synchronizedList(new ArrayList<Runnable>());
        /**
         * 同步锁
         */
        private final Object locker = new Object();

        /**
         * 线程是否在跑，这里暂时不会停掉，由于单例模式
         */
        private boolean isRunning;

        private LoggerThread() {
            initThread();
        }

        private static LoggerThread getInstance() {
            if (instance == null) {
                instance = new LoggerThread();
            }
            return instance;
        }

        public void d(final String tag, final String msg, final Object... params) {
            taskLogList.add(new Runnable() {
                @Override
                public void run() {
                    StringBuffer bf = new StringBuffer();
                    //logcat 打印的话，会打印时间了，这里不需要在打印出来
//                    formatDate(bf,milliseconds);
//                    bf.append(":");
                    msgFromParams(bf, msg, params);
                    Log.d(tag, bf.toString());
                }
            });
            notifyLock();
        }


        public void e(final String tag, final String msg, final Throwable e, final Object... params) {
            taskLogList.add(new Runnable() {
                @Override
                public void run() {
                    StringBuffer bf = new StringBuffer();
                    //logcat 打印的话，会打印时间了，这里不需要在打印出来
//                    formatDate(bf, milliseconds);
//                    bf.append(":");
                    msgFromParams(bf, msg, params);
                    Log.e(tag, bf.toString(), e);
                }
            });
            notifyLock();
        }

        /**
         * 真正写日志进程
         *
         * @param tag
         * @param msg
         * @param params
         */
        public void write(final String tag, final String msg, final Object... params) {
            final long now = System.currentTimeMillis();
            final String currentThreadName = Thread.currentThread().getName();
            taskLogList.add(new Runnable() {
                @Override
                public void run() {
                    fileLogger.creteFile();
                    fileLogger.write(tag, now,currentThreadName, msg, params);
                }
            });
            notifyLock();
        }

        /**
         * 这里采用延迟的方式，避免在销毁之前的前一段日志无法打印
         */
        public void destory() {
            //暂时先不需要销毁，现在有问题，就是退出后，状态还保存，下次再请求，没办法进行初始化优化
        }

        private void setLowThreadPriority() {
            try {
                android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_LOWEST);
                Thread.currentThread().setPriority(Thread.MIN_PRIORITY);
            } catch (Throwable e) {
            }
        }
        /**
         * 初始化线程
         */
        private void initThread(){
            isRunning = true;
            new Thread(new Runnable() {
                @Override
                public void run() {
                    setLowThreadPriority();
                    while (isRunning){
                        try {
                            excuteTask();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
            },"ALLoggerThread").start();
        }

        /**
         * 真正执行打印任务日志
         * @throws InterruptedException
         */
        private void excuteTask() throws InterruptedException {
            int logSize = taskLogList.size();
//            Log.d("tag_auto", "excute logger size = " + logSize );
            if(logSize > 0){
                /**
                 * Auto001-9455 复制- MonkeyLog: (CRASH) java.lang.ArrayIndexOutOfBoundsException
                 * 写日志出现数组越界，由于日志这块，不是必须的，所以用stringBuffer这个非线程安全的方法来处理。
                 * @author xiangdong.wu
                 * @since 2016/06/21
                 */
                try {
                    Runnable r = taskLogList.remove(0);
                    r.run();
                }catch(Throwable e){
                }
            }
            else{
                //没有数据，则等待
                synchronized (locker) {
                    while (taskLogList.isEmpty()){
                        locker.wait();
                    }
                }
            }
        }
        /**
         * 通知解锁，让日志任务开始
         */
        private void notifyLock(){
            try {
                synchronized (locker) {
                    locker.notify();
                }
            } catch (Throwable e) {
            }
        }

    }

    private static Calendar calendar = null;
    /**
     * 格式化日期
     * @param milliseconds
     * @return
     */
    private static void formatDate(StringBuffer bf, long milliseconds){

        if(calendar == null){
            calendar = Calendar.getInstance();
        }
        calendar.setTimeInMillis(milliseconds);
        bf.append(calendar.get(Calendar.YEAR)).append("-");
        bf.append(calendar.get(Calendar.MONTH)+1).append("-");
        bf.append(calendar.get(Calendar.DAY_OF_MONTH)).append(" ");
        bf.append(calendar.get(Calendar.HOUR_OF_DAY)).append(":");
        bf.append(calendar.get(Calendar.MINUTE)).append(":");
        bf.append(calendar.get(Calendar.SECOND)).append(".");
        bf.append(calendar.get(Calendar.MILLISECOND)).append(" ");
    }
}

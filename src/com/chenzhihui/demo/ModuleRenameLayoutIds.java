package com.chenzhihui.demo;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 将layout文件里面的id统一增加前缀
 * <p>
 * 实现步骤，先将全部@+id/全部找出来存起来
 * 然后再来替换
 */
public class ModuleRenameLayoutIds {

    public static final String FILE_JAVA = ".java";
    public static final String FILE_START_DOT = ".";
    private static final String FILE_XML = ".xml";

    //要重命名的module的绝对路径
    public static String renameModulePath = "/Users/chenzhihui193/Documents/GitHub/Demos/mylibrary";
    public static String workSpaceDir = renameModulePath + "/src/main/";
    //String文件的地址
    public static String renameResPath = workSpaceDir + "res/";
    public static String layoutXmlPath = renameResPath + "layout/";
    //要添加的前缀 会加上下划线 所以不用额外加上下划线
    public static String modulePrefix = "aaa";

    public static void main(String[] args) {
        File[] workFiles = new File(layoutXmlPath).listFiles();
        List<String> ids = new ArrayList<>();
        if (workFiles != null) {
            for (File fileItem : workFiles) {
                System.out.println("开始处理" + fileItem.getName());
                ids.addAll(readIdsFromLayoutFiles(fileItem));
            }
        }
        System.out.println("查找id完成,id个数为" + ids.size());
        renameIds(ids);
    }

    public static void renameIds(List<String> ids) {
        int completedCount = 0;
        int totalSize = ids.size();
        for (String id : ids) {
            String newId = modulePrefix + "_" + id;
            System.out.println("开始处理：" + id);
            File[] workFiles = new File(workSpaceDir).listFiles();
            if (workFiles != null) {
                for (File fileItem : workFiles) {
                    if (fileItem != null && fileItem.isDirectory()) {
                        resolveLinks(id, newId, fileItem.getPath());
                    } else {
                        doCheckFile(id, newId, fileItem);
                    }
                }
            }
            completedCount++;
            System.out.println(completedCount + "/" + totalSize + "  " + id + "处理完成");
        }
        System.out.println("替换完成");
    }

    /**
     * 解决重命名之后的引用问题
     *
     * @param oldStringValue 老文件名
     * @param newStringValue 新文件名
     * @param workSpacePath  工作的文件路径
     */
    public static void resolveLinks(String oldStringValue, String newStringValue, String workSpacePath) {
        File workSpaceDir = new File(workSpacePath);
        File[] workFiles = workSpaceDir.listFiles();
        if (workFiles != null) {
            for (File fileItem : workFiles) {
                if (fileItem != null && fileItem.isDirectory()) {
                    resolveLinks(oldStringValue, newStringValue, fileItem.getPath());
                } else {
                    doCheckFile(oldStringValue, newStringValue, fileItem);
                }
            }
        }
    }


    /**
     * 修改文件
     *
     * @param file
     * @param oldstr
     * @param newStr
     * @return
     */
    public static boolean modifyFileContent1(File file, String oldstr, String newStr, Boolean isJavaFile) {
        boolean hasReturn = hasReturnInEndOfFile(file);
        FileReader in = null;
        try {
            in = new FileReader(file);

            BufferedReader bufIn = new BufferedReader(in);

            // 内存流, 作为临时流
            CharArrayWriter tempStream = new CharArrayWriter();

            // 替换
            String line = null;
            String nextLine = null;
            int count = 0;
            line = bufIn.readLine();
            while (line != null) {
                count++;
                line = doReplaceString(isJavaFile, oldstr, newStr, line);
            /*){
                System.out.println("【资源修改】："+line+"，第"+count+"行开始");
                // 替换每行中, 符合条件的字符串
                line = line.replaceAll(oldstr, newStr);
                System.out.println("【资源修改】："+line+"，第"+count+"行完成");
            }*/
                // 将该行写入内存
                tempStream.write(line);
                nextLine = bufIn.readLine();
                if (nextLine == null) {
                    // 添加换行符
                    if (hasReturn) {
                        tempStream.append(System.getProperty("line.separator"));
                    } else {

                    }
                    break;
                } else {
                    // 添加换行符
                    tempStream.append(System.getProperty("line.separator"));
                    line = nextLine;
                }
            }

            // 关闭 输入流
            bufIn.close();

            // 将内存中的流 写入 文件
            FileWriter out = new FileWriter(file);
            tempStream.writeTo(out);
            out.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return true;
    }

    public static boolean hasReturnInEndOfFile(File file) {
        try (RandomAccessFile raFile = new RandomAccessFile(file, "rw");) {
            raFile.seek(file.length() - 1);
            int flag = raFile.read();
            Boolean hasReturn = 10 == flag;
            raFile.close();
            return hasReturn;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    /**
     * 替换字符串里面匹配的字符串
     *
     * @param isJavaFile
     * @param oldstr
     * @param newStr
     * @param line
     * @return
     */
    public static String doReplaceString(Boolean isJavaFile, String oldstr, String newStr, String line) {
        String wrappLine = ";" + line + ";";//进行前后加字符来匹配所需要的字符（如果前后位空行，readline出来的字符串前面后都没有字符的话如果匹配字符的非字符和数字和下划线）
        String noNumNoLetterReg = "([^_|a-z|A-Z|0-9])";//非字母非数字或者为空的字符
        if (isJavaFile) {
            //前后都不是数字文字下划线之外的字符或者是后面或者前面什么字符都没有
            String reg = noNumNoLetterReg + "(R.id." + oldstr + ")" + noNumNoLetterReg;
            Pattern p = Pattern.compile(reg);

            Matcher matcher = p.matcher(wrappLine);
            if (matcher.find()) {
                System.out.println("java文件中匹配到" + wrappLine);
                System.out.println("替换前：" + line);
                line = wrappLine.replaceAll(reg, "$1R.id." + newStr + "$3");//只替换前面的字符串
                line = line.replaceFirst(";", "");
                line = line.substring(0, line.length() - 1);
                System.out.println("替换后：" + line);
            }
        } else {
            //如果是xml的文件匹配 前后都不是数字文字下划线之外的字符或者是后面或者前面什么字符都没有

            //处理@id
            String reg = "@id/" + oldstr + "";
            Pattern p = Pattern.compile(reg);
            Matcher matcher = p.matcher(wrappLine);
            if (matcher.find()) {
                System.out.println("xml文件中匹配到" + wrappLine);
                System.out.println("替换前：" + line);
                line = wrappLine.replaceAll(reg, "@id/" + newStr);
                line = line.replaceFirst(";", "");
                line = line.substring(0, line.length() - 1);
                System.out.println("替换后：" + line);
            } else {//如果没有匹配到就直接返回就好了

            }

            //处理@+id
            String reg2 = "@\\+id/" + oldstr;
            Pattern p2 = Pattern.compile(reg2);
            Matcher matcher2 = p2.matcher(wrappLine);
            if (matcher2.find()) {
                System.out.println("xml文件中匹配到" + wrappLine);
                System.out.println("替换前：" + line);
                line = wrappLine.replaceAll(reg2, "@+id/" + newStr);
                line = line.replaceFirst(";", "");
                line = line.substring(0, line.length() - 1);
                System.out.println("替换后：" + line);
            } else {//如果没有匹配到就直接返回就好了

            }
        }
        return line;
    }


    /**
     * 对单个文件进行读取和检查
     *
     * @param oldStringValue 不包含文件后缀
     * @param newStringValue 不包含文件后缀
     * @param fileItem       要检查的java或者xml文件
     */
    public static void doCheckFile(String oldStringValue, String newStringValue, File fileItem) {
        if (filterFile(fileItem.getName())) {
//            System.out.println("检查" + fileItem + "  " + oldStringValue + "的引用");
            modifyFileContent1(fileItem, oldStringValue, newStringValue, fileItem.getName().endsWith(FILE_JAVA));
        }
    }

    /**
     * 过滤了相关文件
     *
     * @param name
     * @return
     */
    public static boolean filterFile(String name) {
        return name != null && (name.endsWith(FILE_JAVA) || name.endsWith(FILE_XML));
    }


    //找出所有id
    public static List<String> readIdsFromLayoutFiles(File file) {
        List<String> result = new ArrayList<>();
        FileReader in = null;
        try {
            in = new FileReader(file);
            BufferedReader bufIn = new BufferedReader(in);
            String line = null;
            String nextLine = null;
            int count = 0;
            line = bufIn.readLine();
            while (line != null) {
                count++;
                String reg = "^android:id=\"@\\+id.*\"$";
                Pattern p = Pattern.compile(reg);
                String trimLine = line.trim();
                Matcher matcher = p.matcher(trimLine);
                if (matcher.find()) {
                    int startIndex = trimLine.indexOf("/");
                    int endIndex = trimLine.lastIndexOf("\"");
                    result.add(trimLine.substring(startIndex + 1, endIndex));
                }
                nextLine = bufIn.readLine();
                line = nextLine;
            }
            // 关闭 输入流
            bufIn.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return result;
    }
}

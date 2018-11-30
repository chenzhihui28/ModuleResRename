package com.chenzhihui.demo;

import java.io.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 资源文件重命名增加前缀
 */
public class ModuleRenameResName {

    public static final String FILE_JAVA = ".java";
    public static final String FILE_START_DOT = ".";
    public static final String FILE_XML = ".xml";

    //要重命名的module的绝对路径
    public static String renameModulePath = "/Users/chenzhihui193/Documents/GitHub/Demos/mylibrary";
    public static String workSpaceDir = renameModulePath + "/src/main/";
    public static String renameResPath = workSpaceDir + "res/";
    //要添加的前缀 会加上下划线 所以不用额外加上下划线
    public static String modulePrefix = "aaa";

    public static void main(String[] args) {
        doRenameTask();
    }


    /**
     * 重命名资源的名字task
     */
    public static void doRenameTask() {
        File fileDir = new File(renameModulePath);
        if (fileDir != null) {
            File renameFileDir = new File(renameResPath);
            renameResFiles(renameFileDir.listFiles());
        }
    }

    public static void renameResFiles(File[] files) {
        if (files == null) {
            System.out.println("进行重命名的文件下没有任何文件");
            return;
        }
        for (File fileItem : files) {
            if (fileItem != null && fileItem.isDirectory() && filterReplaceResFile(fileItem.getName())) {
                renameResFiles(fileItem.listFiles());
            } else {
                renameSingleFile(fileItem);
            }
        }
    }

    public static boolean filterReplaceResFile(String fileItem) {
        return fileItem != null && (!fileItem.startsWith(FILE_START_DOT));
    }

    /**
     * 重命名单个文件
     *
     * @param fileItem
     */
    public static void renameSingleFile(File fileItem) {
        System.out.println("重命名的文件：" + fileItem.getParent() + "/" + fileItem.getName());
        String path = fileItem.getParent();
        String fileName = fileItem.getName();
        if (!fileName.startsWith(modulePrefix)) {
            String newFileName = modulePrefix + "_" + fileName;
            if (fileItem.renameTo(new File(path + "/" + newFileName))) {
                System.out.println("重命名为" + newFileName + "成功!准备解决引用");
                resolveLinks(path, fileName.substring(0, fileName.indexOf(".")), newFileName.substring(0, newFileName.indexOf(".")), workSpaceDir);
            } else {
                System.out.println(path + fileName + "修改失败");
            }
        }


    }

    /**
     * 解决重命名之后的引用问题
     *
     * @param fileName      老文件名
     * @param newFileName   新文件名
     * @param workSpacePath 工作的文件路径
     */
    public static void resolveLinks(String path, String fileName, String newFileName, String workSpacePath) {
        File workSpaceDir = new File(workSpacePath);
        File[] workFiles = workSpaceDir.listFiles();
        if (workFiles != null) {
            for (File fileItem : workFiles) {
                if (fileItem != null && fileItem.isDirectory()) {
                    resolveLinks(path, fileName, newFileName, fileItem.getPath());
                } else {
                    doCheckFile(path, fileName, newFileName, fileItem);
                }
            }
        }
    }

    /**
     * 对单个文件进行读取和检查
     *
     * @param fileName    不包含文件后缀
     * @param newFileName 不包含文件后缀
     * @param fileItem    要检查的java或者xml文件
     */
    public static void doCheckFile(String path, String fileName, String newFileName, File fileItem) {
        if (filterFile(fileItem.getName())) {
            System.out.println("path = " + path + " fileItem = " + fileItem + " fileName = " + fileName + " newFileName = " + newFileName + " isJavaFile = " + fileItem.getName().endsWith(FILE_JAVA));
            modifyFileContent1(path, fileItem, fileName, newFileName, fileItem.getName().endsWith(FILE_JAVA));
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


    /**
     * 修改文件
     *
     * @param file
     * @param oldstr
     * @param newStr
     * @return
     */
    public static boolean modifyFileContent1(String path, File file, String oldstr, String newStr, Boolean isJavaFile) {
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
                line = doReplaceString(isJavaFile, oldstr, newStr, line, getPathSuffix(path));
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

    /**
     * 根据资源文件的path来的后缀来决定要改字符串的前缀，R.drawable.xxx 要修改drawable类型的所有符合条件的字符
     *
     * @param path
     * @return
     */
    public static String getPathSuffix(String path) {
        String sufix = path.substring(path.lastIndexOf("/") + 1, path.length());
        if (sufix.contains("-")) {//针对drawable-hdpi类型的文件夹
            return sufix.substring(0, sufix.indexOf("-"));
        }
        return sufix;
    }

    /**
     * 替换字符串里面匹配的字符串
     *
     * @param isJavaFile
     * @param oldstr
     * @param newStr
     * @param line
     * @param endParentPath
     * @return
     */
    public static String doReplaceString(Boolean isJavaFile, String oldstr, String newStr, String line, String endParentPath) {
        System.out.println("doReplaceString oldstr = "+oldstr+" newStr = "+newStr+" line = "+line+" endParentPath = "+endParentPath);
        if (endParentPath.equals("values")) {//values资源文件夹下的东西直接重命名
            return line;
        } else {
            endParentPath = endParentPath;
        }
        String wrappLine = ";" + line + ";";//进行前后加字符来匹配所需要的字符（如果前后位空行，readline出来的字符串前面后都没有字符的话如果匹配字符的非字符和数字和下划线）
        String noNumNoLetterReg = "([^_|a-z|A-Z|0-9])";//非字母非数字或者为空的字符
        if (isJavaFile) {//前后都不是数字文字下划线之外的字符或者是后面或者前面什么字符都没有
//             System.out.println("替换前："+line);

            String reg = noNumNoLetterReg + "(R." + endParentPath + "." + oldstr + ")" + noNumNoLetterReg;
            Pattern p = Pattern.compile(reg);

            Matcher matcher = p.matcher(wrappLine);
            if (matcher.find()) {

                System.out.println("java文件中匹配到" + wrappLine);
                line = wrappLine.replaceAll(reg, "$1R." + endParentPath + "." + newStr + "$3");//只替换前面的字符串
                line = line.replaceFirst(";", "");
                line = line.substring(0, line.length() - 1);
            }

//             System.out.println("替换后："+line);
        } else {//如果是xml的文件匹配 前后都不是数字文字下划线之外的字符或者是后面或者前面什么字符都没有
//             System.out.println("替换前："+line);
            String reg = noNumNoLetterReg + "(@" + endParentPath + "/" + oldstr + ")" + noNumNoLetterReg;
//            System.out.println(reg);
            Pattern p = Pattern.compile(reg);
            Matcher matcher = p.matcher(wrappLine);
//             return  line.matches("R."+endParentPath+"."+oldstr+"[^a-zA-Z0-9]*");
            if (matcher.find()) {
                System.out.println("xml文件中匹配到" + wrappLine);
                line = wrappLine.replaceAll(reg, "$1@" + endParentPath + "/" + newStr + "$3");
                line = line.replaceFirst(";", "");
                line = line.substring(0, line.length() - 1);
            } else {//如果没有匹配到就直接返回就好了

            }
        }
        return line;
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
}

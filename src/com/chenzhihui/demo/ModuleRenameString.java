package com.chenzhihui.demo;

import org.dom4j.Attribute;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * 修改strings.xml里面的资源名，增加前缀
 */
public class ModuleRenameString {

    public static final String FILE_JAVA = ".java";
    public static final String FILE_START_DOT = ".";
    private static final String FILE_XML = ".xml";

    //要重命名的module的绝对路径
    public static String renameModulePath = "/Users/chenzhihui193/Documents/GitHub/Demos/mylibrary";
    public static String workSpaceDir = renameModulePath + "/src/main/";
    //String文件的地址
    public static String stringXmlFileNmae = "strings.xml";
    public static String renameResPath = workSpaceDir + "res/";
    public static String stringXmlPath = renameResPath + "values/" + stringXmlFileNmae;
    //要添加的前缀 会加上下划线 所以不用额外加上下划线
    public static String modulePrefix = "aaa";

    public static void main(String[] args) {
        List<String> changedList = renameStringXml();
        if (changedList == null || changedList.isEmpty()) {
            System.out.println("没有修改记录");
            return;
        }
        solveStringReferences(changedList);
    }


    //改string.xml里面的key
    public static List<String> renameStringXml() {
        List<String> modifiedList = new ArrayList<>();
        try {
            SAXReader reader = new SAXReader();
            Document doc = reader.read(new File(stringXmlPath));
            Element root = doc.getRootElement();
            List<Element> list = root.elements();

            boolean modified = false;
            for (Element item : list) {
                System.out.println("key = " + item.attribute("name").getValue() + " value = " + item.getStringValue());
                Attribute attribute = item.attribute("name");
                String attributeValue = attribute.getValue();
                if (!attribute.getValue().startsWith(modulePrefix)) {
                    modifiedList.add(attributeValue);
                    attribute.setValue(modulePrefix + "_" + attributeValue);
                    modified = true;
                }
            }
            if (modified) {
                //指定文件输出的位置
                FileOutputStream out = new FileOutputStream(stringXmlPath);
                // 指定文本的写出的格式：  //漂亮格式：有空格换行
                OutputFormat format = OutputFormat.createPrettyPrint();
                format.setEncoding("UTF-8");
                //1.创建写出对象
                XMLWriter writer = new XMLWriter(out, format);
                //2.写出Document对象
                writer.write(doc);
                //3.关闭流
                writer.close();
                System.out.println("修改完毕!");
            } else {
                System.out.println("没有需要修改的!");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return modifiedList;
    }

    //修改对string.xml里面的引用
    public static void solveStringReferences(List<String> modifiedList) {
        try {
            for (String oldStringValue : modifiedList) {
                String newStringValue = modulePrefix + "_" + oldStringValue;
                System.out.println("开始处理：" + oldStringValue);
                File[] workFiles = new File(workSpaceDir).listFiles();
                if (workFiles != null) {
                    for (File fileItem : workFiles) {
                        if (fileItem != null && fileItem.isDirectory()) {
                            resolveLinks(oldStringValue, newStringValue, fileItem.getPath());
                        } else {
                            doCheckFile(oldStringValue, newStringValue, fileItem);
                        }
                    }
                }
                System.out.println(oldStringValue + "处理完成");
            }
            System.out.println("完成!");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 解决重命名之后的引用问题
     *
     * @param oldStringValue
     * @param newStringValue
     * @param workSpacePath 工作的文件路径
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
//        System.out.println("doReplaceString oldstr = " + oldstr + " newStr = " + newStr + " line = " + line);
        String wrappLine = ";" + line + ";";//进行前后加字符来匹配所需要的字符（如果前后位空行，readline出来的字符串前面后都没有字符的话如果匹配字符的非字符和数字和下划线）
        String noNumNoLetterReg = "([^_|a-z|A-Z|0-9])";//非字母非数字或者为空的字符
        if (isJavaFile) {
            //前后都不是数字文字下划线之外的字符或者是后面或者前面什么字符都没有
            String reg = noNumNoLetterReg + "(R.string." + oldstr + ")" + noNumNoLetterReg;
            Pattern p = Pattern.compile(reg);

            Matcher matcher = p.matcher(wrappLine);
            if (matcher.find()) {
                System.out.println("java文件中匹配到" + wrappLine);
                System.out.println("替换前：" + line);
                line = wrappLine.replaceAll(reg, "$1R.string." + newStr + "$3");//只替换前面的字符串
                line = line.replaceFirst(";", "");
                line = line.substring(0, line.length() - 1);
                System.out.println("替换后：" + line);
            }
        } else {
            //如果是xml的文件匹配 前后都不是数字文字下划线之外的字符或者是后面或者前面什么字符都没有

            String reg = noNumNoLetterReg + "(@string/" + oldstr + ")" + noNumNoLetterReg;
//            System.out.println(reg);
            Pattern p = Pattern.compile(reg);
            Matcher matcher = p.matcher(wrappLine);
//             return  line.matches("R."+endParentPath+"."+oldstr+"[^a-zA-Z0-9]*");
            if (matcher.find()) {
                System.out.println("xml文件中匹配到" + wrappLine);
                System.out.println("替换前：" + line);
                line = wrappLine.replaceAll(reg, "$1@string/" + newStr + "$3");
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
     * @param oldStringValue    不包含文件后缀
     * @param newStringValue 不包含文件后缀
     * @param fileItem    要检查的java或者xml文件
     */
    public static void doCheckFile(String oldStringValue, String newStringValue, File fileItem) {
        if (filterFile(fileItem.getName())) {
            System.out.println("检查" + fileItem + "  " + oldStringValue + "的引用");
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
        //这里只针对string.xml修改，所以要跳过string.xml
        return name != null && (name.endsWith(FILE_JAVA) || name.endsWith(FILE_XML)) && !name.equals(stringXmlFileNmae);
    }

}

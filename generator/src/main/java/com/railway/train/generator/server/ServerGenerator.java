package com.railway.train.generator.server;

import com.railway.train.generator.util.FreemarkerUtil;
import freemarker.template.TemplateException;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class ServerGenerator {
        // 输出文件保存路径，双反斜杠是Java字符串转义，表示Windows路径分隔符 \
        static String toPath = "generator\\src\\main\\java\\com\\railway\\train\\generator\\test\\";

        // 静态代码块：类加载的时候就执行，提前创建输出文件夹
        static {
            new File(toPath).mkdirs();
            // mkdirs()：多级目录创建；如果目录已存在，不会报错
        }

        public static void main(String[] args) throws IOException, TemplateException {
            // 1.初始化FreeMarker配置，加载模板文件 test.ftl
            FreemarkerUtil.initConfig("test.ftl");

            // 2.构造模板需要的数据模型，map里的key对应ftl模板里的变量 ${domain}
            Map<String, Object> param = new HashMap<>();
            param.put("domain", "Test");

            // 3.执行生成：读取test.ftl模板 + param数据，输出到 toPath + Test.java 文件
            FreemarkerUtil.generator(toPath + "Test.java", param);
        }
}


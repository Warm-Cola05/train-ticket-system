package com.railway.train.generator.server;

import com.railway.train.generator.util.FreemarkerUtil;
import freemarker.template.TemplateException;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Node;
import org.dom4j.io.SAXReader;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class ServerGenerator {
        // 输出文件保存路径，双反斜杠是Java字符串转义，表示Windows路径分隔符 \
        static String serverPath = "[module]/src/main/java/com/railway/train/[module]/";
        static String pomPath = "generator\\pom.xml";
        // 静态代码块：类加载的时候就执行，提前创建输出文件夹
        static {
            new File(serverPath).mkdirs();
            // mkdirs()：多级目录创建；如果目录已存在，不会报错
        }

        public static void main(String[] args) throws Exception {
            //获取mybatis-generator
            String generatorPath = getGenerator();
            //比如generator-config-member.xml，得到module=member
            String module = generatorPath.replace("src/main/resources/generator-config-","").replace(".xml","");
            System.out.println("module:"+module);
            serverPath = serverPath.replace("[module]", module);
            // new File(servicePath).mkdirs();
            System.out.println("serverPath:"+serverPath);


            //读取table节点
            Document document = new SAXReader().read("generator/"+generatorPath);
            // 查询文档里任意位置的 <table> 标签
            Node table = document.selectSingleNode("//table");
            System.out.println(table);

            // @属性名 是XPath语法：读取标签的属性
            Node tableName = table.selectSingleNode("@tableName");
            Node domainObjectName = table.selectSingleNode("@domainObjectName");

            System.out.println(tableName.getText() + "/" + domainObjectName.getText());


            // 示例：表名 jiawa_test
            // Domain = JiawaTest
            String Domain = domainObjectName.getText();

            // domain = jiawaTest
            String domain = Domain.substring(0, 1).toLowerCase() + Domain.substring(1);

            // do_main = jiawa-test
            String do_main = tableName.getText().replaceAll("_", "-");

            //组装参数
            Map<String, Object> param = new HashMap<>();
            param.put("module", module);
            param.put("Domain", Domain);
            param.put("domain", domain);
            param.put("do_main", do_main);
            System.out.println("组装参数："+param);


            gen(Domain, param, "service");
            gen(Domain, param, "controller");

        }

    private static void gen(String Domain, Map<String, Object> param,String target) throws IOException, TemplateException {
        // 1.初始化FreeMarker配置，加载模板文件 test.ftl
        FreemarkerUtil.initConfig(target+".ftl");
        String toPath = serverPath + target + "/";
        new File(toPath).mkdirs();
//            // 2.构造模板需要的数据模型，map里的key对应ftl模板里的变量 ${domain}
//            Map<String, Object> param = new HashMap<>();
//            param.put("domain", "Test");
        String Target = target.substring(0, 1).toUpperCase() + target.substring(1);
        String fileName= toPath + Domain + Target + ".java";
        System.out.println("输出文件："+fileName);
        // 3.执行生成：读取test.ftl模板 + param数据，输出到 toPath + Test.java 文件
        FreemarkerUtil.generator(fileName,param);
    }

    private static String getGenerator() throws DocumentException {
        // 创建dom4j读取器
        SAXReader saxReader = new SAXReader();

        // 【命名空间映射map】key=自定义前缀pom，value=pom.xml的命名空间URI
        Map<String, String> map = new HashMap<String, String>();
        map.put("pom", "http://maven.apache.org/POM/4.0.0");

        // 把命名空间注册给XPath解析工厂
        saxReader.getDocumentFactory().setXPathNamespaceURIs(map);

        // 读取pom.xml磁盘文件，得到XML文档对象
        Document document = saxReader.read(pomPath);

        // XPath查询：//pom:configurationFile
        // pom: 就是上面注册的前缀，代表maven的命名空间
        Node node = document.selectSingleNode("//pom:configurationFile");

        // 获取标签里面的文本内容
        System.out.println(node.getText());
        return node.getText();
    }
}


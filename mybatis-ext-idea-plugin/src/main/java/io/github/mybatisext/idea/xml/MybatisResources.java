package io.github.mybatisext.idea.xml;

import com.intellij.javaee.ResourceRegistrar;
import com.intellij.javaee.StandardResourceProvider;

/** 离线 MyBatis DTD 来自随插件打包的对应版本 MyBatis 库。 */
public final class MybatisResources implements StandardResourceProvider {

    @Override
    public void registerResources(ResourceRegistrar registrar) {
        for (String kind : new String[]{"mapper", "config"}) {
            for (String scheme : new String[]{"http", "https"}) {
                for (String host : new String[]{"mybatis.org", "www.mybatis.org"}) {
                    registrar.addStdResource(scheme + "://" + host + "/dtd/mybatis-3-" + kind + ".dtd", "/org/apache/ibatis/builder/xml/mybatis-3-" + kind + ".dtd", MybatisResources.class);
                }
            }
        }
    }
}

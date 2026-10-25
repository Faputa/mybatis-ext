package io.github.mybatisext.idea.xml;

import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.jetbrains.annotations.NotNull;

import com.intellij.ide.highlighter.XmlFileType;
import com.intellij.util.indexing.DataIndexer;
import com.intellij.util.indexing.FileBasedIndex;
import com.intellij.util.indexing.FileContent;
import com.intellij.util.indexing.ID;
import com.intellij.util.indexing.ScalarIndexExtension;
import com.intellij.util.io.EnumeratorStringDescriptor;
import com.intellij.util.io.KeyDescriptor;

/** 仅索引 namespace 和 configuration 标记，由 PSI 解析已索引文件中的 ID。 */
public final class MapperIndex extends ScalarIndexExtension<String> {

    public static final ID<String, Void> NAME = ID.create("io.github.mybatisext.mapper");

    @Override
    public @NotNull ID<String, Void> getName() {
        return NAME;
    }

    @Override
    public int getVersion() {
        return 1;
    }

    @Override
    public boolean dependsOnFileContent() {
        return true;
    }

    @Override
    public @NotNull KeyDescriptor<String> getKeyDescriptor() {
        return EnumeratorStringDescriptor.INSTANCE;
    }

    @Override
    public @NotNull FileBasedIndex.InputFilter getInputFilter() {
        return file -> file.getFileType() == XmlFileType.INSTANCE;
    }

    @Override
    public @NotNull DataIndexer<String, Void, FileContent> getIndexer() {
        return content -> {
            Map<String, Void> keys = new HashMap<>();
            XMLInputFactory factory = XMLInputFactory.newFactory();
            factory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
            factory.setProperty("javax.xml.stream.isSupportingExternalEntities", false);
            XMLStreamReader reader = null;
            try {
                reader = factory.createXMLStreamReader(new StringReader(content.getContentAsText().toString()));
                while (reader.hasNext()) {
                    if (reader.next() == XMLStreamConstants.START_ELEMENT) {
                        if ("mapper".equals(reader.getLocalName())) {
                            String ns = reader.getAttributeValue(null, "namespace");
                            if (ns != null && !ns.isBlank()) {
                                keys.put("mapper:" + ns, null);
                            }
                        } else if ("configuration".equals(reader.getLocalName())) {
                            keys.put("configuration", null);
                        }
                        break;
                    }
                }
            } catch (XMLStreamException ignored) {
                // 不完整的 XML 会在下次编辑时重新索引，不获取外部 DTD。
            } finally {
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (XMLStreamException ignored) {
                    }
                }
            }
            return keys;
        };
    }
}

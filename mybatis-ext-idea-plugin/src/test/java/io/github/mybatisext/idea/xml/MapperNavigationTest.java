package io.github.mybatisext.idea.xml;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.intellij.codeInsight.daemon.LineMarkerInfo;
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerInfo;
import com.intellij.psi.PsiClass;
import com.intellij.psi.xml.XmlFile;
import com.intellij.psi.xml.XmlTag;
import com.intellij.testFramework.LightProjectDescriptor;
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase;

public final class MapperNavigationTest extends LightJavaCodeInsightFixtureTestCase {

    @Override
    protected LightProjectDescriptor getProjectDescriptor() {
        return JAVA_17;
    }

    public void testStatementCandidatesShowDatabaseIdAndFile() {
        PsiClass mapper = myFixture.addClass("package sample; public interface UserMapper { String load(); }");
        myFixture.addFileToProject("UserMapper.xml", """
                <mapper namespace="sample.UserMapper">
                    <select id="load">select 1</select>
                    <select id="load" databaseId="mysql">select 2</select>
                </mapper>
                """);
        myFixture.addFileToProject("UserMapper-oracle.xml", """
                <mapper namespace="sample.UserMapper">
                    <select id="load" databaseId="oracle">select 3 from dual</select>
                </mapper>
                """);
        List<LineMarkerInfo<?>> markers = new ArrayList<>();
        new MapperNavigation().collectSlowLineMarkers(List.of(mapper.getMethods()[0].getNameIdentifier()), markers);
        assertEquals(1, markers.size());
        RelatedItemLineMarkerInfo<?> marker = assertInstanceOf(markers.get(0), RelatedItemLineMarkerInfo.class);
        var items = marker.createGotoRelatedItems();
        assertEquals(3, items.size());

        MapperNavigation.XmlTargetRenderer renderer = new MapperNavigation.XmlTargetRenderer();
        Set<String> labels = new HashSet<>();
        Set<String> files = new HashSet<>();
        for (var item : items) {
            var target = item.getElement();
            var presentation = renderer.getPresentation(target);
            labels.add(presentation.getPresentableText());
            files.add(presentation.getLocationText());
            assertNull(presentation.getContainerText());
            assertNotNull(presentation.getIcon());
        }
        assertEquals(Set.of("load", "load [mysql]", "load [oracle]"), labels);
        assertEquals(Set.of("UserMapper.xml", "UserMapper-oracle.xml"), files);

        XmlFile file = (XmlFile) myFixture.configureByText("database-id.xml", """
                <mapper namespace="sample.UserMapper">
                    <select id="empty" databaseId=""/>
                    <select id="blank" databaseId=" "/>
                    <select id="explicit" databaseId="default"/>
                </mapper>
                """);
        XmlTag[] tags = file.getRootTag().getSubTags();
        assertEquals("empty", renderer.getElementText(tags[0]));
        assertEquals("blank", renderer.getElementText(tags[1]));
        assertEquals("explicit [default]", renderer.getElementText(tags[2]));
        assertNotNull(renderer.getElementText(file.getRootTag()));
    }

    public void testStatementCandidatesPreserveDeclarationOrder() {
        PsiClass mapper = myFixture.addClass("package sample; public interface UserMapper { String load(); }");
        myFixture.addFileToProject("UserMapper.xml", """
                <mapper namespace="sample.UserMapper">
                    <select id="load" databaseId="oracle">select 1 from dual</select>
                    <select id="load">select 2</select>
                    <select id="load" databaseId="postgresql">select 3</select>
                    <select id="load" databaseId="mysql">select 4</select>
                </mapper>
                """);
        List<LineMarkerInfo<?>> markers = new ArrayList<>();
        new MapperNavigation().collectSlowLineMarkers(List.of(mapper.getMethods()[0].getNameIdentifier()), markers);
        assertEquals(1, markers.size());
        RelatedItemLineMarkerInfo<?> marker = assertInstanceOf(markers.get(0), RelatedItemLineMarkerInfo.class);
        MapperNavigation.XmlTargetRenderer renderer = new MapperNavigation.XmlTargetRenderer();
        List<String> labels = new ArrayList<>();
        for (var item : marker.createGotoRelatedItems()) {
            labels.add(renderer.getPresentation(item.getElement()).getPresentableText());
        }
        assertEquals(List.of("load [oracle]", "load", "load [postgresql]", "load [mysql]"), labels);
    }
}

package io.github.mybatisext.test.spring;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class OrgRelationCameraTest {

    @Autowired
    private OrgMapper orgMapper;

    @Test
    public void test() {
        List<Org> orgs = orgMapper.selectOrg();
        List<Org> orgs2 = orgMapper.selectOrgLazy();
        List<Org> orgs3 = orgMapper.selectOrgJoin();
        System.out.println(orgs.size());
        System.out.println(orgs3.size());
        System.out.println(orgs2.size());
    }
}

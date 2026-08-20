package io.github.mybatisext.test.jpa;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

import javax.persistence.EntityManager;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.data.mapping.PropertyReferenceException;
import org.springframework.data.repository.Repository;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactory;
import org.springframework.data.repository.query.QueryCreationException;

@SpringBootTest
public class SpringDataJpaCompatibilityIntegrationTest {

    @Autowired
    private SysUserRepository sysUserRepository;
    @Autowired
    private EntityManager entityManager;

    @Test
    public void executesSpringDataDerivedCountQuery() {
        assertEquals(0L, sysUserRepository.countByDeptId(103L));
    }

    @Test
    public void rejectsCountSysUserAsDerivedQuery() {
        JpaRepositoryFactory repositoryFactory = new JpaRepositoryFactory(entityManager);

        QueryCreationException exception = assertThrows(QueryCreationException.class, () -> repositoryFactory.getRepository(InvalidSysUserRepository.class));
        Throwable rootCause = NestedExceptionUtils.getMostSpecificCause(exception);
        PropertyReferenceException propertyReferenceException = assertInstanceOf(PropertyReferenceException.class, rootCause);
        assertEquals("countSysUser", propertyReferenceException.getPropertyName());
    }

    private interface InvalidSysUserRepository extends Repository<SysUser, Long> {
        long countSysUser();
    }
}

package io.github.mybatisext.test.spring;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class CameraRelationOrgTest {

    @Autowired
    private CameraMapper cameraMapper;

    @Test
    public void test() {
        List<Camera> cameras = cameraMapper.selectCamera();
        List<Camera> cameras2 = cameraMapper.selectCameraLazy();
        List<Camera> cameras3 = cameraMapper.selectCameraJoin();
        System.out.println(cameras.size());
        System.out.println(cameras2.size());
        System.out.println(cameras3.size());
    }
}

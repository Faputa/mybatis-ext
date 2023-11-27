package com.mybatisext.test.spring;

import java.util.List;

import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;

public class AssociationTest {
    
    public static void main(String[] args) {
		ApplicationContext context = SpringApplication.run(MybatisSpringApplication.class, args);
		CameraMapper cameraMapper = context.getBean(CameraMapper.class);
        List<Camera> cameras = cameraMapper.selectCamera();
        List<Camera> cameras2 = cameraMapper.selectCameraLazy();
        List<Camera> cameras3 = cameraMapper.selectCameraJoin();
        System.out.println(cameras.size());
        System.out.println(cameras2.size());
        System.out.println(cameras3.size());
    }
}

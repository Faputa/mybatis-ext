package com.example;

import javax.annotation.Resource;

import org.apache.ibatis.mapping.Environment;

public class MjSpringConfiguration extends MjConfiguration {

    private Resource[] mapperLocations;

    public Resource[] getMapperLocations() {
        return mapperLocations;
    }

    public void setMapperLocations(Resource[] mapperLocations) {
        this.mapperLocations = mapperLocations;
    }

    public MjSpringConfiguration(Environment environment) {
        super(environment);
    }

    @Override
    protected boolean hasXmlDefinedStatement(String methodName, Class<?> mapperInterface) {
        // TODO
        return false;
    }

}

package com.example.creditapplicationservice.mapper;

import com.example.creditapplicationservice.dto.ApplicationResponse;
import com.example.creditapplicationservice.entity.Application;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ApplicationMapper {
    @Mapping(target = "status", expression = "java(application.getStatus().name())")
    ApplicationResponse toResponse(Application application);
}

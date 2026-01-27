package com.lovespace.dto;

import com.lovespace.entity.Space;
import com.lovespace.entity.User;
import lombok.Data;

import java.util.List;

@Data
public class SpaceDetail {
    private Space space;
    private List<User> members;
}


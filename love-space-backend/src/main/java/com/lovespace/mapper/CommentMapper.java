package com.lovespace.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lovespace.entity.Comment;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CommentMapper extends BaseMapper<Comment> {
}

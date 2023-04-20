package com.yupi.springbootinit.model.dto.picture;

import com.yupi.springbootinit.common.PageRequest;
import lombok.Data;

import java.util.List;
@Data
public class PictureQueryRequest extends PageRequest {


    /**
     * 搜索词
     */
    private String searchText;


}
